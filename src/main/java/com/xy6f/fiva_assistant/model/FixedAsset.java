package com.xy6f.fiva_assistant.model;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FixedAsset {

    private Long id;
    private Long userId;
    private String assetCode;
    private String assetName;
    private String category;
    private LocalDate purchaseDate;
    private BigDecimal originalValue;

    @Builder.Default
    private BigDecimal salvageValue = BigDecimal.ZERO;

    @Builder.Default
    private String depreciationMethod = "STRAIGHT_LINE";

    private Integer usefulLifeMonths;
    private BigDecimal monthlyDepreciation;

    @Builder.Default
    private BigDecimal accumulatedDepreciation = BigDecimal.ZERO;

    @Builder.Default
    private String status = "IN_USE";

    private Long accountId;
    private Long depreciationAccountId;
    private Long expenseAccountId;

    @Builder.Default
    private Integer depreciatedMonths = 0;

    private LocalDateTime createdAt;

    public BigDecimal getNetValue() {
        return originalValue.subtract(accumulatedDepreciation != null ? accumulatedDepreciation : BigDecimal.ZERO);
    }
}
