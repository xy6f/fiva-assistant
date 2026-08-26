package com.xy6f.fiva_assistant.service.tax;

import com.xy6f.fiva_assistant.dto.TaxCalculation;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 附加税策略：城建税/教育费附加/地方教育附加，以应纳增值税为计税基础。
 */
@Component
@Order(2)
@RequiredArgsConstructor
public class SurchargesTaxStrategy implements TaxCalcStrategy {

    private final TaxRuleEngine engine;

    @Override
    public boolean supports(TaxCalculation input) {
        return true;
    }

    @Override
    public void apply(TaxCalculation input) {
        BigDecimal vatPayable = input.getVatPayable() != null ? input.getVatPayable() : BigDecimal.ZERO;
        BigDecimal urbanTax = vatPayable.multiply(engine.rate("SURCHARGE_URBAN_RATE", new BigDecimal("0.07")))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal eduSurcharge = vatPayable.multiply(engine.rate("SURCHARGE_EDU_RATE", new BigDecimal("0.03")))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal localEduSurcharge = vatPayable.multiply(engine.rate("SURCHARGE_LOCAL_EDU_RATE", new BigDecimal("0.02")))
                .setScale(2, RoundingMode.HALF_UP);
        input.setUrbanMaintenanceTax(urbanTax);
        input.setEducationSurcharge(eduSurcharge);
        input.setLocalEducationSurcharge(localEduSurcharge);
    }
}
