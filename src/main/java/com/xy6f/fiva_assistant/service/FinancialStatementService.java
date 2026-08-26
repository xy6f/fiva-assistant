package com.xy6f.fiva_assistant.service;

import com.xy6f.fiva_assistant.model.Account;
import com.xy6f.fiva_assistant.mapper.AccountMapper;
import com.xy6f.fiva_assistant.mapper.VoucherEntryMapper;
import com.xy6f.fiva_assistant.util.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FinancialStatementService {

    private final VoucherEntryMapper voucherEntryMapper;
    private final AccountMapper accountMapper;

    /**
     * 资产负债表。
     * 余额全部来自 SQL 聚合，不再把历史分录全量加载进内存。
     * 科目余额方向按类型处理：资产=借-贷；负债/权益=贷-借；
     * 累计折旧等资产类贷方余额科目自然以负数列示于资产方（抵减项）。
     */
    public Map<String, Object> getBalanceSheet(String period) {
        Long userId = UserContext.getUserId();
        LocalDate[] range = parsePeriod(period);
        LocalDate end = range[1];

        List<Account> accounts = accountMapper.findByUserIdAndEnabledTrueOrderByCodeAsc(userId);
        Map<Long, Account> accountById = accounts.stream().collect(Collectors.toMap(Account::getId, a -> a));

        // 截至期末各科目累计借/贷发生额（单条 SQL）
        Map<Long, BigDecimal[]> balanceMap = new HashMap<>();
        for (Map<String, Object> row : voucherEntryMapper.sumGroupByAccount(userId, end)) {
            Long accId = ((Number) row.get("accountId")).longValue();
            balanceMap.put(accId, new BigDecimal[]{toBd(row.get("debit")), toBd(row.get("credit"))});
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("period", period);

        // === 资产类 ===
        List<Map<String, Object>> assets = new ArrayList<>();
        BigDecimal totalAssets = BigDecimal.ZERO;
        for (Account acc : filterByType(accounts, "ASSET")) {
            BigDecimal[] amt = balanceMap.getOrDefault(acc.getId(), new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            BigDecimal balance = amt[0].subtract(amt[1]); // 借-贷
            if (balance.compareTo(BigDecimal.ZERO) != 0) {
                assets.add(item(acc.getCode(), acc.getName(), balance));
                totalAssets = totalAssets.add(balance);
            }
        }
        result.put("assets", assets);
        result.put("totalAssets", totalAssets);

        // === 负债类 ===
        BigDecimal totalLiabilities = collectSide(result, filterByType(accounts, "LIABILITY"), balanceMap, "liabilities", "totalLiabilities");

        // === 权益类 ===
        List<Map<String, Object>> equities = new ArrayList<>();
        BigDecimal totalEquity = BigDecimal.ZERO;
        for (Account acc : filterByType(accounts, "EQUITY")) {
            BigDecimal[] amt = balanceMap.getOrDefault(acc.getId(), new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            BigDecimal equityBalance = amt[1].subtract(amt[0]); // 贷-借
            if (equityBalance.compareTo(BigDecimal.ZERO) != 0) {
                equities.add(item(acc.getCode(), acc.getName(), equityBalance));
                totalEquity = totalEquity.add(equityBalance);
            }
        }

        // 累计未分配利润 = 累计收入(贷-借净额) - 累计费用(借-贷净额)，同样来自聚合查询
        LocalDate sinceStart = LocalDate.of(2020, 1, 1);
        Map<Long, BigDecimal[]> periodMap = sumBetween(userId, sinceStart, end);
        BigDecimal netProfit = calcNetProfitFromSums(accounts, periodMap);
        if (netProfit.compareTo(BigDecimal.ZERO) != 0) {
            equities.add(item("", "未分配利润(本期)", netProfit));
            totalEquity = totalEquity.add(netProfit);
        }
        result.put("equities", equities);
        result.put("totalEquity", totalEquity);

        // 负债+权益合计
        BigDecimal totalLiabilitiesAndEquity = totalLiabilities.add(totalEquity);
        result.put("totalLiabilitiesAndEquity", totalLiabilitiesAndEquity);

        // === 勾稽关系校验：资产 = 负债 + 所有者权益 ===
        BigDecimal diff = totalAssets.subtract(totalLiabilitiesAndEquity);
        Map<String, Object> balanceCheck = new LinkedHashMap<>();
        boolean passed = diff.abs().compareTo(new BigDecimal("0.01")) <= 0;
        balanceCheck.put("passed", passed);
        balanceCheck.put("diff", diff.setScale(2, java.math.RoundingMode.HALF_UP));
        if (!passed) {
            log.warn("资产负债表勾稽不平衡: 资产={}, 负债+权益={}, 差额={}", totalAssets, totalLiabilitiesAndEquity, diff);
        }
        result.put("balanceCheck", balanceCheck);

        return result;
    }

    /**
     * 利润表。收入与费用均按净额计算：
     * 收入 = 贷方发生 - 借方发生（红字冲销正确扣减）；
     * 费用 = 借方发生 - 贷方发生（费用冲销正确扣减）。
     */
    public Map<String, Object> getIncomeStatement(String period) {
        Long userId = UserContext.getUserId();
        LocalDate[] range = parsePeriod(period);
        LocalDate start = range[0];
        LocalDate end = range[1];

        List<Account> accounts = accountMapper.findByUserIdAndEnabledTrueOrderByCodeAsc(userId);
        Map<Long, BigDecimal[]> periodMap = sumBetween(userId, start, end);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("period", period);

        // 收入类：贷-借净额
        List<Map<String, Object>> revenues = new ArrayList<>();
        BigDecimal totalRevenue = BigDecimal.ZERO;
        for (Account acc : filterByType(accounts, "REVENUE")) {
            BigDecimal[] amt = periodMap.getOrDefault(acc.getId(), new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            BigDecimal revenue = amt[1].subtract(amt[0]);
            if (revenue.compareTo(BigDecimal.ZERO) != 0) {
                revenues.add(item(acc.getCode(), acc.getName(), revenue));
                totalRevenue = totalRevenue.add(revenue);
            }
        }
        result.put("revenues", revenues);
        result.put("totalRevenue", totalRevenue);

        // 费用类：借-贷净额
        List<Map<String, Object>> expenses = new ArrayList<>();
        BigDecimal totalExpenses = BigDecimal.ZERO;
        for (Account acc : filterByType(accounts, "EXPENSE")) {
            BigDecimal[] amt = periodMap.getOrDefault(acc.getId(), new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            BigDecimal expense = amt[0].subtract(amt[1]);
            if (expense.compareTo(BigDecimal.ZERO) != 0) {
                expenses.add(item(acc.getCode(), acc.getName(), expense));
                totalExpenses = totalExpenses.add(expense);
            }
        }
        result.put("expenses", expenses);
        result.put("totalExpenses", totalExpenses);

        // 净利润
        result.put("netProfit", totalRevenue.subtract(totalExpenses));

        return result;
    }

    /**
     * 现金流量表（简化版）。
     * 改进点：
     * 1. 按对方科目分录金额比例分摊现金净变动，而不是只看第一条分录；
     * 2. 活动分类更合理：固定资产(16xx)/无形资产(17xx)→投资活动，
     *    负债/权益→筹资活动，其余→经营活动。
     */
    public Map<String, Object> getCashFlowStatement(String period) {
        Long userId = UserContext.getUserId();
        LocalDate[] range = parsePeriod(period);
        LocalDate start = range[0];
        LocalDate end = range[1];

        List<Account> accounts = accountMapper.findByUserIdAndEnabledTrueOrderByCodeAsc(userId);
        Map<Long, Account> accountById = accounts.stream().collect(Collectors.toMap(Account::getId, a -> a));
        Set<Long> cashAccountIds = accounts.stream()
                .filter(a -> "1001".equals(a.getCode()) || "1002".equals(a.getCode()))
                .map(Account::getId).collect(Collectors.toSet());

        List<com.xy6f.fiva_assistant.model.VoucherEntry> periodEntries =
                voucherEntryMapper.findAllInPeriod(userId, start, end);
        Map<Long, List<com.xy6f.fiva_assistant.model.VoucherEntry>> voucherEntriesMap = periodEntries.stream()
                .collect(Collectors.groupingBy(com.xy6f.fiva_assistant.model.VoucherEntry::getVoucherId));

        BigDecimal operatingInflow = BigDecimal.ZERO, operatingOutflow = BigDecimal.ZERO;
        BigDecimal investingInflow = BigDecimal.ZERO, investingOutflow = BigDecimal.ZERO;
        BigDecimal financingInflow = BigDecimal.ZERO, financingOutflow = BigDecimal.ZERO;

        for (List<com.xy6f.fiva_assistant.model.VoucherEntry> entries : voucherEntriesMap.values()) {
            List<com.xy6f.fiva_assistant.model.VoucherEntry> cashEntries = new ArrayList<>();
            List<com.xy6f.fiva_assistant.model.VoucherEntry> otherEntries = new ArrayList<>();
            for (com.xy6f.fiva_assistant.model.VoucherEntry e : entries) {
                (cashAccountIds.contains(e.getAccountId()) ? cashEntries : otherEntries).add(e);
            }
            if (cashEntries.isEmpty() || otherEntries.isEmpty()) continue; // 纯转账或非现金业务

            // 该凭证的现金净变动（正=流入）
            BigDecimal cashDebit = cashEntries.stream().map(e -> nvl(e.getDebitAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal cashCredit = cashEntries.stream().map(e -> nvl(e.getCreditAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal netCash = cashDebit.subtract(cashCredit);

            // 对方科目绝对金额合计，用于比例分摊
            BigDecimal totalAbs = BigDecimal.ZERO;
            for (com.xy6f.fiva_assistant.model.VoucherEntry other : otherEntries) {
                totalAbs = totalAbs.add(nvl(other.getDebitAmount())).add(nvl(other.getCreditAmount()));
            }
            if (totalAbs.compareTo(BigDecimal.ZERO) == 0) continue;

            for (com.xy6f.fiva_assistant.model.VoucherEntry other : otherEntries) {
                BigDecimal share = nvl(other.getDebitAmount()).add(nvl(other.getCreditAmount()))
                        .multiply(netCash)
                        .divide(totalAbs, 4, java.math.RoundingMode.HALF_UP);
                Account acc = accountById.get(other.getAccountId());
                String category = classifyActivity(acc);
                if (share.compareTo(BigDecimal.ZERO) > 0) {
                    switch (category) {
                        case "INVESTING" -> investingInflow = investingInflow.add(share);
                        case "FINANCING" -> financingInflow = financingInflow.add(share);
                        default -> operatingInflow = operatingInflow.add(share);
                    }
                } else if (share.compareTo(BigDecimal.ZERO) < 0) {
                    BigDecimal out = share.negate();
                    switch (category) {
                        case "INVESTING" -> investingOutflow = investingOutflow.add(out);
                        case "FINANCING" -> financingOutflow = financingOutflow.add(out);
                        default -> operatingOutflow = operatingOutflow.add(out);
                    }
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("period", period);

        result.put("operating", activity(operatingInflow, operatingOutflow));
        result.put("investing", activity(investingInflow, investingOutflow));
        result.put("financing", activity(financingInflow, financingOutflow));

        BigDecimal totalNet = operatingInflow.subtract(operatingOutflow)
                .add(investingInflow.subtract(investingOutflow))
                .add(financingInflow.subtract(financingOutflow));
        result.put("totalNetCashFlow", totalNet);

        return result;
    }

    // ---- 辅助方法 ----

    /**
     * 根据对方科目判断现金流量活动类别
     */
    private String classifyActivity(Account acc) {
        if (acc == null) return "OPERATING";
        String type = acc.getType();
        if ("LIABILITY".equals(type) || "EQUITY".equals(type)) return "FINANCING";
        if ("ASSET".equals(type)) {
            String code = acc.getCode() != null ? acc.getCode() : "";
            // 固定资产/累计折旧/在建工程/无形资产等长期资产 → 投资活动
            if (code.startsWith("16") || code.startsWith("17")) return "INVESTING";
        }
        return "OPERATING";
    }

    private Map<String, Object> activity(BigDecimal inflow, BigDecimal outflow) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("inflow", inflow);
        m.put("outflow", outflow);
        m.put("netCashFlow", inflow.subtract(outflow));
        return m;
    }

    private Map<String, Object> item(String code, String name, BigDecimal amount) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("code", code != null ? code : "");
        m.put("name", name);
        m.put("amount", amount);
        return m;
    }

    /** 负债类汇总（贷-借），写入结果并返回合计 */
    private BigDecimal collectSide(Map<String, Object> result, List<Account> accounts,
                                   Map<Long, BigDecimal[]> balanceMap, String listKey, String totalKey) {
        List<Map<String, Object>> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (Account acc : accounts) {
            BigDecimal[] amt = balanceMap.getOrDefault(acc.getId(), new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            BigDecimal balance = amt[1].subtract(amt[0]);
            if (balance.compareTo(BigDecimal.ZERO) != 0) {
                items.add(item(acc.getCode(), acc.getName(), balance));
                total = total.add(balance);
            }
        }
        result.put(listKey, items);
        result.put(totalKey, total);
        return total;
    }

    private Map<Long, BigDecimal[]> sumBetween(Long userId, LocalDate start, LocalDate end) {
        Map<Long, BigDecimal[]> map = new HashMap<>();
        for (Map<String, Object> row : voucherEntryMapper.sumGroupByAccountBetween(userId, start, end)) {
            Long accId = ((Number) row.get("accountId")).longValue();
            map.put(accId, new BigDecimal[]{toBd(row.get("debit")), toBd(row.get("credit"))});
        }
        return map;
    }

    /** 从按科目聚合的发生额中计算净利润：Σ(收入 贷-借) - Σ(费用 借-贷) */
    private BigDecimal calcNetProfitFromSums(List<Account> accounts, Map<Long, BigDecimal[]> sums) {
        Map<String, String> typeById = new HashMap<>();
        accounts.forEach(a -> typeById.put(String.valueOf(a.getId()), a.getType()));
        BigDecimal revenue = BigDecimal.ZERO, expense = BigDecimal.ZERO;
        for (Map.Entry<Long, BigDecimal[]> e : sums.entrySet()) {
            String type = typeById.get(String.valueOf(e.getKey()));
            if ("REVENUE".equals(type)) {
                revenue = revenue.add(e.getValue()[1].subtract(e.getValue()[0]));
            } else if ("EXPENSE".equals(type)) {
                expense = expense.add(e.getValue()[0].subtract(e.getValue()[1]));
            }
        }
        return revenue.subtract(expense);
    }

    private LocalDate[] parsePeriod(String period) {
        if (period == null || period.isBlank() || "ALL".equals(period)) {
            return new LocalDate[]{LocalDate.of(2020, 1, 1), LocalDate.now()};
        }
        try {
            YearMonth ym = YearMonth.parse(period);
            return new LocalDate[]{ym.atDay(1), ym.atEndOfMonth()};
        } catch (Exception e) {
            YearMonth current = YearMonth.now();
            return new LocalDate[]{current.atDay(1), current.atEndOfMonth()};
        }
    }

    private List<Account> filterByType(List<Account> accounts, String type) {
        return accounts.stream().filter(a -> type.equals(a.getType())).collect(Collectors.toList());
    }

    private BigDecimal toBd(Object o) {
        if (o == null) return BigDecimal.ZERO;
        if (o instanceof BigDecimal bd) return bd;
        return new BigDecimal(o.toString());
    }

    private BigDecimal nvl(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
