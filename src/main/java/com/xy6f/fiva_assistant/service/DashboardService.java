package com.xy6f.fiva_assistant.service;

import com.xy6f.fiva_assistant.dto.DashboardSummary;
import com.xy6f.fiva_assistant.mapper.InvoiceMapper;
import com.xy6f.fiva_assistant.mapper.TransactionMapper;
import com.xy6f.fiva_assistant.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final InvoiceMapper invoiceMapper;
    private final TransactionMapper transactionMapper;

    public DashboardSummary getSummary() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime monthStart = now.with(TemporalAdjusters.firstDayOfMonth()).withHour(0).withMinute(0).withSecond(0);
        LocalDate today = LocalDate.now();
        LocalDate monthStartDate = today.withDayOfMonth(1);
        LocalDate yearStart = today.withDayOfYear(1);
        Long userId = UserContext.getUserId();

        // === 发票统计 ===
        Long totalCount = invoiceMapper.countByUserId(userId);
        BigDecimal totalAmount = invoiceMapper.sumTotalAmountSince(LocalDateTime.of(2000, 1, 1, 0, 0), userId);
        BigDecimal totalTax = invoiceMapper.sumTaxAmountSince(LocalDateTime.of(2000, 1, 1, 0, 0), userId);
        Long monthlyCount = invoiceMapper.countSince(monthStart, userId);
        BigDecimal monthlyAmount = invoiceMapper.sumTotalAmountSince(monthStart, userId);
        BigDecimal monthlyTax = invoiceMapper.sumTaxAmountSince(monthStart, userId);

        List<Map<String, Object>> stats = invoiceMapper.categoryStats(userId);
        List<DashboardSummary.CategoryStat> categoryStats = new ArrayList<>();
        for (Map<String, Object> row : stats) {
            String category = row.get("category") != null ? row.get("category").toString() : "未分类";
            Long count = ((Number) row.get("cnt")).longValue();
            BigDecimal amount = (BigDecimal) row.get("amt");
            categoryStats.add(DashboardSummary.CategoryStat.builder()
                    .category(category).count(count).amount(amount).build());
        }

        // === 收支统计 ===
        BigDecimal totalIncome = transactionMapper.sumByTypeSince("INCOME", yearStart, userId);
        BigDecimal totalExpense = transactionMapper.sumByTypeSince("EXPENSE", yearStart, userId);
        BigDecimal monthlyIncome = transactionMapper.sumByTypeBetween("INCOME", monthStartDate, today, userId);
        BigDecimal monthlyExpense = transactionMapper.sumByTypeBetween("EXPENSE", monthStartDate, today, userId);

        // === 月度趋势（近12个月） ===
        List<DashboardSummary.MonthlyTrend> monthlyTrends = new ArrayList<>();
        for (int i = 11; i >= 0; i--) {
            YearMonth ym = YearMonth.now().minusMonths(i);
            LocalDate mStart = ym.atDay(1);
            LocalDate mEnd = ym.atEndOfMonth();
            LocalDateTime mStartLdt = mStart.atStartOfDay();
            LocalDateTime mEndLdt = mEnd.atTime(23, 59, 59);

            BigDecimal invoiceAmt = invoiceMapper.sumTotalAmountBetween(mStartLdt, mEndLdt, userId);
            BigDecimal inc = transactionMapper.sumByTypeBetween("INCOME", mStart, mEnd, userId);
            BigDecimal exp = transactionMapper.sumByTypeBetween("EXPENSE", mStart, mEnd, userId);

            monthlyTrends.add(DashboardSummary.MonthlyTrend.builder()
                    .month(ym.format(DateTimeFormatter.ofPattern("yyyy-MM")))
                    .invoiceAmount(invoiceAmt != null ? invoiceAmt : BigDecimal.ZERO)
                    .income(inc != null ? inc : BigDecimal.ZERO)
                    .expense(exp != null ? exp : BigDecimal.ZERO)
                    .profit((inc != null ? inc : BigDecimal.ZERO).subtract(exp != null ? exp : BigDecimal.ZERO))
                    .build());
        }

        return DashboardSummary.builder()
                .totalInvoiceCount(totalCount)
                .totalAmount(totalAmount != null ? totalAmount : BigDecimal.ZERO)
                .totalTaxAmount(totalTax != null ? totalTax : BigDecimal.ZERO)
                .monthlyInvoiceCount(monthlyCount)
                .monthlyAmount(monthlyAmount != null ? monthlyAmount : BigDecimal.ZERO)
                .monthlyTaxAmount(monthlyTax != null ? monthlyTax : BigDecimal.ZERO)
                .categoryStats(categoryStats)
                .totalIncome(totalIncome != null ? totalIncome : BigDecimal.ZERO)
                .totalExpense(totalExpense != null ? totalExpense : BigDecimal.ZERO)
                .netProfit((totalIncome != null ? totalIncome : BigDecimal.ZERO)
                        .subtract(totalExpense != null ? totalExpense : BigDecimal.ZERO))
                .monthlyIncome(monthlyIncome != null ? monthlyIncome : BigDecimal.ZERO)
                .monthlyExpense(monthlyExpense != null ? monthlyExpense : BigDecimal.ZERO)
                .monthlyProfit((monthlyIncome != null ? monthlyIncome : BigDecimal.ZERO)
                        .subtract(monthlyExpense != null ? monthlyExpense : BigDecimal.ZERO))
                .monthlyTrends(monthlyTrends)
                .build();
    }
}
