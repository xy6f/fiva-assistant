package com.xy6f.fiva_assistant.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxRule {
    private Long id;
    private String ruleKey;
    private String taxType;
    private BigDecimal rateValue;
    private BigDecimal thresholdValue;
    private String description;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
}
