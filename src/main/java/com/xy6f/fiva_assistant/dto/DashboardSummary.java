package com.xy6f.fiva_assistant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummary {
    // 发票统计
    private Long totalInvoiceCount;
    private BigDecimal totalAmount;
    private BigDecimal totalTaxAmount;
    private Long monthlyInvoiceCount;
    private BigDecimal monthlyAmount;
    private BigDecimal monthlyTaxAmount;
    private List<CategoryStat> categoryStats;

    // 收支统计
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal netProfit;
    private BigDecimal monthlyIncome;
    private BigDecimal monthlyExpense;
    private BigDecimal monthlyProfit;

    // 月度趋势（近12个月）
    private List<MonthlyTrend> monthlyTrends;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryStat {
        private String category;
        private Long count;
        private BigDecimal amount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyTrend {
        private String month;      // yyyy-MM
        private BigDecimal invoiceAmount;
        private BigDecimal income;
        private BigDecimal expense;
        private BigDecimal profit;
    }
}
