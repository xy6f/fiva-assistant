package com.xy6f.fiva_assistant.controller;

import com.xy6f.fiva_assistant.service.LedgerService;
import com.xy6f.fiva_assistant.util.ExcelExportUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api/ledger")
@RequiredArgsConstructor
public class LedgerController {

    private final LedgerService ledgerService;

    /**
     * 明细账
     */
    @GetMapping("/detail")
    public ResponseEntity<?> detail(
            @RequestParam Long accountId,
            @RequestParam(required = false, defaultValue = "") String period) {
        try {
            return ResponseEntity.ok(Map.of("success", true, "data", ledgerService.getDetailLedger(accountId, period)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 总账
     */
    @GetMapping("/general")
    public ResponseEntity<?> general(
            @RequestParam(required = false, defaultValue = "") String period) {
        try {
            return ResponseEntity.ok(Map.of("success", true, "data", ledgerService.getGeneralLedger(period)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 科目余额表
     */
    @GetMapping("/trial-balance")
    public ResponseEntity<?> trialBalance(
            @RequestParam(required = false, defaultValue = "") String period) {
        try {
            return ResponseEntity.ok(Map.of("success", true, "data", ledgerService.getTrialBalance(period)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== 导出 ====================

    @GetMapping("/trial-balance/export/excel")
    public ResponseEntity<byte[]> exportTrialBalanceExcel(
            @RequestParam(required = false, defaultValue = "") String period) {
        try {
            Map<String, Object> data = ledgerService.getTrialBalance(period);
            List<Map<String, Object>> items = (List<Map<String, Object>>) data.get("items");

            List<Map<String, Object>> rows = new ArrayList<>();
            for (Map<String, Object> item : items) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("科目编码", item.get("accountCode"));
                row.put("科目名称", item.get("accountName"));
                row.put("期初借方", item.get("openingDebit"));
                row.put("期初贷方", item.get("openingCredit"));
                row.put("本期借方", item.get("periodDebit"));
                row.put("本期贷方", item.get("periodCredit"));
                row.put("期末借方", item.get("closingDebit"));
                row.put("期末贷方", item.get("closingCredit"));
                rows.add(row);
            }

            byte[] excelData = ExcelExportUtil.exportTransactions(rows,
                    new String[]{"科目编码", "科目名称", "期初借方", "期初贷方", "本期借方", "本期贷方", "期末借方", "期末贷方"},
                    "科目余额表");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "科目余额表_" + (period.isEmpty() ? "全部" : period) + ".xlsx");
            return ResponseEntity.ok().headers(headers).body(excelData);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/detail/export/excel")
    public ResponseEntity<byte[]> exportDetailLedgerExcel(
            @RequestParam Long accountId,
            @RequestParam(required = false, defaultValue = "") String period) {
        try {
            Map<String, Object> data = ledgerService.getDetailLedger(accountId, period);
            List<Map<String, Object>> items = (List<Map<String, Object>>) data.get("items");
            Map<String, Object> account = (Map<String, Object>) data.get("account");

            List<Map<String, Object>> rows = new ArrayList<>();
            for (Map<String, Object> item : items) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("日期", item.get("date"));
                row.put("凭证号", item.get("voucherNo"));
                row.put("摘要", item.get("summary"));
                row.put("借方", item.get("debitAmount"));
                row.put("贷方", item.get("creditAmount"));
                row.put("余额", item.get("balance"));
                rows.add(row);
            }

            String accountName = account != null ? account.get("code") + " " + account.get("name") : "科目";
            byte[] excelData = ExcelExportUtil.exportTransactions(rows,
                    new String[]{"日期", "凭证号", "摘要", "借方", "贷方", "余额"},
                    "明细账");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "明细账_" + accountName + "_" + (period.isEmpty() ? "全部" : period) + ".xlsx");
            return ResponseEntity.ok().headers(headers).body(excelData);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/general/export/excel")
    public ResponseEntity<byte[]> exportGeneralLedgerExcel(
            @RequestParam(required = false, defaultValue = "") String period) {
        try {
            Map<String, Object> data = ledgerService.getGeneralLedger(period);
            List<Map<String, Object>> items = (List<Map<String, Object>>) data.get("items");

            List<Map<String, Object>> rows = new ArrayList<>();
            for (Map<String, Object> item : items) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("科目编码", item.get("accountCode"));
                row.put("科目名称", item.get("accountName"));
                row.put("方向", "DEBIT".equals(item.get("balanceDirection")) ? "借" : "贷");
                row.put("期初余额", item.get("openingBalance"));
                row.put("本期借方", item.get("periodDebit"));
                row.put("本期贷方", item.get("periodCredit"));
                row.put("期末余额", item.get("closingBalance"));
                rows.add(row);
            }

            byte[] excelData = ExcelExportUtil.exportTransactions(rows,
                    new String[]{"科目编码", "科目名称", "方向", "期初余额", "本期借方", "本期贷方", "期末余额"},
                    "总账");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "总账_" + (period.isEmpty() ? "全部" : period) + ".xlsx");
            return ResponseEntity.ok().headers(headers).body(excelData);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
