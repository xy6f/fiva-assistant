package com.xy6f.fiva_assistant.service;

import com.xy6f.fiva_assistant.model.Account;
import com.xy6f.fiva_assistant.model.PeriodClose;
import com.xy6f.fiva_assistant.model.Voucher;
import com.xy6f.fiva_assistant.model.VoucherEntry;
import com.xy6f.fiva_assistant.mapper.AccountMapper;
import com.xy6f.fiva_assistant.mapper.PeriodCloseMapper;
import com.xy6f.fiva_assistant.mapper.VoucherEntryMapper;
import com.xy6f.fiva_assistant.mapper.VoucherMapper;
import com.xy6f.fiva_assistant.util.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PeriodCloseService {

    private final VoucherEntryMapper voucherEntryMapper;
    private final VoucherMapper voucherMapper;
    private final AccountMapper accountMapper;
    private final PeriodCloseMapper periodCloseMapper;

    /**
     * 获取结转状态及预览
     */
    public Map<String, Object> getCloseStatus(String period) {
        Long userId = UserContext.getUserId();
        LocalDate[] range = parsePeriod(period);
        LocalDate start = range[0];
        LocalDate end = range[1];
        String normPeriod = normalizePeriod(period);

        // 标记表直接判定
        PeriodClose marker = periodCloseMapper.findByUserIdAndPeriod(userId, normPeriod);

        List<Account> accounts = accountMapper.findByUserIdAndEnabledTrueOrderByCodeAsc(userId);
        List<VoucherEntry> periodEntries = voucherEntryMapper.findAllInPeriod(userId, start, end);

        Map<Long, BigDecimal[]> periodMap = new HashMap<>();
        for (VoucherEntry e : periodEntries) {
            periodMap.computeIfAbsent(e.getAccountId(), k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            periodMap.get(e.getAccountId())[0] = periodMap.get(e.getAccountId())[0].add(e.getDebitAmount());
            periodMap.get(e.getAccountId())[1] = periodMap.get(e.getAccountId())[1].add(e.getCreditAmount());
        }

        List<Map<String, Object>> revenueItems = new ArrayList<>();
        BigDecimal totalRevenue = BigDecimal.ZERO;
        for (Account acc : accounts.stream().filter(a -> "REVENUE".equals(a.getType())).collect(Collectors.toList())) {
            BigDecimal[] amt = periodMap.getOrDefault(acc.getId(), new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            if (amt[1].compareTo(BigDecimal.ZERO) > 0) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("code", acc.getCode());
                item.put("name", acc.getName());
                item.put("creditAmount", amt[1]);
                revenueItems.add(item);
                totalRevenue = totalRevenue.add(amt[1]);
            }
        }

        List<Map<String, Object>> expenseItems = new ArrayList<>();
        BigDecimal totalExpense = BigDecimal.ZERO;
        for (Account acc : accounts.stream().filter(a -> "EXPENSE".equals(a.getType())).collect(Collectors.toList())) {
            BigDecimal[] amt = periodMap.getOrDefault(acc.getId(), new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            if (amt[0].compareTo(BigDecimal.ZERO) > 0) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("code", acc.getCode());
                item.put("name", acc.getName());
                item.put("debitAmount", amt[0]);
                expenseItems.add(item);
                totalExpense = totalExpense.add(amt[0]);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("period", normPeriod);
        result.put("closed", marker != null);
        result.put("closeVoucherId", marker != null ? marker.getCloseVoucherId() : null);
        result.put("closeVoucherCount", marker != null ? 1 : 0);
        result.put("revenueItems", revenueItems);
        result.put("totalRevenue", totalRevenue);
        result.put("expenseItems", expenseItems);
        result.put("totalExpense", totalExpense);
        result.put("netProfit", totalRevenue.subtract(totalExpense));
        return result;
    }

    /**
     * 执行期末结转
     */
    @Transactional
    public Map<String, Object> close(String period) {
        Long userId = UserContext.getUserId();
        LocalDate[] range = parsePeriod(period);
        LocalDate start = range[0];
        LocalDate end = range[1];
        String normPeriod = normalizePeriod(period);

        // 幂等检查：标记表已有则拒绝
        if (periodCloseMapper.findByUserIdAndPeriod(userId, normPeriod) != null) {
            throw new RuntimeException("该期间已经结转，不可重复操作");
        }

        List<Account> accounts = accountMapper.findByUserIdAndEnabledTrueOrderByCodeAsc(userId);
        Account profitAccount = accountMapper.findByUserIdAndCode(userId, "3101");
        if (profitAccount == null) {
            profitAccount = Account.builder()
                    .userId(userId).code("3101").name("本年利润")
                    .type("EQUITY").balanceDirection("CREDIT").enabled(true)
                    .createdAt(LocalDateTime.now())
                    .build();
            accountMapper.insert(profitAccount);
        }

        List<VoucherEntry> periodEntries = voucherEntryMapper.findAllInPeriod(userId, start, end);
        Map<Long, BigDecimal[]> periodMap = new HashMap<>();
        for (VoucherEntry e : periodEntries) {
            periodMap.computeIfAbsent(e.getAccountId(), k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            periodMap.get(e.getAccountId())[0] = periodMap.get(e.getAccountId())[0].add(e.getDebitAmount());
            periodMap.get(e.getAccountId())[1] = periodMap.get(e.getAccountId())[1].add(e.getCreditAmount());
        }

        List<VoucherEntry> closeEntries = new ArrayList<>();
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;

        for (Account acc : accounts.stream().filter(a -> "REVENUE".equals(a.getType())).collect(Collectors.toList())) {
            BigDecimal[] amt = periodMap.getOrDefault(acc.getId(), new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            if (amt[1].compareTo(BigDecimal.ZERO) > 0) {
                closeEntries.add(VoucherEntry.builder()
                        .accountId(acc.getId())
                        .summary("结转" + acc.getName())
                        .debitAmount(amt[1])
                        .creditAmount(BigDecimal.ZERO)
                        .build());
                totalRevenue = totalRevenue.add(amt[1]);
            }
        }

        for (Account acc : accounts.stream().filter(a -> "EXPENSE".equals(a.getType())).collect(Collectors.toList())) {
            BigDecimal[] amt = periodMap.getOrDefault(acc.getId(), new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            if (amt[0].compareTo(BigDecimal.ZERO) > 0) {
                closeEntries.add(VoucherEntry.builder()
                        .accountId(acc.getId())
                        .summary("结转" + acc.getName())
                        .debitAmount(BigDecimal.ZERO)
                        .creditAmount(amt[0])
                        .build());
                totalExpense = totalExpense.add(amt[0]);
            }
        }

        if (closeEntries.isEmpty()) {
            throw new RuntimeException("本期无收入或费用科目发生额，无需结转");
        }

        BigDecimal netProfit = totalRevenue.subtract(totalExpense);
        if (netProfit.compareTo(BigDecimal.ZERO) > 0) {
            closeEntries.add(VoucherEntry.builder()
                    .accountId(profitAccount.getId())
                    .summary("结转本期净利润")
                    .debitAmount(BigDecimal.ZERO)
                    .creditAmount(netProfit)
                    .build());
        } else if (netProfit.compareTo(BigDecimal.ZERO) < 0) {
            closeEntries.add(VoucherEntry.builder()
                    .accountId(profitAccount.getId())
                    .summary("结转本期净亏损")
                    .debitAmount(netProfit.negate())
                    .creditAmount(BigDecimal.ZERO)
                    .build());
        }

        String dateStr = end.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        // 凭证号含 userId 避免全局唯一索引跨用户撞号
        Voucher voucher = Voucher.builder()
                .userId(userId)
                .voucherNo("JZ-" + dateStr + "-" + userId + "-001")
                .type("TRANSFER")
                .amount(totalRevenue.max(totalExpense))
                .category("期末结转")
                .description("期末结转损益 " + normPeriod)
                .voucherDate(end)
                .counterparty("")
                .hasInvoice(false)
                .status("POSTED")
                .build();
        voucherMapper.insert(voucher);

        for (int i = 0; i < closeEntries.size(); i++) {
            closeEntries.get(i).setId(null);
            closeEntries.get(i).setVoucherId(voucher.getId());
            closeEntries.get(i).setSortOrder(i);
            voucherEntryMapper.insert(closeEntries.get(i));
        }

        // 写入结转标记
        periodCloseMapper.insert(PeriodClose.builder()
                .userId(userId)
                .period(normPeriod)
                .closeVoucherId(voucher.getId())
                .createdAt(LocalDateTime.now())
                .build());

        log.info("用户 {} 完成 {} 期间期末结转，凭证: {}", userId, normPeriod, voucher.getVoucherNo());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("voucherNo", voucher.getVoucherNo());
        result.put("totalRevenue", totalRevenue);
        result.put("totalExpense", totalExpense);
        result.put("netProfit", netProfit);
        return result;
    }

    /**
     * 反结转：删除结转标记并冲销自动生成的结转凭证
     */
    @Transactional
    public Map<String, Object> reopen(String period) {
        Long userId = UserContext.getUserId();
        String normPeriod = normalizePeriod(period);

        PeriodClose marker = periodCloseMapper.findByUserIdAndPeriod(userId, normPeriod);
        if (marker == null) {
            throw new RuntimeException("该期间未结转，无需反结转");
        }

        // 删除结转凭证及其分录
        if (marker.getCloseVoucherId() != null) {
            Voucher cv = voucherMapper.findById(marker.getCloseVoucherId());
            if (cv != null && userId.equals(cv.getUserId())) {
                voucherEntryMapper.deleteByVoucherId(cv.getId());
                voucherMapper.deleteById(cv.getId());
            }
        }

        periodCloseMapper.deleteByUserIdAndPeriod(userId, normPeriod);

        log.info("用户 {} 反结转 {} 期间，已撤销凭证 ID {}", userId, normPeriod, marker.getCloseVoucherId());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("period", normPeriod);
        return result;
    }

    // ---------------------------------------------------------------- helpers

    private String normalizePeriod(String period) {
        if (period == null || period.isBlank() || "ALL".equals(period)) {
            return YearMonth.now().toString();
        }
        try {
            return YearMonth.parse(period).toString();
        } catch (Exception e) {
            return YearMonth.now().toString();
        }
    }

    private LocalDate[] parsePeriod(String period) {
        String norm = normalizePeriod(period);
        YearMonth ym = YearMonth.parse(norm);
        return new LocalDate[]{ym.atDay(1), ym.atEndOfMonth()};
    }
}
