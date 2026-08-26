package com.xy6f.fiva_assistant.model;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Budget {

    private Long id;
    private Long userId;
    private String department;
    private Integer year;
    private Integer month;
    private String category;
    private BigDecimal budgetAmount;

    @Builder.Default
    private BigDecimal usedAmount = BigDecimal.ZERO;

    private Integer alertThreshold = 80;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public BigDecimal getUsagePercent() {
        if (budgetAmount == null || budgetAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return usedAmount.multiply(new BigDecimal("100"))
                .divide(budgetAmount, 2, java.math.RoundingMode.HALF_UP);
    }

    public boolean isOverBudget() {
        return usedAmount.compareTo(budgetAmount) > 0;
    }

    public boolean isAlertTriggered() {
        if (alertThreshold == null) return false;
        return getUsagePercent().compareTo(new BigDecimal(alertThreshold)) >= 0;
    }
}
