package com.xy6f.fiva_assistant.service;

import com.xy6f.fiva_assistant.dto.TransactionSummary;
import com.xy6f.fiva_assistant.model.Transaction;
import com.xy6f.fiva_assistant.mapper.TransactionMapper;
import com.xy6f.fiva_assistant.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionMapper transactionMapper;

    @Transactional
    public Transaction save(Transaction transaction) {
        transaction.setUserId(UserContext.getUserId());
        transactionMapper.insert(transaction);
        return transaction;
    }

    @Transactional
    public Transaction update(Long id, Transaction updated) {
        Long userId = UserContext.getUserId();
        Transaction t = transactionMapper.findById(id);
        if (t == null || !userId.equals(t.getUserId())) throw new RuntimeException("记录不存在或无权访问");
        t.setType(updated.getType());
        t.setAmount(updated.getAmount());
        t.setCategory(updated.getCategory());
        t.setDescription(updated.getDescription());
        t.setTransactionDate(updated.getTransactionDate());
        t.setCounterparty(updated.getCounterparty());
        transactionMapper.update(t);
        return t;
    }

    @Transactional
    public void delete(Long id) {
        Long userId = UserContext.getUserId();
        Transaction t = transactionMapper.findById(id);
        if (t == null || !userId.equals(t.getUserId())) throw new RuntimeException("记录不存在或无权访问");
        transactionMapper.deleteById(id);
    }

    public Transaction getById(Long id) {
        Long userId = UserContext.getUserId();
        Transaction existing = transactionMapper.findById(id);
        if (existing == null || !userId.equals(existing.getUserId())) throw new RuntimeException("记录不存在或无权访问");
        return existing;
    }

    public List<Transaction> listAll() {
        return transactionMapper.findByUserIdOrderByTransactionDateDescCreatedAtDesc(UserContext.getUserId());
    }

    public List<Transaction> listByType(String type) {
        return transactionMapper.findByUserIdAndTypeOrderByTransactionDateDesc(UserContext.getUserId(), type);
    }

    public List<Transaction> listByDateRange(LocalDate start, LocalDate end) {
        return transactionMapper.findByUserIdAndTransactionDateBetweenOrderByTransactionDateDesc(UserContext.getUserId(), start, end);
    }

    public TransactionSummary getSummary() {
        LocalDate now = LocalDate.now();
        LocalDate monthStart = now.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate yearStart = now.withDayOfYear(1);
        Long userId = UserContext.getUserId();

        BigDecimal totalIncome = transactionMapper.sumByTypeSince("INCOME", yearStart, userId);
        BigDecimal totalExpense = transactionMapper.sumByTypeSince("EXPENSE", yearStart, userId);
        BigDecimal monthlyIncome = transactionMapper.sumByTypeBetween("INCOME", monthStart, now, userId);
        BigDecimal monthlyExpense = transactionMapper.sumByTypeBetween("EXPENSE", monthStart, now, userId);

        // Category stats
        List<Map<String, Object>> incomeStats = transactionMapper.categorySumByTypeSince("INCOME", yearStart, userId);
        List<Map<String, Object>> expenseStats = transactionMapper.categorySumByTypeSince("EXPENSE", yearStart, userId);

        List<TransactionSummary.CategoryStat> incomeCats = new ArrayList<>();
        for (Map<String, Object> row : incomeStats) {
            incomeCats.add(TransactionSummary.CategoryStat.builder()
                    .category(row.get("category") != null ? row.get("category").toString() : "其他")
                    .amount((BigDecimal) row.get("amt"))
                    .build());
        }

        List<TransactionSummary.CategoryStat> expenseCats = new ArrayList<>();
        for (Map<String, Object> row : expenseStats) {
            expenseCats.add(TransactionSummary.CategoryStat.builder()
                    .category(row.get("category") != null ? row.get("category").toString() : "其他")
                    .amount((BigDecimal) row.get("amt"))
                    .build());
        }

        // Monthly trend (current year)
        LocalDate yearEnd = now;
        List<Map<String, Object>> monthlyData = transactionMapper.monthlyIncomeExpense(yearStart, yearEnd, userId);
        List<TransactionSummary.MonthlyData> trend = new ArrayList<>();
        for (Map<String, Object> row : monthlyData) {
            int yyyymm = ((Number) row.get("m")).intValue();
            int month = yyyymm % 100;
            BigDecimal income = row.get("income") != null ? (BigDecimal) row.get("income") : BigDecimal.ZERO;
            BigDecimal expense = row.get("expense") != null ? (BigDecimal) row.get("expense") : BigDecimal.ZERO;
            trend.add(TransactionSummary.MonthlyData.builder()
                    .month(month)
                    .income(income)
                    .expense(expense)
                    .profit(income.subtract(expense))
                    .build());
        }

        return TransactionSummary.builder()
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
