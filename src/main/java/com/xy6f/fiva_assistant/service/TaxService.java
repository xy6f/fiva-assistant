package com.xy6f.fiva_assistant.service;

import com.xy6f.fiva_assistant.dto.TaxCalculation;
import com.xy6f.fiva_assistant.service.tax.TaxCalcStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 税费计算服务。
 * AI 税负建议/深度分析已迁移至 Python LangChain 服务（ai-agent，/api/tax/suggestion*、/api/tax/analysis）。
 */
@Service
@RequiredArgsConstructor
public class TaxService {

    private final List<TaxCalcStrategy> taxStrategies;

    /**
     * 计算企业税费（由策略链依次处理，参数来自财税规则表）
     */
    public TaxCalculation calculate(TaxCalculation input) {
        for (TaxCalcStrategy strategy : taxStrategies) {
            if (strategy.supports(input)) {
                strategy.apply(input);
            }
        }

        BigDecimal revenue = input.getAnnualRevenue() != null ? input.getAnnualRevenue() : BigDecimal.ZERO;

        // === 税负分析 ===
        BigDecimal vatPayable = nvl(input.getVatPayable());
        BigDecimal urbanTax = nvl(input.getUrbanMaintenanceTax());
        BigDecimal eduSurcharge = nvl(input.getEducationSurcharge());
        BigDecimal localEduSurcharge = nvl(input.getLocalEducationSurcharge());
        BigDecimal incomeTax = nvl(input.getIncomeTaxPayable());

        BigDecimal totalTax = vatPayable.add(urbanTax).add(eduSurcharge).add(localEduSurcharge).add(incomeTax);
        input.setTotalTaxBurden(totalTax);

        BigDecimal taxBurdenRate = revenue.compareTo(BigDecimal.ZERO) > 0
                ? totalTax.divide(revenue, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
                : BigDecimal.ZERO;
        input.setTaxBurdenRate(taxBurdenRate.setScale(2, RoundingMode.HALF_UP));

        BigDecimal profitBeforeTax = input.getTaxableIncome() != null ? input.getTaxableIncome() : BigDecimal.ZERO;
        BigDecimal netProfit = profitBeforeTax.subtract(incomeTax);
        input.setNetProfit(netProfit);

        BigDecimal profitMargin = revenue.compareTo(BigDecimal.ZERO) > 0
                ? netProfit.divide(revenue, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
                : BigDecimal.ZERO;
        input.setProfitMargin(profitMargin.setScale(2, RoundingMode.HALF_UP));

        return input;
    }

    private BigDecimal nvl(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
