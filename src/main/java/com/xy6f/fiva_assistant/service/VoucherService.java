package com.xy6f.fiva_assistant.service;

import com.xy6f.fiva_assistant.dto.VoucherSummary;
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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VoucherService {

    private final VoucherMapper voucherMapper;
    private final VoucherEntryMapper voucherEntryMapper;
    private final AccountMapper accountMapper;
    private final com.xy6f.fiva_assistant.mapper.SequenceMapper sequenceMapper;
    private final com.xy6f.fiva_assistant.mapper.PeriodCloseMapper periodCloseMapper;
    private final AccountResolver accountResolver;

    /**
     * 生成凭证编号 PZ-20260616-001。
     * 并发安全：通过 t_sequence 表原子自增实现全局按日序列（跨用户唯一），
     * 避免旧实现"按当日条数+1"在删除/并发场景下的撞号问题。
     */
    private String generateVoucherNo() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String seqName = "VOUCHER_" + dateStr;
        sequenceMapper.initSequence(seqName);
        sequenceMapper.nextSequence(seqName);
        long seq = sequenceMapper.lastInsertId();
        return "PZ-" + dateStr + "-" + String.format("%03d", seq);
    }

    @Transactional
    public Voucher save(Voucher voucher, List<VoucherEntry> entries) {
        validateBalance(entries);
        assertPeriodOpen(voucher.getVoucherDate());
        if (entries != null) {
            Long userId = UserContext.getUserId();
            for (VoucherEntry entry : entries) {
                if (entry.getAccountId() == null
                        || accountMapper.findByIdAndUserId(entry.getAccountId(), userId) == null) {
                    throw new IllegalArgumentException("分录科目不存在或不属于当前用户");
                }
            }
        }
        if (voucher.getVoucherNo() == null || voucher.getVoucherNo().isBlank()) {
            voucher.setVoucherNo(generateVoucherNo());
        }
        voucher.setUserId(UserContext.getUserId());
        voucher.setStatus("DRAFT");
        voucherMapper.insert(voucher);
        Voucher saved = voucher;

        // 保存分录
        if (entries != null) {
            for (int i = 0; i < entries.size(); i++) {
                VoucherEntry entry = entries.get(i);
                entry.setId(null);
                entry.setVoucherId(saved.getId());
                entry.setSortOrder(i);
                voucherEntryMapper.insert(entry);
            }
        }
        saved.setEntries(entries != null ? entries : new ArrayList<>());
        return saved;
    }

    /**
     * 兼容旧接口：单条目保存（自动生成简单分录）
     */
    @Transactional
    public Voucher save(Voucher voucher) {
        // 根据凭证类型自动生成分录
        List<VoucherEntry> entries = new ArrayList<>();
        Long userId = UserContext.getUserId();
        BigDecimal amount = voucher.getAmount() != null ? voucher.getAmount() : BigDecimal.ZERO;

        // 兜底科目：不存在时自动补建，不再静默跳过导致借贷不平
        Account cashAccount = accountResolver.ensureAccount(userId, "1002", "银行存款", "ASSET", "DEBIT");
        Account expenseAccount = accountResolver.ensureAccount(userId, "6602", "管理费用-其他", "EXPENSE", "DEBIT");
        Account incomeAccount = accountResolver.ensureAccount(userId, "5001", "主营业务收入", "REVENUE", "CREDIT");

        if ("INCOME".equals(voucher.getType())) {
            entries.add(VoucherEntry.builder().accountId(cashAccount.getId()).summary(voucher.getDescription()).debitAmount(amount).creditAmount(BigDecimal.ZERO).build());
            entries.add(VoucherEntry.builder().accountId(incomeAccount.getId()).summary(voucher.getDescription()).debitAmount(BigDecimal.ZERO).creditAmount(amount).build());
        } else {
            entries.add(VoucherEntry.builder().accountId(expenseAccount.getId()).summary(voucher.getDescription()).debitAmount(amount).creditAmount(BigDecimal.ZERO).build());
            entries.add(VoucherEntry.builder().accountId(cashAccount.getId()).summary(voucher.getDescription()).debitAmount(BigDecimal.ZERO).creditAmount(amount).build());
        }

        return save(voucher, entries);
    }

    @Transactional
    public Voucher update(Long id, Voucher updated, List<VoucherEntry> entries) {
        Long userId = UserContext.getUserId();
        Voucher v = voucherMapper.findById(id);
        if (v == null) throw new RuntimeException("凭证不存在");
        if (!userId.equals(v.getUserId())) throw new RuntimeException("无权操作此凭证");
        if ("POSTED".equals(v.getStatus())) {
            throw new RuntimeException("已过账的凭证不可编辑");
        }
        assertPeriodOpen(v.getVoucherDate());
        if (updated.getVoucherDate() != null) {
            assertPeriodOpen(updated.getVoucherDate());
        }
        v.setType(updated.getType());
        v.setAmount(updated.getAmount());
        v.setCategory(updated.getCategory());
        v.setDescription(updated.getDescription());
        v.setVoucherDate(updated.getVoucherDate());
        v.setCounterparty(updated.getCounterparty());
        v.setHasInvoice(updated.getHasInvoice());
        v.setInvoiceImageUrl(updated.getInvoiceImageUrl());
        voucherMapper.update(v);
        Voucher saved = v;

        // 更新分录：先删后增
        if (entries != null) {
            validateBalance(entries);
            for (VoucherEntry entry : entries) {
                if (entry.getAccountId() == null
                        || accountMapper.findByIdAndUserId(entry.getAccountId(), userId) == null) {
                    throw new IllegalArgumentException("分录科目不存在或不属于当前用户");
                }
            }
            voucherEntryMapper.deleteByVoucherId(saved.getId());
            for (int i = 0; i < entries.size(); i++) {
                VoucherEntry entry = entries.get(i);
                entry.setId(null);
                entry.setVoucherId(saved.getId());
                entry.setSortOrder(i);
                voucherEntryMapper.insert(entry);
            }
        }
        saved.setEntries(entries != null ? entries : getEntriesByVoucherId(saved.getId()));
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        Long userId = UserContext.getUserId();
        Voucher v = voucherMapper.findById(id);
        if (v == null || !userId.equals(v.getUserId())) throw new RuntimeException("凭证不存在或无权访问");
        if ("POSTED".equals(v.getStatus())) {
            throw new RuntimeException("已过账的凭证不可删除");
        }
        if ("PERIOD_CLOSING".equals(v.getType())) {
            throw new RuntimeException("期末结转凭证不可直接删除，请先反结账");
        }
        assertPeriodOpen(v.getVoucherDate());
        voucherEntryMapper.deleteByVoucherId(id);
        voucherMapper.deleteById(id);
    }

    public Voucher getById(Long id) {
        Long userId = UserContext.getUserId();
        Voucher v = voucherMapper.findById(id);
        if (v == null || !userId.equals(v.getUserId())) throw new RuntimeException("凭证不存在或无权访问");
        v.setEntries(getEntriesByVoucherId(v.getId()));
        return v;
    }

    public List<Voucher> listAll() {
        List<Voucher> vouchers = voucherMapper.findByUserIdOrderByVoucherDateDescCreatedAtDesc(UserContext.getUserId());
        fillEntries(vouchers);
        return vouchers;
    }

    /**
     * 分页查询（统一 PageResult 结构）
     */
    public com.xy6f.fiva_assistant.dto.PageResult<Voucher> pageByFilter(
            String type, String status, LocalDate start, LocalDate end, int page, int pageSize) {
        Long userId = UserContext.getUserId();
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(pageSize, 1), 100);
        List<Voucher> vouchers = voucherMapper.findPage(userId, blankToNull(type), blankToNull(status), start, end,
                safeSize, (safePage - 1) * safeSize);
        long total = voucherMapper.countPage(userId, blankToNull(type), blankToNull(status), start, end);
        fillEntries(vouchers);
        return com.xy6f.fiva_assistant.dto.PageResult.of(vouchers, total, safePage, safeSize);
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    public List<Voucher> listAllWithEntries() {
        return listAll();
    }

    public List<Voucher> listByType(String type) {
        List<Voucher> vouchers = voucherMapper.findByUserIdAndTypeOrderByVoucherDateDesc(UserContext.getUserId(), type);
        fillEntries(vouchers);
        return vouchers;
    }

    public List<Voucher> listByStatus(String status) {
        List<Voucher> vouchers = voucherMapper.findByUserIdAndStatusOrderByVoucherDateDescCreatedAtDesc(
                UserContext.getUserId(), status);
        fillEntries(vouchers);
        return vouchers;
    }

    public List<Voucher> listByDateRange(LocalDate start, LocalDate end) {
        List<Voucher> vouchers = voucherMapper.findByUserIdAndVoucherDateBetweenOrderByVoucherDateDesc(
                UserContext.getUserId(), start, end);
        fillEntries(vouchers);
        return vouchers;
    }

    public List<Voucher> listByDateRangeAndStatus(LocalDate start, LocalDate end, String status) {
        List<Voucher> vouchers = voucherMapper.findByUserIdAndVoucherDateBetweenAndStatusOrderByVoucherDateDesc(
                UserContext.getUserId(), start, end, status);
        fillEntries(vouchers);
        return vouchers;
    }

    /**
     * 审核流程：提交审核 DRAFT -> REVIEWED
     */
    @Transactional
    public Voucher submitForReview(Long id) {
        Long userId = UserContext.getUserId();
        Voucher v = voucherMapper.findById(id);
        if (v == null || !userId.equals(v.getUserId())) throw new RuntimeException("凭证不存在或无权访问");
        if (!"DRAFT".equals(v.getStatus())) {
            throw new RuntimeException("只有草稿状态可以提交审核");
        }
        List<VoucherEntry> entries = getEntriesByVoucherId(id);
        validateBalance(entries);
        v.setStatus("REVIEWED");
        voucherMapper.update(v);
        return v;
    }

    @Transactional
    public Voucher approve(Long id) {
        Long userId = UserContext.getUserId();
        Voucher v = voucherMapper.findById(id);
        if (v == null || !userId.equals(v.getUserId())) throw new RuntimeException("凭证不存在或无权访问");
        if (!"REVIEWED".equals(v.getStatus())) {
            throw new RuntimeException("只有已审核状态可以通过");
        }
        v.setStatus("POSTED");
        voucherMapper.update(v);
        return v;
    }

    @Transactional
    public Voucher reject(Long id) {
        Long userId = UserContext.getUserId();
        Voucher v = voucherMapper.findById(id);
        if (v == null || !userId.equals(v.getUserId())) throw new RuntimeException("凭证不存在或无权访问");
        if (!"REVIEWED".equals(v.getStatus())) {
            throw new RuntimeException("只有已审核状态可以驳回");
        }
        v.setStatus("DRAFT");
        voucherMapper.update(v);
        return v;
    }

    // ---- 辅助方法 ----

    private List<VoucherEntry> getEntriesByVoucherId(Long voucherId) {
        List<VoucherEntry> entries = voucherEntryMapper.findByVoucherIdOrderBySortOrderAsc(voucherId);
        // 填充科目信息
        if (!entries.isEmpty()) {
            Long userId = UserContext.getUserId();
            List<Account> accounts = accountMapper.findByUserIdAndEnabledTrueOrderByCodeAsc(userId);
            Map<Long, Account> accountMap = accounts.stream().collect(Collectors.toMap(Account::getId, a -> a));
            for (VoucherEntry entry : entries) {
                entry.setAccount(accountMap.get(entry.getAccountId()));
            }
        }
        return entries;
    }

    private void fillEntries(List<Voucher> vouchers) {
        if (vouchers.isEmpty()) return;
        List<Long> voucherIds = vouchers.stream().map(Voucher::getId).collect(Collectors.toList());
        List<VoucherEntry> allEntries = voucherEntryMapper.findByVoucherIdIn(voucherIds);
        // 填充科目信息
        Long userId = UserContext.getUserId();
        List<Account> accounts = accountMapper.findByUserIdAndEnabledTrueOrderByCodeAsc(userId);
        Map<Long, Account> accountMap = accounts.stream().collect(Collectors.toMap(Account::getId, a -> a));
        for (VoucherEntry entry : allEntries) {
            entry.setAccount(accountMap.get(entry.getAccountId()));
        }
        Map<Long, List<VoucherEntry>> entryMap = allEntries.stream()
                .collect(Collectors.groupingBy(VoucherEntry::getVoucherId));
        for (Voucher v : vouchers) {
            v.setEntries(entryMap.getOrDefault(v.getId(), new ArrayList<>()));
        }
    }

    /**
     * 若期间已结转，禁止新增/修改/删除该期间凭证
     */
    private void assertPeriodOpen(LocalDate date) {
        if (date == null) return;
        Long userId = UserContext.getUserId();
        String period = YearMonth.from(date).toString();
        if (periodCloseMapper.findByUserIdAndPeriod(userId, period) != null) {
            throw new IllegalArgumentException("期间 " + period + " 已结转，不可新增或变更凭证");
        }
    }

    /**
     * 校验借贷平衡：借方合计 = 贷方合计
     */
    private void validateBalance(List<VoucherEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            throw new RuntimeException("凭证至少需要一条分录");
        }
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        for (VoucherEntry entry : entries) {
            if (entry.getDebitAmount() == null) entry.setDebitAmount(BigDecimal.ZERO);
            if (entry.getCreditAmount() == null) entry.setCreditAmount(BigDecimal.ZERO);
            totalDebit = totalDebit.add(entry.getDebitAmount());
            totalCredit = totalCredit.add(entry.getCreditAmount());
        }
        if (totalDebit.compareTo(totalCredit) != 0) {
            throw new RuntimeException(String.format("借贷不平衡！借方合计: %s, 贷方合计: %s", totalDebit, totalCredit));
        }
        if (totalDebit.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("凭证金额不能为零");
        }
    }

    public VoucherSummary getSummary() {
        LocalDate now = LocalDate.now();
        LocalDate monthStart = now.withDayOfMonth(1);
        LocalDate yearStart = now.withDayOfYear(1);

        Long userId = UserContext.getUserId();
        BigDecimal totalIncome = voucherMapper.sumByTypeSince("INCOME", yearStart, userId);
        BigDecimal totalExpense = voucherMapper.sumByTypeSince("EXPENSE", yearStart, userId);
        BigDecimal monthlyIncome = voucherMapper.sumByTypeBetween("INCOME", monthStart, now, userId);
        BigDecimal monthlyExpense = voucherMapper.sumByTypeBetween("EXPENSE", monthStart, now, userId);

        List<Map<String, Object>> incomeStats = voucherMapper.categorySumByTypeSince("INCOME", yearStart, userId);
        List<Map<String, Object>> expenseStats = voucherMapper.categorySumByTypeSince("EXPENSE", yearStart, userId);

        List<VoucherSummary.CategoryStat> incomeCats = new ArrayList<>();
        for (Map<String, Object> row : incomeStats) {
            incomeCats.add(VoucherSummary.CategoryStat.builder()
                    .category(row.get("category") != null ? row.get("category").toString() : "其他")
                    .amount((BigDecimal) row.get("amt"))
                    .build());
        }

        List<VoucherSummary.CategoryStat> expenseCats = new ArrayList<>();
        for (Map<String, Object> row : expenseStats) {
            expenseCats.add(VoucherSummary.CategoryStat.builder()
                    .category(row.get("category") != null ? row.get("category").toString() : "其他")
                    .amount((BigDecimal) row.get("amt"))
                    .build());
        }

        List<Map<String, Object>> monthlyData = voucherMapper.monthlyIncomeExpense(yearStart, now, userId);
        List<VoucherSummary.MonthlyData> trend = new ArrayList<>();
        for (Map<String, Object> row : monthlyData) {
            int yyyymm = ((Number) row.get("m")).intValue();
            int month = yyyymm % 100;
            BigDecimal income = row.get("income") != null ? (BigDecimal) row.get("income") : BigDecimal.ZERO;
            BigDecimal expense = row.get("expense") != null ? (BigDecimal) row.get("expense") : BigDecimal.ZERO;
            trend.add(VoucherSummary.MonthlyData.builder()
                    .month(month)
                    .income(income)
                    .expense(expense)
                    .profit(income.subtract(expense))
                    .build());
        }

        return VoucherSummary.builder()
                .totalIncome(totalIncome != null ? totalIncome : BigDecimal.ZERO)
                .totalExpense(totalExpense != null ? totalExpense : BigDecimal.ZERO)
                .netProfit((totalIncome != null ? totalIncome : BigDecimal.ZERO)
                        .subtract(totalExpense != null ? totalExpense : BigDecimal.ZERO))
                .monthlyIncome(monthlyIncome != null ? monthlyIncome : BigDecimal.ZERO)
                .monthlyExpense(monthlyExpense != null ? monthlyExpense : BigDecimal.ZERO)
                .monthlyProfit((monthlyIncome != null ? monthlyIncome : BigDecimal.ZERO)
                        .subtract(monthlyExpense != null ? monthlyExpense : BigDecimal.ZERO))
                .incomeCategoryStats(incomeCats)
                .expenseCategoryStats(expenseCats)
                .monthlyTrend(trend)
                .build();
    }
}
