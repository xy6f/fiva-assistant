package com.xy6f.fiva_assistant.service.tax;

import com.xy6f.fiva_assistant.dto.TaxCalculation;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 企业所得税策略：利润总额 = 收入 - 成本 - 费用 - 税金及附加；
 * 小型微利优惠（应纳税所得额≤300万按5%实际税负）参数来自 t_tax_rule。
 */
@Component
@Order(3)
@RequiredArgsConstructor
public class IncomeTaxStrategy implements TaxCalcStrategy {

    private final TaxRuleEngine engine;

    @Override
    public boolean supports(TaxCalculation input) {
        return true;
    }

    @Override
    public void apply(TaxCalculation input) {
        BigDecimal revenue = nvl(input.getAnnualRevenue());
        BigDecimal cost = nvl(input.getAnnualCost());
        BigDecimal expenses = nvl(input.getAnnualExpenses());

        BigDecimal totalSurcharges = nvl(input.getUrbanMaintenanceTax())
                .add(nvl(input.getEducationSurcharge()))
                .add(nvl(input.getLocalEducationSurcharge()));

        BigDecimal profitBeforeTax = revenue.subtract(cost).subtract(expenses).subtract(totalSurcharges);
        if (profitBeforeTax.compareTo(BigDecimal.ZERO) < 0) {
            profitBeforeTax = BigDecimal.ZERO;
        }
        input.setTaxableIncome(profitBeforeTax);

        BigDecimal smallThreshold = engine.threshold("CIT_SMALL_THRESHOLD", new BigDecimal("3000000"));
        BigDecimal incomeTaxRate;
        BigDecimal incomeTax;
        BigDecimal discount = BigDecimal.ZERO;

        if (profitBeforeTax.compareTo(smallThreshold) <= 0) {
            // 小型微利企业实际税负（如 5%）
            incomeTaxRate = engine.rate("CIT_SMALL_EFFECTIVE_RATE", new BigDecimal("0.05"));
            BigDecimal fullTax = profitBeforeTax.multiply(engine.rate("CIT_STANDARD_RATE", new BigDecimal("0.25")))
                    .setScale(2, RoundingMode.HALF_UP);
            incomeTax = profitBeforeTax.multiply(incomeTaxRate).setScale(2, RoundingMode.HALF_UP);
            discount = fullTax.subtract(incomeTax);
        } else {
            incomeTaxRate = engine.rate("CIT_STANDARD_RATE", new BigDecimal("0.25"));
            incomeTax = profitBeforeTax.multiply(incomeTaxRate).setScale(2, RoundingMode.HALF_UP);
        }
        input.setIncomeTaxRate(incomeTaxRate);
        input.setIncomeTaxPayable(incomeTax);
        input.setIncomeTaxDiscount(discount);
    }

    private BigDecimal nvl(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
