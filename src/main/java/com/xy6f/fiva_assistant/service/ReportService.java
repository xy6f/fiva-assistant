package com.xy6f.fiva_assistant.service;

import com.xy6f.fiva_assistant.mapper.TransactionMapper;
import com.xy6f.fiva_assistant.util.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReportService {

    private final TransactionMapper transactionMapper;

    /**
     * 生成综合财务报表
     */
    public Map<String, Object> generateReport(String period) {
        Map<String, Object> report = new LinkedHashMap<>();

        // 根据周期计算起止日期
        LocalDate startDate = getStartDate(period);
        LocalDate endDate = LocalDate.now();
        Long userId = UserContext.getUserId();

        report.put("period", period);
        report.put("generatedAt", LocalDate.now().toString());

        // === 利润表简表（基于记账数据） ===
        Map<String, Object> profitStatement = new LinkedHashMap<>();

        BigDecimal totalIncome = transactionMapper.sumByTypeBetween("INCOME", startDate, endDate, userId);
        BigDecimal totalExpense = transactionMapper.sumByTypeBetween("EXPENSE", startDate, endDate, userId);
        if (totalIncome == null) totalIncome = BigDecimal.ZERO;
        if (totalExpense == null) totalExpense = BigDecimal.ZERO;
        BigDecimal grossProfit = totalIncome.subtract(totalExpense);

        profitStatement.put("totalIncome", totalIncome);
        profitStatement.put("totalExpense", totalExpense);
        profitStatement.put("grossProfit", grossProfit);
        report.put("profitStatement", profitStatement);

        // === 税费汇总 ===
        Map<String, Object> taxSummary = new LinkedHashMap<>();
        // 估算增值税（收入的13%）
        BigDecimal estimatedVat = totalIncome.multiply(new BigDecimal("0.13")).setScale(2, java.math.RoundingMode.HALF_UP);
        taxSummary.put("estimatedVat", estimatedVat);
        // 附加税（增值税的12%）
        BigDecimal surcharge = estimatedVat.multiply(new BigDecimal("0.12")).setScale(2, java.math.RoundingMode.HALF_UP);
        taxSummary.put("surcharge", surcharge);
        // 企业所得税（利润的5%小微优惠）
        BigDecimal incomeTax = grossProfit.compareTo(BigDecimal.ZERO) > 0 
                ? grossProfit.multiply(new BigDecimal("0.05")).setScale(2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        taxSummary.put("incomeTax", incomeTax);
        // 税费合计
        BigDecimal totalTaxBurden = estimatedVat.add(surcharge).add(incomeTax);
        taxSummary.put("totalTaxBurden", totalTaxBurden);
        report.put("taxSummary", taxSummary);

        // === 收入分类统计 ===
        List<Map<String, Object>> incomeCategories = transactionMapper.categorySumByTypeSince("INCOME", startDate, userId);
        List<Map<String, Object>> incomeList = new ArrayList<>();
        for (Map<String, Object> row : incomeCategories) {
            Map<String, Object> item = new HashMap<>();
            item.put("category", row.get("category"));
            item.put("total", row.get("amt"));
            incomeList.add(item);
        }
        report.put("incomeCategories", incomeList);

        // === 费用分类统计 ===
        List<Map<String, Object>> expenseCategories = transactionMapper.categorySumByTypeSince("EXPENSE", startDate, userId);
        List<Map<String, Object>> expenseList = new ArrayList<>();
        for (Map<String, Object> row : expenseCategories) {
            Map<String, Object> item = new HashMap<>();
            item.put("category", row.get("category"));
            item.put("total", row.get("amt"));
            expenseList.add(item);
        }
        report.put("expenseCategories", expenseList);

        // === 月度趋势（最近6个月） ===
        List<Map<String, Object>> monthlyTrend = new ArrayList<>();
        YearMonth current = YearMonth.now();
        LocalDate halfYearAgo = current.minusMonths(5).atDay(1);
        List<Map<String, Object>> monthlyData = transactionMapper.monthlyIncomeExpense(halfYearAgo, endDate, userId);
        
        // 将查询结果转为 Map (key = YYYYMM)
        Map<Integer, Map<String, Object>> monthlyMap = new HashMap<>();
        for (Map<String, Object> row : monthlyData) {
            monthlyMap.put(((Number) row.get("m")).intValue(), row);
        }
        
        for (int i = 5; i >= 0; i--) {
            YearMonth ym = current.minusMonths(i);
            Map<String, Object> month = new LinkedHashMap<>();
            month.put("month", ym.toString());
            
            int key = ym.getYear() * 100 + ym.getMonthValue();
            Map<String, Object> data = monthlyMap.get(key);
            if (data != null) {
                month.put("income", data.get("income"));
                month.put("expense", data.get("expense"));
            } else {
                month.put("income", BigDecimal.ZERO);
                month.put("expense", BigDecimal.ZERO);
            }
            monthlyTrend.add(month);
        }
        report.put("monthlyTrend", monthlyTrend);

        return report;
    }

    private LocalDate getStartDate(String period) {
        if (period == null || "ALL".equals(period)) {
            return LocalDate.of(2020, 1, 1);
        }
        try {
            YearMonth ym = YearMonth.parse(period);
            return ym.atDay(1);
        } catch (Exception e) {
            return YearMonth.now().atDay(1);
        }
    }
}
