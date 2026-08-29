# FIVA 智能财税系统 — 全面测试报告

**测试日期：** 2026-08-29  
**测试角色：** 会计专业 + 开发专业 双视角  
**分支：** master (e34b0f1)  
**测试账号：** admin/admin123 (ADMIN), accountant/admin123 (ACCOUNTANT)  
**e2e 结构回归：** 27/27 PASS (e2e_test3.ps1)

---

## 一、P0 — 必须立即修复（数据错误 / 安全漏洞 / 会计准则违规）

### P0-1: 科目余额表格式错误 — 期末余额方向反了
**模块：** TrialBalance.vue  
**行号：** 37-46  
**现象：** 审计线索显示"贷方: 168800"，但资产类科目（管理费用 6601、银行存款 1002）的期末余额应该在借方。利润表显示"净利润: -84400"与 VoucherList.vue 的"净利润: 84400"完全相反。  
**根因：** TrialBalance.vue 在计算 `endingBalance` 时，DEBIT 账户计算为 `credit - debit`，CREDIT 账户计算为 `debit - credit`，方向完全颠倒。  
**修复建议：**
```javascript
// 错误 (TrialBalance.vue:40)
const balance = entry.debitSum - entry.creditSum  // 应为 debit - credit
const balance = entry.creditSum - entry.debitSum   // 应为 credit - debit
// 正确
if (entry.balanceDirection === 'DEBIT') {
  endingBalance.value = entry.debitSum - entry.creditSum + openingBalance.value
} else {
  endingBalance.value = entry.creditSum - entry.debitSum + openingBalance.value
}
```
**影响范围：** 所有查看科目余额表的用户看到的余额方向和金额都是错的。

### P0-2: 小规模纳税人增值税免征额逻辑反转
**模块：** TaxService.java  
**行号：** 95-108  
**现象：** 系统设定 `threshold=300000, rate=0.03`。当季度销售额 < 30 万时，输出"增值税: ¥0.00"（免税），但会计应为：**< 30 万免征，> 30 万全额征税（不是仅对超出部分征税）**。  
**根因：** 当前实现使用累进逻辑（超过阈值的部分征税），而小规模纳税人增值税政策是**全额征免**。  
**修复建议：**
```java
// 错误 (TaxService.java:101)
amount = amount.subtract(threshold); // 只对超出部分征税
// 正确
if (amount.compareTo(threshold) > 0) {
    tax = amount.multiply(rate); // 全额征税
} else {
    tax = BigDecimal.ZERO; // 全额免税
}
```
**影响范围：** 2026 年所有季度税务测算结果都是错的。

### P0-3: 企业所得税税率过时 — 应为 25% 不是 30%
**模块：** TaxService.java  
**行号：** 114-143  
**现象：** 代码写死 `rate = new BigDecimal("0.30")`，但中国《企业所得税法》第四条规定税率为 **25%**。  
**修复建议：** `rate = new BigDecimal("0.25")`  
**影响范围：** 所有所得税测算结果偏高 5%。

### P0-4: 个人所得税免征额过时 — 应为 5000 不是 3500
**模块：** TaxService.java  
**行号：** 149-170  
**现象：** `exemption = new BigDecimal("3500")`，但 2018 年 10 月 1 日起免征额已调整为 **5000**。  
**修复建议：** `exemption = new BigDecimal("5000")`  
**影响范围：** 所有员工个税计算结果偏高，工资实发金额偏低。

### P0-5: VAT 进项税额计算错误 — 用成本×13% 而非实际发票
**模块：** TaxService.java  
**行号：** 75-79  
**现象：** 代码从成本费用汇总表取金额后乘 13% 作为进项税，但这些金额**已经包含增值税**。正确的进项税应从发票税额直接取。  
**根因：** 使用会计科目余额而非发票税额字段。  
**修复建议：** 改为从 `invoice` 表汇总 `tax_amount` 字段。  
**影响范围：** 增值税测算结果偏差很大。

### P0-6: 会计科目删除无关联凭证检查 — 可能导致数据孤立
**模块：** AccountService.java  
**行号：** 98-101  
**现象：** `delete()` 直接删除科目，不检查是否已有凭证引用。已入账的科目被删除后，历史凭证引用的科目 ID 变成孤儿，明细账/总账/科目余额表查询结果不一致。  
**修复建议：** 删除前检查 `voucherEntryMapper.existsByAccountId(id)`。  
**影响范围：** 数据完整性，历史凭证数据不可追溯。

### P0-7: 科目借方/贷方总额取数错误
**模块：** LedgerService.java  
**行号：** 92-109  
**现象：** `getLedgerByAccountCode` 用 `SUM(voucher.debit_amount)` 和 `SUM(voucher.credit_amount)` 查询，但 `voucher` 表只有 `total_amount`，没有 `debit_amount` 和 `credit_amount`。这两个字段在 `voucher_entry` 表。SQL 查不到数据或报错。  
**根因：** JOIN 了 `voucher` 表但查了不存在的列。  
**修复建议：** 应从 `voucher_entry` 表汇总借贷方金额。  
**影响范围：** 明细账借贷合计列全部为空。

### P0-8: Expenses 审批接口 — approver 由调用者传入而非取当前登录用户
**模块：** ExpenseService.java  
**行号：** 67  
**现象：** `approve()` 方法的 `approver` 参数由调用者传入，而非从 `UserContext.getUserId()` 获取。攻击者可以伪造其他用户名进行审批。  
**修复建议：** 从安全上下文获取当前用户。  
**影响范围：** 费用审批审计信息造假，合规性风险。

### P0-9: Expenses / Contacts — update/delete 无 userId 归属校验
**模块：** ExpenseService.java, ContactService.java  
**行号：** ExpenseService:48,77 / ContactService:54,70  
**现象：** `getById(id)` 不检查记录是否属于当前用户。用户 A 可以通过猜测 ID 来查看、修改、删除用户 B 的费用报销和联系人。  
**根因：** MyBatis Mapper 的 `findById()` 不带 `user_id` 条件。  
**修复建议：** `getById` 后校验 `record.getUserId().equals(UserContext.getUserId())`。  
**影响范围：** 多用户环境下的数据泄露和篡改。

---

## 二、P1 — 建议修复（逻辑风险 / 安全加固 / 会计处理不严谨）

### P1-1: 凭证状态 — 可以跳过 DRAFT 直接 POSTED
**模块：** VoucherService.java  
**行号：** 147-161  
**现象：** `update()` 中从 DRAFT 过渡到 POSTED 的分支（`oldStatus == DRAFT && newStatus == POSTED`）没有问题，但 `save()` 允许前端直接提交 `status=POSTED`。前端 AddVoucher.vue 提交 `status='DRAFT'`（行 224），但修改凭证界面允许选"已过账"状态直接提交。  
**修复建议：** save() 应始终插入为 DRAFT，过账只能通过 `updateStatus()` 单独操作。

### P1-2: 期末结转 — 损益结转凭证可被删除
**模块：** VoucherService.java  
**行号：** 47-50  
**现象：** `delete()` 只检查 `POSTED` 状态阻止删除，但 `PERIOD_CLOSING` 状态的结转凭证可以被直接删除。删除后损益数据不平，但系统无任何警告。  
**修复建议：** `PERIOD_CLOSING` 状态也应禁止删除，或至少要求先反结账。

### P1-3: 期末结转 — 可能 double-counting 收入费用
**模块：** PeriodCloseService.java + FinancialStatementService.java  
**现象：** `closePeriod()` 生成结转凭证（收入/费用清零，利润转入未分配利润），然后 `getIncomeStatement()` 再次从收入费用科目汇总。如果结转凭证的日期在报表期间内，**净利润会被计算两次**。  
**修复建议：** 报表查询时排除 `PERIOD_CLOSING` 类型凭证的分录，或使用 `voucher.type` 过滤。

### P1-4: 工资记账 — 所有部门都记入管理费用
**模块：** SalaryService.java  
**行号：** 112-113  
**现象：** `generateSalaryVoucher()` 借方固定使用 `6601 管理费用-工资`，但销售部门工资应记 `6602 销售费用`，生产部门记 `5001 生产成本`。  
**修复建议：** 根据员工部门自动选择费用科目。

### P1-5: 工资 batchSave — 并发竞态条件
**模块：** SalaryService.java  
**行号：** 40-53  
**现象：** 两个并发请求可以同时通过 `findByUserIdAndEmployeeNoAndMonth` 检查（都返回 null），然后都执行插入，产生重复记录。  
**修复建议：** 在 `salary_record` 表添加 `(user_id, employee_no, month)` 唯一约束。

### P1-6: 科目余额表/总账 — 期初余额取数不准
**模块：** LedgerService.java  
**行号：** 10-24  
**现象：** 期初余额使用 `getAccountOpeningBalance` 查询，SQL 中 `voucher_date < #{startDate}` 但 JOIN 条件 `ve.voucher_id = v.id` 只匹配已过账凭证。**已入账（POSTED）但尚未结账的凭证也应纳入期初计算**，当前 SQL 逻辑对这一点的处理不够清晰。  
**修复建议：** 确认期初计算逻辑是否包含所有 POSTED 凭证（不仅仅是当期之前）。

### P1-7: 预算 — 无月份范围校验，可创建 month=13
**模块：** BudgetService.java  
**行号：** 45-51  
**现象：** `create()` 不校验 `month` 是否在 1-12 范围，不检查是否已有同部门同月预算。  
**修复建议：** 添加 `month < 1 || month > 12` 校验 + 重复预算检查。

### P1-8: 预算告警 — 定时任务无并发保护
**模块：** AlertService.java  
**行号：** 86-98  
**现象：** `checkBudgetAlerts()` 遍历所有用户时无 `AtomicBoolean` 或分布式锁保护，多次触发可能产生重复告警。  
**修复建议：** 添加 `@SchedulerLock` 或简单的 `AtomicBoolean` 守卫。

### P1-9: 预算告警 — 加载全部用户到内存
**模块：** AlertService.java  
**行号：** 92  
**现象：** `userMapper.findAll()` 一次性加载所有用户，数据量大时 OOM。  
**修复建议：** 使用分页或流式查询。

### P1-10: 前端 XSS — 三处 v-html 未做 HTML 转义
**模块：** AiChat.vue:65, TaxAnalysis.vue:36, TaxCalc.vue:79  
**现象：** 三个文件使用正则将 AI 返回的 Markdown 转为 HTML 后直接 `v-html` 渲染，未转义 `&<>"` 等字符。如果 AI 返回含 `<script>` 的内容，会直接执行。  
**修复建议：** 使用 `marked` + `DOMPurify`，或至少在转换前 `.replace(/</g, '&lt;')`。

### P1-11: ExpenseList/BudgetList/BankTransaction/FixedAsset — VIEWER 可见编辑按钮
**模块：** 多个 Vue 文件  
**现象：** 这些页面没有导入 `canWrite()`，VIEWER 登录后能看到并点击"编辑""删除"按钮，虽然后端会 403，但用户体验差。  
**修复建议：** 统一在这些页面加 `canWrite()` 控制按钮可见性。

### P1-12: 现金流量表 — 分类逻辑不完整
**模块：** FinancialStatementService.java  
**行号：** 249-258  
**现象：** 
- 短期投资 (`1101`)、长期债券投资 (`1501`) 被归为经营活动，应为投资活动
- `1012 其他货币资金` 未纳入现金范围
- 编制方法用绝对金额比例分摊，不是标准做法
**修复建议：** 维护标准现金流量分类映射表。

---

## 三、P2 — 建议改进（用户体验 / 代码质量 / 性能）

| # | 模块 | 问题 | 影响 |
|---|------|------|------|
| P2-1 | FinancialStatementService:79 | 净利润计算从 2020-01-01 硬编码起始日期 | 公司成立日期不同会导致结果不准 |
| P2-2 | FinancialStatementService | 利润表未单独列示所得税费用（6801） | 不符合中国会计准则格式要求 |
| P2-3 | FinancialStatementService | 资产负债表累计折旧作为独立负数行显示 | 不符合传统报表列示格式 |
| P2-4 | SalaryService:216 | 社保和公积金合并记入 2241 其他应付款 | 应分设子科目 |
| P2-5 | BudgetService | `updateUsedAmount` 无访问控制，可被任意调用 | 数据篡改风险 |
| P2-6 | AccountService | `getById`/`update` 无 userId 归属检查 | 多用户数据泄露 |
| P2-7 | BudgetService | `getById` 无 userId 归属检查 | 多用户数据泄露 |
| P2-8 | AlertService | 告警去重仅检查 UNREAD 状态 | READ 状态后仍会重复创建 |
| P2-9 | SecurityConfig | CORS origins 硬编码 localhost | 生产部署必须修改 |
| P2-10 | SecurityConfig | JWT 无吊销/黑名单机制 | 被禁用用户的 token 仍有效直到过期 |
| P2-11 | router/index.js | 缺少 404 catch-all 路由 | 不存在的路径显示空白页 |
| P2-12 | App.vue | `/tax/analysis` 路由有但侧边栏无菜单入口 | 用户只能通过 URL 访问 |
| P2-13 | Register.vue | 无密码强度校验，允许任意特殊字符 | 财务系统安全标准偏低 |
| P2-14 | AddVoucher.vue | 单行分录可同时填借方和贷方 | 不符合复式记账规范 |
| P2-15 | api/index.js | 多个 401 并发请求触发多次弹窗 | 用户体验差 |
| P2-16 | AlertService:116-121 | 告警去重使用 N+1 查询 | 大数据量下性能差 |
| P2-17 | ExpenseService:20 | create 无输入校验 | 空记录可入库 |
| P2-18 | ExpenseService | 无多级审批支持 | 只能单级审批 |

---

## 四、P3 — 长期优化（架构 / 新功能）

| # | 模块 | 问题 | 建议 |
|---|------|------|------|
| P3-1 | TaxService | 缺少增值税进项税额抵扣链条 | 需关联采购发票 |
| P3-2 | TaxService | 缺少税务申报表生成 | 需对接电子税务局 |
| P3-3 | SalaryService | 无社保/公积金计算逻辑 | 需要按地区配置比例 |
| P3-4 | FinancialStatement | 现金流量表编制方法非标准 | 需要人工分类或映射表 |
| P3-5 | ExpenseService | 无多级审批工作流 | 需要审批链配置 |
| P3-6 | PeriodCloseService | 无跨年自动结转上年余额 | 需要年末特别处理 |
| P3-7 | BankTransaction | CSV导入无重复校验 | 可能产生重复流水 |

---

## 五、测试汇总

| 严重级别 | 数量 | 需立即修复 |
|----------|------|-----------|
| **P0 — 必须修复** | 9 个 | 是 |
| **P1 — 建议修复** | 12 个 | 尽快 |
| **P2 — 建议改进** | 18 个 | 按优先级 |
| **P3 — 长期优化** | 7 个 | 规划中 |
| **合计** | **46 个** | — |

### 会计视角核心问题

系统在**税务计算准确性**方面存在严重偏差（P0-2,3,4,5），如果用于实际报税会导致：
- 增值税免征额逻辑错误（应全额免征变成累进征税）
- 所得税税率偏高 5%（30% vs 25%）
- 个税免征额偏旧（3500 vs 5000）
- 进项税额取数错误（用成本估算而非实际发票）

科目余额表（P0-1）借贷方向完全颠倒，**报表不可信**。

### 开发视角核心问题

安全层面存在**多用户数据隔离缺失**（P0-6,8,9），生产环境多用户使用时存在数据泄露风险。前端 XSS（P1-10）需要在上线前修复。

---

**测试完成。** 以上 46 个问题覆盖凭证管理、账簿管理、期末处理、税务测算、财务报表、权限安全、数据完整性、前端交互、边界条件等全部测试维度。
