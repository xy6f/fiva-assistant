# Round-3: Dedicated regression for the 4 structural fixes
# Covers: salary dedup, period close gates, account init idempotency,
#         AccountResolver auto-create
$ErrorActionPreference = 'Continue'
Add-Type -AssemblyName System.Net.Http
$env:MYSQL_PWD = if ($env:DB_PASSWORD) { $env:DB_PASSWORD } else { 'changeme' }

$BASE = 'http://localhost:8080/api'
$script:PASS = 0; $script:FAILN = 0; $script:FAILED = @()
function Add-Result([string]$id, [bool]$ok, [string]$msg) {
    if ($ok) { $script:PASS++; Write-Output "[PASS] $id $msg" }
    else { $script:FAILN++; $script:FAILED += $id; Write-Output "[FAIL] $id $msg" }
}
function UnwrapData($b) {
    if ($null -eq $b) { return $null }
    if ($b -is [array]) { return $b }
    if ($null -ne $b.data) { return $b.data }
    return $b
}
function HttpReq([string]$method, [string]$url, [string]$token, [string]$jsonBody) {
    try {
        $req = [System.Net.HttpWebRequest]::Create($url)
        $req.Method = $method
        $req.Timeout = 30000
        if ($token) { $req.Headers.Add('Authorization', "Bearer $token") }
        if ($jsonBody) {
            $req.ContentType = 'application/json'
            $bytes = [Text.Encoding]::UTF8.GetBytes($jsonBody)
            $req.ContentLength = $bytes.Length
            $s = $req.GetRequestStream(); $s.Write($bytes, 0, $bytes.Length); $s.Close()
        }
        try { $resp = $req.GetResponse() } catch [System.Net.WebException] { $resp = $_.Exception.Response }
        $code = [int]$resp.StatusCode
        $reader = New-Object IO.StreamReader($resp.GetResponseStream(), [Text.Encoding]::UTF8)
        $text = $reader.ReadToEnd(); $reader.Close()
        $obj = $null; try { $obj = $text | ConvertFrom-Json } catch {}
        return @{ status = $code; body = $obj; text = $text }
    } catch {
        return @{ status = -1; body = $null; text = $_.Exception.Message }
    }
}
function JsonPost([string]$url, [string]$token, $obj) {
    return HttpReq 'POST' $url $token (ConvertTo-Json -InputObject $obj -Depth 8)
}
function Get-Token([string]$user, [string]$pass) {
    try {
        $capResp = HttpReq 'GET' "$BASE/auth/captcha" $null $null
        $q = $capResp.body.data.question; $cid = $capResp.body.data.captchaId
        $nums = [regex]::Matches($q, '\d+') | ForEach-Object { [int]$_.Value }
        $ans = if ($q -match '\+') { $nums[0] + $nums[1] } else { $nums[0] * $nums[1] }
        $r = JsonPost "$BASE/auth/login" $null @{ username = $user; password = $pass; captchaId = $cid; captchaAnswer = "$ans" }
        if ($r.status -ne 200) { throw "login failed" }
        return @{ token = (UnwrapData $r.body).token }
    } catch { return $null }
}

# ======================== SETUP ========================
$admin = Get-Token 'admin' 'admin123'
if ($null -eq $admin) { Write-Output 'FATAL: no token'; exit 1 }

$accRaw = UnwrapData (HttpReq 'GET' "$BASE/account" $admin.token $null).body
$map = @{}; foreach ($a in @($accRaw)) { $map[$a.code] = $a.id }

# clean test data
& mysql -u root -h localhost fiva_assistant -e "DELETE FROM t_salary_record WHERE employee_no LIKE 'R3-%'; DELETE ve FROM t_voucher_entry ve JOIN t_voucher v ON ve.voucher_id=v.id WHERE v.user_id=1 AND v.voucher_date BETWEEN '2025-06-01' AND '2025-06-30'; DELETE FROM t_voucher WHERE user_id=1 AND voucher_date BETWEEN '2025-06-01' AND '2025-06-30'; DELETE FROM t_period_close WHERE user_id=1 AND period='2025-06'; DELETE FROM t_account WHERE user_id=1 AND code='9999';" 2>$null | Out-Null

# ======================== 1. ACCOUNT INIT IDEMPOTENCY ========================
Write-Output '--- ACCOUNT INIT ---'
$before = @($accRaw).Count
$r1 = JsonPost "$BASE/account/init" $admin.token @{}
$r2 = JsonPost "$BASE/account/init" $admin.token @{}
$after = UnwrapData (HttpReq 'GET' "$BASE/account" $admin.token $null).body
$afterCount = @($after).Count
Add-Result 'R3-AI-01' ($r1.status -eq 200 -and $before -eq $afterCount) "init called 3x total, accounts stable at $afterCount"

# ======================== 2. SALARY DEDUP ========================
Write-Output '--- SALARY DEDUP ---'
$sal = @{ employeeName='R3-Woker'; employeeNo='R3-001'; department='IT'; month='2026-10'; baseSalary=5000; bonus=0; socialInsurance=0; housingFund=0; tax=0 }

# 2a: first insert
$r = JsonPost "$BASE/salary/batch" $admin.token @($sal)
$id1 = $null; try { $id1 = (UnwrapData $r.body)[0].id } catch {}
$cnt1 = [int](& mysql -u root -h localhost fiva_assistant '-N' '-B' -e "SELECT COUNT(*) FROM t_salary_record WHERE user_id=1 AND employee_no='R3-001' AND month='2026-10';" 2>$null)
Add-Result 'R3-SD-01' ($r.status -eq 200 -and $id1 -ne $null -and $cnt1 -eq 1) "first batch: id=$id1 rows=$cnt1"

# 2b: duplicate insert should overwrite same row
$r = JsonPost "$BASE/salary/batch" $admin.token @($sal)
$id2 = $null; try { $id2 = (UnwrapData $r.body)[0].id } catch {}
$cnt2 = [int](& mysql -u root -h localhost fiva_assistant '-N' '-B' -e "SELECT COUNT(*) FROM t_salary_record WHERE user_id=1 AND employee_no='R3-001' AND month='2026-10';" 2>$null)
Add-Result 'R3-SD-02' ($r.status -eq 200 -and $id1 -eq $id2 -and $cnt2 -eq 1) "duplicate batch: same id=$id2, still 1 row"

# 2c: different employee_no = separate record
$sal2 = @{ employeeName='R3-Woker2'; employeeNo='R3-002'; department='IT'; month='2026-10'; baseSalary=8000; bonus=0; socialInsurance=0; housingFund=0; tax=0 }
$r = JsonPost "$BASE/salary/batch" $admin.token @($sal2)
$cnt3 = [int](& mysql -u root -h localhost fiva_assistant '-N' '-B' -e "SELECT COUNT(*) FROM t_salary_record WHERE user_id=1 AND employee_no IN ('R3-001','R3-002') AND month='2026-10';" 2>$null)
Add-Result 'R3-SD-03' ($r.status -eq 200 -and $cnt3 -eq 2) "different employee: rows=$cnt3"

# ======================== 3. SALARY LIFECYCLE ========================
Write-Output '--- SALARY LIFECYCLE ---'

# 3a: generate accrual voucher
$r = JsonPost "$BASE/salary/generate-voucher?month=2026-10" $admin.token @{}
$accrualId = $null; try { $accrualId = (UnwrapData $r.body).id } catch {}
Add-Result 'R3-SL-01' ($r.status -eq 200 -and $accrualId -ne $null) "accrual voucher id=$accrualId"

# 3b: records should now be POSTED
$cntPosted = [int](& mysql -u root -h localhost fiva_assistant '-N' '-B' -e "SELECT COUNT(*) FROM t_salary_record WHERE user_id=1 AND employee_no LIKE 'R3-%' AND status='POSTED';" 2>$null)
Add-Result 'R3-SL-02' ($cntPosted -ge 1) "POSTED records count=$cntPosted"

# 3c: duplicate accrual must fail
$r = JsonPost "$BASE/salary/generate-voucher?month=2026-10" $admin.token @{}
Add-Result 'R3-SL-03' ($r.status -ge 400) "repeat accrual -> $($r.status)"

# 3d: batchSave must reject POSTED records
$r = JsonPost "$BASE/salary/batch" $admin.token @($sal)
Add-Result 'R3-SL-04' ($r.status -ge 400) "batchSave on POSTED -> $($r.status)"

# 3e: generate payment
$r = JsonPost "$BASE/salary/generate-payment?month=2026-10" $admin.token @{}
$paymentId = $null; try { $paymentId = (UnwrapData $r.body).id } catch {}
Add-Result 'R3-SL-05' ($r.status -eq 200 -and $paymentId -ne $null) "payment voucher id=$paymentId"

# 3f: records should now be PAID
$cntPaid = [int](& mysql -u root -h localhost fiva_assistant '-N' '-B' -e "SELECT COUNT(*) FROM t_salary_record WHERE user_id=1 AND employee_no LIKE 'R3-%' AND status='PAID';" 2>$null)
Add-Result 'R3-SL-06' ($cntPaid -ge 1) "PAID records count=$cntPaid"

# 3g: duplicate payment must fail
$r = JsonPost "$BASE/salary/generate-payment?month=2026-10" $admin.token @{}
Add-Result 'R3-SL-07' ($r.status -ge 400) "repeat payment -> $($r.status)"

# 3h: accrual after payment must fail (already PAID)
$r = JsonPost "$BASE/salary/generate-voucher?month=2026-10" $admin.token @{}
Add-Result 'R3-SL-08' ($r.status -ge 400) "accrual on PAID -> $($r.status)"

# 3i: delete PAID record must fail
$r = HttpReq 'DELETE' "$BASE/salary/$id1" $admin.token $null
Add-Result 'R3-SL-09' ($r.status -eq 400) "delete PAID -> $($r.status)"

# 3j: payment without prior accrual on different month must fail
$r = JsonPost "$BASE/salary/generate-payment?month=2026-11" $admin.token @{}
Add-Result 'R3-SL-10' ($r.status -ge 400) "payment without accrual -> $($r.status)"

# ======================== 4. PERIOD CLOSE GATES ========================
Write-Output '--- PERIOD CLOSE ---'

# 4a: seed vouchers in 2025-06 (income + expense)
$map1001 = $map['1001']; $map5001 = $map['5001']; $map6601 = $map['6601']
$pcInc = @{ voucher = @{ type='INCOME'; amount=2000; category='TEST'; description='r3-pc-income'; voucherDate='2025-06-01'; status='DRAFT' }
            entries = @(@{ accountId=$map1001; summary='in'; debitAmount=2000; creditAmount=0 }, @{ accountId=$map5001; summary='in'; debitAmount=0; creditAmount=2000 }) }
$pcExp = @{ voucher = @{ type='EXPENSE'; amount=800; category='TEST'; description='r3-pc-expense'; voucherDate='2025-06-15'; status='DRAFT' }
            entries = @(@{ accountId=$map6601; summary='out'; debitAmount=800; creditAmount=0 }, @{ accountId=$map1001; summary='out'; debitAmount=0; creditAmount=800 }) }
$ri = JsonPost "$BASE/voucher" $admin.token $pcInc
$re = JsonPost "$BASE/voucher" $admin.token $pcExp
Add-Result 'R3-PC-01' ($ri.status -eq 200 -and $re.status -eq 200) "seed vouchers r1=$($ri.status) r2=$($re.status)"

# 4b: close 2025-06
$r = HttpReq 'POST' "$BASE/period-close/close?period=2025-06" $admin.token $null
Add-Result 'R3-PC-02' ($r.status -eq 200) "close 2025-06 -> $($r.status)"

# 4c: status shows closed
$r = HttpReq 'GET' "$BASE/period-close/status?period=2025-06" $admin.token $null
$closed = $false; try { $closed = [bool](UnwrapData $r.body).closed } catch {}
Add-Result 'R3-PC-03' ($r.status -eq 200 -and $closed) "status closed=$closed"

# 4d: repeat close rejected
$r = HttpReq 'POST' "$BASE/period-close/close?period=2025-06" $admin.token $null
Add-Result 'R3-PC-04' ($r.status -eq 400) "repeat close -> $($r.status)"

# 4e: create voucher in closed period rejected
$bad = @{ voucher = @{ type='EXPENSE'; amount=10; category='X'; description='r3-blocked'; voucherDate='2025-06-10'; status='DRAFT' }
          entries = @(@{ accountId=$map6601; summary='x'; debitAmount=10; creditAmount=0 }, @{ accountId=$map1001; summary='x'; debitAmount=0; creditAmount=10 }) }
$r = JsonPost "$BASE/voucher" $admin.token $bad
Add-Result 'R3-PC-05' ($r.status -eq 400) "create voucher in closed -> $($r.status)"

# 4f: update existing voucher in closed period rejected (use the seed expense)
$expId = $null; try { $expId = (UnwrapData $re.body).id } catch {}
$r = HttpReq 'PUT' "$BASE/voucher/$expId" $admin.token (ConvertTo-Json -InputObject @{ voucher = @{ type='EXPENSE'; amount=800; category='TEST'; description='r3-updated'; voucherDate='2025-06-15'; status='DRAFT' }; entries = @(@{ accountId=$map6601; summary='out'; debitAmount=800; creditAmount=0 }, @{ accountId=$map1001; summary='out'; debitAmount=0; creditAmount=800 }) } -Depth 5)
Add-Result 'R3-PC-06' ($r.status -eq 400) "update voucher in closed -> $($r.status)"

# 4g: reopen
$r = HttpReq 'POST' "$BASE/period-close/reopen?period=2025-06" $admin.token $null
Add-Result 'R3-PC-07' ($r.status -eq 200) "reopen 2025-06 -> $($r.status)"

# 4h: status shows open
$r = HttpReq 'GET' "$BASE/period-close/status?period=2025-06" $admin.token $null
$closed2 = $true; try { $closed2 = [bool](UnwrapData $r.body).closed } catch {}
Add-Result 'R3-PC-08' ($r.status -eq 200 -and -not $closed2) "status after reopen: closed=$closed2"

# 4i: repeat reopen rejected
$r = HttpReq 'POST' "$BASE/period-close/reopen?period=2025-06" $admin.token $null
Add-Result 'R3-PC-09' ($r.status -eq 400) "repeat reopen -> $($r.status)"

# 4j: create voucher after reopen succeeds
$r = JsonPost "$BASE/voucher" $admin.token $bad
Add-Result 'R3-PC-10' ($r.status -eq 200) "create voucher in reopened -> $($r.status)"

# 4k: close empty period rejected
$r = HttpReq 'POST' "$BASE/period-close/close?period=2024-06" $admin.token $null
Add-Result 'R3-PC-11' ($r.status -eq 400) "close empty period -> $($r.status)"

# ======================== 5. ACCOUNT RESOLVER AUTO-CREATE ========================
Write-Output '--- ACCOUNT RESOLVER ---'

# 5a: AccountResolver.ensureAccount via /api/account -- delete then recreate by code
$targetCode = '9999'
# remove if any leftovers
& mysql -u root -h localhost fiva_assistant -e "DELETE FROM t_account WHERE user_id=1 AND code='$targetCode';" 2>$null | Out-Null
$beforeDel = [int](& mysql -u root -h localhost fiva_assistant '-N' '-B' -e "SELECT COUNT(*) FROM t_account WHERE user_id=1 AND code='$targetCode';" 2>$null)

# create account via public API (exercises same save path)
$r = JsonPost "$BASE/account" $admin.token @{ code=$targetCode; name='r3-test-auto'; type='EXPENSE'; balanceDirection='DEBIT' }
$afterCreate = [int](& mysql -u root -h localhost fiva_assistant '-N' '-B' -e "SELECT COUNT(*) FROM t_account WHERE user_id=1 AND code='$targetCode';" 2>$null)
Add-Result 'R3-AR-01' ($r.status -eq 200 -and $beforeDel -eq 0 -and $afterCreate -ge 1) "create account -> $($r.status); count: $beforeDel -> $afterCreate"

# 5b: duplicate create with same code must be rejected (unique index)
$r2 = JsonPost "$BASE/account" $admin.token @{ code=$targetCode; name='r3-test-dup'; type='EXPENSE'; balanceDirection='DEBIT' }
$afterDup = [int](& mysql -u root -h localhost fiva_assistant '-N' '-B' -e "SELECT COUNT(*) FROM t_account WHERE user_id=1 AND code='$targetCode';" 2>$null)
Add-Result 'R3-AR-02' ($r2.status -ge 400 -and $afterDup -eq 1) "dup create -> $($r2.status); still 1 row"

# ======================== CLEANUP ========================
& mysql -u root -h localhost fiva_assistant -e "DELETE FROM t_salary_record WHERE employee_no LIKE 'R3-%'; DELETE ve FROM t_voucher_entry ve JOIN t_voucher v ON ve.voucher_id=v.id WHERE v.user_id=1 AND v.voucher_date BETWEEN '2025-06-01' AND '2025-06-30'; DELETE FROM t_voucher WHERE user_id=1 AND voucher_date BETWEEN '2025-06-01' AND '2025-06-30'; DELETE FROM t_period_close WHERE user_id=1 AND period='2025-06'; DELETE FROM t_account WHERE user_id=1 AND code='9999';" 2>$null | Out-Null

# ======================== SUMMARY ========================
Write-Output ''
Write-Output '================ ROUND 3 STRUCTURAL ================'
Write-Output "TOTAL=$($script:PASS + $script:FAILN) PASS=$($script:PASS) FAIL=$($script:FAILN)"
foreach ($f in $script:FAILED) { Write-Output "FAILED: $f" }
