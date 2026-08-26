package com.xy6f.fiva_assistant.ai;

import com.xy6f.fiva_assistant.mapper.AccountMapper;
import com.xy6f.fiva_assistant.mapper.BudgetMapper;
import com.xy6f.fiva_assistant.mapper.InvoiceMapper;
import com.xy6f.fiva_assistant.mapper.VoucherEntryMapper;
import com.xy6f.fiva_assistant.mapper.VoucherMapper;
import com.xy6f.fiva_assistant.model.Account;
import com.xy6f.fiva_assistant.model.Budget;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * AI 查询用户真实账务数据的只读工具集。
 * 每次会话构造新实例，userId 在请求进入时捕获（SSE 异步线程中 ThreadLocal 不可用）。
 */
@Slf4j
public class FivaDataTools {

    private final Long userId;
    private final VoucherMapper voucherMapper;
    private final VoucherEntryMapper voucherEntryMapper;
    private final AccountMapper accountMapper;
    private final InvoiceMapper invoiceMapper;
    private final BudgetMapper budgetMapper;

    public FivaDataTools(Long userId,
                         VoucherMapper voucherMapper,
                         VoucherEntryMapper voucherEntryMapper,
                         AccountMapper accountMapper,
                         InvoiceMapper invoiceMapper,
                         BudgetMapper budgetMapper) {
        this.userId = userId;
        this.voucherMapper = voucherMapper;
        this.voucherEntryMapper = voucherEntryMapper;
        this.accountMapper = accountMapper;
        this.invoiceMapper = invoiceMapper;
        this.budgetMapper = budgetMapper;
    }

    @Tool("查询当前用户本月的收入合计、支出合计和净额（基于凭证数据）")
    public String monthlyIncomeExpense() {
        LocalDate now = LocalDate.now();
        LocalDate monthStart = now.withDayOfMonth(1);
        BigDecimal income = nvl(voucherMapper.sumByTypeBetween("INCOME", monthStart, now, userId));
        BigDecimal expense = nvl(voucherMapper.sumByTypeBetween("EXPENSE", monthStart, now, userId));
        return String.format("本月(%s)收入合计: %s 元, 支出合计: %s 元, 净额: %s 元",
                now.getYear() + "年" + now.getMonthValue() + "月", income, expense, income.subtract(expense));
    }

    @Tool("查询当前用户今年的收入合计、支出合计和净利润（基于凭证数据）")
    public String yearlyIncomeExpense() {
        LocalDate now = LocalDate.now();
        LocalDate yearStart = now.withDayOfYear(1);
        BigDecimal income = nvl(voucherMapper.sumByTypeSince("INCOME", yearStart, userId));
        BigDecimal expense = nvl(voucherMapper.sumByTypeSince("EXPENSE", yearStart, userId));
        return String.format("今年(%d年)累计收入: %s 元, 累计支出: %s 元, 净利润: %s 元",
                now.getYear(), income, expense, income.subtract(expense));
    }

    @Tool("查询指定日期区间的收支汇总，日期格式 yyyy-MM-dd")
    public String incomeExpenseBetween(@P("开始日期 yyyy-MM-dd") String start,
                                       @P("结束日期 yyyy-MM-dd") String end) {
        LocalDate s = LocalDate.parse(start);
        LocalDate e = LocalDate.parse(end);
        if (e.isBefore(s)) return "结束日期不能早于开始日期";
        BigDecimal income = nvl(voucherMapper.sumByTypeBetween("INCOME", s, e, userId));
        BigDecimal expense = nvl(voucherMapper.sumByTypeBetween("EXPENSE", s, e, userId));
        return String.format("%s 至 %s 收入合计: %s 元, 支出合计: %s 元, 净额: %s 元",
                start, end, income, expense, income.subtract(expense));
    }

    @Tool("查询当前用户各会计科目的期末余额（借方为正、贷方为负），用于分析资产负债结构")
    public String accountBalances() {
        List<Account> accounts = accountMapper.findByUserIdAndEnabledTrueOrderByCodeAsc(userId);
        Map<Long, Account> accountMap = new java.util.HashMap<>();
        accounts.forEach(a -> accountMap.put(a.getId(), a));

        List<Map<String, Object>> sums = voucherEntryMapper.sumGroupByAccount(userId, LocalDate.now());
        StringBuilder sb = new StringBuilder("科目余额(借-贷,单位元):\n");
        boolean any = false;
        for (Map<String, Object> row : sums) {
            Long accId = ((Number) row.get("accountId")).longValue();
            Account acc = accountMap.get(accId);
            if (acc == null) continue;
            BigDecimal debit = toBd(row.get("debit"));
            BigDecimal credit = toBd(row.get("credit"));
            BigDecimal balance = debit.subtract(credit);
            if (balance.compareTo(BigDecimal.ZERO) == 0) continue;
            sb.append(String.format("%s %s [%s]: %s%n", acc.getCode(), acc.getName(), acc.getType(), balance));
            any = true;
        }
        return any ? sb.toString().trim() : "暂无科目余额数据";
    }

    @Tool("查询当前用户最近的凭证列表（默认10条，最多50条）")
    public String recentVouchers(@P("返回条数") int limit) {
        int n = Math.min(Math.max(limit, 1), 50);
        List<com.xy6f.fiva_assistant.model.Voucher> vouchers =
                voucherMapper.findByUserIdOrderByVoucherDateDescCreatedAtDesc(userId);
        StringBuilder sb = new StringBuilder(String.format("最近凭证(共%d张,显示%d张):\n", vouchers.size(), Math.min(n, vouchers.size())));
        vouchers.stream().limit(n).forEach(v -> sb.append(String.format(
                "%s | %s | %s | 金额%s元 | %s | 状态:%s%n",
                v.getVoucherNo(), v.getVoucherDate(),
                "INCOME".equals(v.getType()) ? "收入" : "支出",
                v.getAmount(),
                v.getDescription() != null ? v.getDescription() : "",
                v.getStatus())));
        return sb.toString().trim();
    }

    @Tool("查询当前用户的发票统计：总数、各类别数量与金额、价税合计")
    public String invoiceStats() {
        Long count = invoiceMapper.countByUserId(userId);
        List<Map<String, Object>> cats = invoiceMapper.categoryStats(userId);
        StringBuilder sb = new StringBuilder(String.format("发票总数: %d 张%n", count != null ? count : 0));
        BigDecimal total = BigDecimal.ZERO;
        for (Map<String, Object> row : cats) {
            Object cat = row.get("category");
            BigDecimal amt = toBd(row.get("amt"));
            Number cnt = (Number) row.get("cnt");
            total = total.add(amt);
            sb.append(String.format("类别[%s]: %d 张, 价税合计 %s 元%n",
                    cat != null ? cat.toString() : "未分类", cnt.intValue(), amt));
        }
        sb.append(String.format("全部发票价税合计: %s 元", total));
        return sb.toString();
    }

    @Tool("查询当前用户今年各月份的收支趋势")
    public String monthlyTrend() {
        LocalDate now = LocalDate.now();
        List<Map<String, Object>> rows = voucherMapper.monthlyIncomeExpense(now.withDayOfYear(1), now, userId);
        if (rows.isEmpty()) return "今年暂无收支数据";
        StringBuilder sb = new StringBuilder("今年各月收支(元):\n");
        for (Map<String, Object> row : rows) {
            int m = ((Number) row.get("m")).intValue();
            BigDecimal income = toBd(row.get("income"));
            BigDecimal expense = toBd(row.get("expense"));
            sb.append(String.format("%d月: 收入%s, 支出%s, 净额%s%n", m, income, expense, income.subtract(expense)));
        }
        return sb.toString().trim();
    }

    @Tool("查询当前用户本年度预算执行情况")
    public String budgetUsage() {
        List<Budget> budgets = budgetMapper.findByUserIdAndYearOrderByCreatedAtDesc(userId, LocalDate.now().getYear());
        if (budgets.isEmpty()) return "今年暂无预算数据";
        StringBuilder sb = new StringBuilder("预算执行情况:\n");
        for (Budget b : budgets.stream().limit(30).toList()) {
            BigDecimal used = b.getUsedAmount() != null ? b.getUsedAmount() : BigDecimal.ZERO;
            BigDecimal budgetAmount = b.getBudgetAmount() != null ? b.getBudgetAmount() : BigDecimal.ZERO;
            String pct = budgetAmount.compareTo(BigDecimal.ZERO) > 0
                    ? used.multiply(new BigDecimal("100")).divide(budgetAmount, 1, java.math.RoundingMode.HALF_UP) + "%"
                    : "N/A";
            sb.append(String.format("%d年%d月 %s[%s]: 预算%s元, 已用%s元(%s)%n",
                    b.getYear(), b.getMonth(), b.getDepartment(),
                    b.getCategory() != null ? b.getCategory() : "", budgetAmount, used, pct));
        }
        return sb.toString().trim();
    }

    private BigDecimal nvl(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private BigDecimal toBd(Object o) {
        if (o == null) return BigDecimal.ZERO;
        if (o instanceof BigDecimal bd) return bd;
        return new BigDecimal(o.toString());
    }
}
