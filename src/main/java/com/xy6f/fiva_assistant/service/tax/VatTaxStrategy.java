package com.xy6f.fiva_assistant.service.tax;

import com.xy6f.fiva_assistant.dto.TaxCalculation;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 增值税策略：一般纳税人默认 13%（销项-进项，留抵不为负）；
 * 小规模纳税人征收率 3%，年销售额在免征额度内免征。
 * 参数全部来自 t_tax_rule。
 */
@Component
@Order(1)
@RequiredArgsConstructor
public class VatTaxStrategy implements TaxCalcStrategy {

    private final TaxRuleEngine engine;

    @Override
    public boolean supports(TaxCalculation input) {
        return true;
    }

    @Override
    public void apply(TaxCalculation input) {
        BigDecimal revenue = nvl(input.getAnnualRevenue());
        BigDecimal cost = nvl(input.getAnnualCost());

        BigDecimal vatPayable;
        BigDecimal vatRate;
        if (Boolean.TRUE.equals(input.getSmallScale())) {
            vatRate = engine.rate("VAT_SMALL_SCALE_RATE", new BigDecimal("0.03"));
            BigDecimal exemptThreshold = engine.threshold("VAT_SMALL_SCALE_EXEMPT_ANNUAL", new BigDecimal("300000"));
            if (revenue.compareTo(exemptThreshold) <= 0) {
                vatPayable = BigDecimal.ZERO;
                input.setVatExemptAmount(revenue);
            } else {
                vatPayable = revenue.multiply(vatRate).setScale(2, RoundingMode.HALF_UP);
                input.setVatExemptAmount(BigDecimal.ZERO);
            }
        } else {
            vatRate = engine.rate("VAT_GENERAL_RATE", new BigDecimal("0.13"));
            BigDecimal outputTax = revenue.multiply(vatRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal inputTax = cost.multiply(vatRate).setScale(2, RoundingMode.HALF_UP);
            vatPayable = outputTax.subtract(inputTax);
            if (vatPayable.compareTo(BigDecimal.ZERO) < 0) {
                vatPayable = BigDecimal.ZERO; // 留抵
            }
            input.setVatWithInputDeduction(inputTax);
        }
        input.setVatRate(vatRate);
        input.setVatPayable(vatPayable);
    }

    private BigDecimal nvl(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
