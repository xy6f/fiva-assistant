package com.xy6f.fiva_assistant.service;

import com.xy6f.fiva_assistant.model.Account;
import com.xy6f.fiva_assistant.model.Voucher;
import com.xy6f.fiva_assistant.model.VoucherEntry;
import com.xy6f.fiva_assistant.mapper.AccountMapper;
import com.xy6f.fiva_assistant.mapper.VoucherEntryMapper;
import com.xy6f.fiva_assistant.mapper.VoucherMapper;
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
public class LedgerService {

    private final VoucherEntryMapper voucherEntryMapper;
    private final VoucherMapper voucherMapper;
    private final AccountMapper accountMapper;

    /**
     * 明细账：指定科目在某期间的所有分录
     */
    public Map<String, Object> getDetailLedger(Long accountId, String period) {
        Long userId = UserContext.getUserId();
        Account account = accountMapper.findById(accountId);
        if (account == null) throw new RuntimeException("科目不存在");

        LocalDate[] range = parsePeriod(period);
        LocalDate start = range[0];
        LocalDate end = range[1];

        // 查询期初（该科目在期间之前的所有分录）
        List<VoucherEntry> beforeEntries = voucherEntryMapper.findBeforePeriodByAccount(accountId, userId, start);
        BigDecimal openingBalance = calcBalance(beforeEntries, account.getBalanceDirection());

        // 查询本期分录
        List<VoucherEntry> entries = voucherEntryMapper.findDetailLedger(accountId, userId, start, end);

        // 填充凭证信息
        List<Long> voucherIds = entries.stream().map(VoucherEntry::getVoucherId).distinct().collect(Collectors.toList());
        Map<Long, Voucher> voucherMap = new HashMap<>();
        if (!voucherIds.isEmpty()) {
            voucherMapper.findAllById(voucherIds).forEach(v -> voucherMap.put(v.getId(), v));
        }

        // 构建返回数据
        List<Map<String, Object>> items = new ArrayList<>();
        BigDecimal periodDebit = BigDecimal.ZERO;
        BigDecimal periodCredit = BigDecimal.ZERO;

        for (VoucherEntry entry : entries) {
            Voucher voucher = voucherMap.get(entry.getVoucherId());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", voucher != null ? voucher.getVoucherDate() : null);
            item.put("voucherNo", voucher != null ? voucher.getVoucherNo() : null);
            item.put("summary", entry.getSummary());
            item.put("debitAmount", entry.getDebitAmount());
            item.put("creditAmount", entry.getCreditAmount());
            // 计算逐笔余额
            if ("DEBIT".equals(account.getBalanceDirection())) {
                openingBalance = openingBalance.add(entry.getDebitAmount()).subtract(entry.getCreditAmount());
            } else {
                openingBalance = openingBalance.add(entry.getCreditAmount()).subtract(entry.getDebitAmount());
            }
            item.put("balance", openingBalance);
            item.put("direction", openingBalance.compareTo(BigDecimal.ZERO) >= 0 ? account.getBalanceDirection() : "OPPOSITE");
            items.add(item);

            periodDebit = periodDebit.add(entry.getDebitAmount());
            periodCredit = periodCredit.add(entry.getCreditAmount());
        }

        // 重新计算期初余额（不修改之前的openingBalance）
        List<VoucherEntry> beforeEntries2 = voucherEntryMapper.findBeforePeriodByAccount(accountId, userId, start);
        BigDecimal opening = calcBalance(beforeEntries2, account.getBalanceDirection());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("account", account);
        result.put("period", period);
        result.put("openingBalance", opening);
        result.put("periodDebit", periodDebit);
        result.put("periodCredit", periodCredit);
        BigDecimal closing;
        if ("DEBIT".equals(account.getBalanceDirection())) {
            closing = opening.add(periodDebit).subtract(periodCredit);
        } else {
            closing = opening.add(periodCredit).subtract(periodDebit);
        }
        result.put("closingBalance", closing);
        result.put("items", items);
        return result;
    }

    /**
     * 总账：按科目汇总某期间的借方合计、贷方合计、余额
     */
    public Map<String, Object> getGeneralLedger(String period) {
        Long userId = UserContext.getUserId();
        LocalDate[] range = parsePeriod(period);
        LocalDate start = range[0];
        LocalDate end = range[1];

        List<Account> accounts = accountMapper.findByUserIdAndEnabledTrueOrderByCodeAsc(userId);
        List<VoucherEntry> periodEntries = voucherEntryMapper.findAllInPeriod(userId, start, end);
        List<VoucherEntry> beforeEntries = voucherEntryMapper.findAllBeforePeriod(userId, start);

        // 按科目汇总本期发生额
        Map<Long, BigDecimal[]> periodMap = new HashMap<>();
        for (VoucherEntry e : periodEntries) {
            periodMap.computeIfAbsent(e.getAccountId(), k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            periodMap.get(e.getAccountId())[0] = periodMap.get(e.getAccountId())[0].add(e.getDebitAmount());
            periodMap.get(e.getAccountId())[1] = periodMap.get(e.getAccountId())[1].add(e.getCreditAmount());
        }

        // 按科目汇总期初余额
        Map<Long, BigDecimal> openingMap = new HashMap<>();
        for (VoucherEntry e : beforeEntries) {
            openingMap.merge(e.getAccountId(),
                    e.getDebitAmount().subtract(e.getCreditAmount()),
                    BigDecimal::add);
        }

        List<Map<String, Object>> items = new ArrayList<>();
        for (Account acc : accounts) {
            BigDecimal[] periodAmt = periodMap.getOrDefault(acc.getId(), new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            BigDecimal opening = openingMap.getOrDefault(acc.getId(), BigDecimal.ZERO);
            BigDecimal closing = opening.add(periodAmt[0]).subtract(periodAmt[1]);

            // 如果该科目本期无发生额且期初为零，跳过（可配置是否显示零发生额科目）
            if (periodAmt[0].compareTo(BigDecimal.ZERO) == 0 && periodAmt[1].compareTo(BigDecimal.ZERO) == 0 && opening.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("accountCode", acc.getCode());
            item.put("accountName", acc.getName());
            item.put("accountType", acc.getType());
            item.put("balanceDirection", acc.getBalanceDirection());
            item.put("openingBalance", opening);
            item.put("periodDebit", periodAmt[0]);
            item.put("periodCredit", periodAmt[1]);
            item.put("closingBalance", closing);
            items.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("period", period);
        result.put("items", items);
        return result;
    }

    /**
     * 科目余额表：所有科目的期初余额、本期借方、本期贷方、期末余额
     */
    public Map<String, Object> getTrialBalance(String period) {
        Long userId = UserContext.getUserId();
        LocalDate[] range = parsePeriod(period);
        LocalDate start = range[0];
        LocalDate end = range[1];

        List<Account> accounts = accountMapper.findByUserIdAndEnabledTrueOrderByCodeAsc(userId);
        List<VoucherEntry> periodEntries = voucherEntryMapper.findAllInPeriod(userId, start, end);
        List<VoucherEntry> beforeEntries = voucherEntryMapper.findAllBeforePeriod(userId, start);

        // 按科目汇总本期发生额
        Map<Long, BigDecimal[]> periodMap = new HashMap<>();
        for (VoucherEntry e : periodEntries) {
            periodMap.computeIfAbsent(e.getAccountId(), k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            periodMap.get(e.getAccountId())[0] = periodMap.get(e.getAccountId())[0].add(e.getDebitAmount());
            periodMap.get(e.getAccountId())[1] = periodMap.get(e.getAccountId())[1].add(e.getCreditAmount());
        }

        // 按科目汇总期初余额
        Map<Long, BigDecimal> openingMap = new HashMap<>();
        for (VoucherEntry e : beforeEntries) {
            openingMap.merge(e.getAccountId(),
                    e.getDebitAmount().subtract(e.getCreditAmount()),
                    BigDecimal::add);
        }

        List<Map<String, Object>> items = new ArrayList<>();
        BigDecimal totalOpeningDebit = BigDecimal.ZERO, totalOpeningCredit = BigDecimal.ZERO;
        BigDecimal totalPeriodDebit = BigDecimal.ZERO, totalPeriodCredit = BigDecimal.ZERO;
        BigDecimal totalClosingDebit = BigDecimal.ZERO, totalClosingCredit = BigDecimal.ZERO;

        for (Account acc : accounts) {
            BigDecimal[] periodAmt = periodMap.getOrDefault(acc.getId(), new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            BigDecimal opening = openingMap.getOrDefault(acc.getId(), BigDecimal.ZERO);
            BigDecimal closing = opening.add(periodAmt[0]).subtract(periodAmt[1]);

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("accountCode", acc.getCode());
            item.put("accountName", acc.getName());
            item.put("accountType", acc.getType());

            // 期初余额（借方为正，贷方为负）
            if (opening.compareTo(BigDecimal.ZERO) >= 0) {
                item.put("openingDebit", opening);
                item.put("openingCredit", BigDecimal.ZERO);
                totalOpeningDebit = totalOpeningDebit.add(opening);
            } else {
                item.put("openingDebit", BigDecimal.ZERO);
                item.put("openingCredit", opening.negate());
                totalOpeningCredit = totalOpeningCredit.add(opening.negate());
            }

            item.put("periodDebit", periodAmt[0]);
            item.put("periodCredit", periodAmt[1]);
            totalPeriodDebit = totalPeriodDebit.add(periodAmt[0]);
            totalPeriodCredit = totalPeriodCredit.add(periodAmt[1]);

            // 期末余额
            if (closing.compareTo(BigDecimal.ZERO) >= 0) {
                item.put("closingDebit", closing);
                item.put("closingCredit", BigDecimal.ZERO);
                totalClosingDebit = totalClosingDebit.add(closing);
            } else {
                item.put("closingDebit", BigDecimal.ZERO);
                item.put("closingCredit", closing.negate());
                totalClosingCredit = totalClosingCredit.add(closing.negate());
            }

            items.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("period", period);
        result.put("items", items);

        // 合计行
        Map<String, Object> totals = new LinkedHashMap<>();
        totals.put("openingDebit", totalOpeningDebit);
        totals.put("openingCredit", totalOpeningCredit);
        totals.put("periodDebit", totalPeriodDebit);
        totals.put("periodCredit", totalPeriodCredit);
        totals.put("closingDebit", totalClosingDebit);
        totals.put("closingCredit", totalClosingCredit);
        result.put("totals", totals);
        return result;
    }

    // ---- 辅助方法 ----

    /**
     * 解析期间字符串为起止日期
     * 格式: "2026-06" 或 "2026" 或 "ALL"
     */
    private LocalDate[] parsePeriod(String period) {
        if (period == null || period.isBlank() || "ALL".equals(period)) {
            return new LocalDate[]{LocalDate.of(2020, 1, 1), LocalDate.now()};
        }
        try {
            YearMonth ym = YearMonth.parse(period);
            return new LocalDate[]{ym.atDay(1), ym.atEndOfMonth()};
        } catch (Exception e) {
            // 尝试解析为年份
            try {
                int year = Integer.parseInt(period);
                return new LocalDate[]{LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31)};
            } catch (Exception e2) {
                YearMonth current = YearMonth.now();
                return new LocalDate[]{current.atDay(1), current.atEndOfMonth()};
            }
        }
    }

    /**
     * 根据余额方向计算余额
     */
    private BigDecimal calcBalance(List<VoucherEntry> entries, String balanceDirection) {
        BigDecimal debit = BigDecimal.ZERO;
        BigDecimal credit = BigDecimal.ZERO;
        for (VoucherEntry e : entries) {
            debit = debit.add(e.getDebitAmount());
            credit = credit.add(e.getCreditAmount());
        }
        if ("DEBIT".equals(balanceDirection)) {
            return debit.subtract(credit);
        } else {
            return credit.subtract(debit);
        }
    }
}
