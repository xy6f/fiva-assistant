package com.xy6f.fiva_assistant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxCalculation {

    // === 输入参数 ===
    private BigDecimal annualRevenue;        // 年营业收入
    private BigDecimal annualCost;           // 年营业成本
    private BigDecimal annualExpenses;       // 年期间费用（管理/销售/财务）
    private int employeeCount;               // 员工人数
    private Boolean smallScale;              // 是否小规模纳税人

    // === 增值税计算 ===
    private BigDecimal vatRate;              // 适用税率
    private BigDecimal vatPayable;           // 应纳增值税
    private BigDecimal vatWithInputDeduction;// 进项税额抵扣（一般纳税人）
    private BigDecimal vatExemptAmount;      // 免征额（小规模纳税人）

    // === 附加税 ===
    private BigDecimal urbanMaintenanceTax;  // 城建税（7%/5%/1%）
    private BigDecimal educationSurcharge;   // 教育费附加（3%）
    private BigDecimal localEducationSurcharge; // 地方教育附加（2%）

    // === 企业所得税 ===
    private BigDecimal taxableIncome;        // 应纳税所得额
    private BigDecimal incomeTaxRate;        // 所得税税率
    private BigDecimal incomeTaxPayable;     // 应纳企业所得税
    private BigDecimal incomeTaxDiscount;    // 优惠减免金额

    // === 税负分析 ===
    private BigDecimal totalTaxBurden;       // 税费合计
    private BigDecimal taxBurdenRate;        // 综合税负率
    private BigDecimal netProfit;            // 净利润
    private BigDecimal profitMargin;         // 净利润率

    // === AI 筹划建议 ===
    private String suggestion;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Suggestion {
        private String title;
        private String detail;
        private String savingAmount;
    }
}
