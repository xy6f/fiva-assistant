package com.xy6f.fiva_assistant.model;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalaryRecord {

    private Long id;
    private Long userId;
    private String employeeName;
    private String employeeNo;
    private String department;
    private String position;

    @Builder.Default
    private BigDecimal baseSalary = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal bonus = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal allowance = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal overtime = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal grossPay = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal socialInsurance = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal housingFund = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal tax = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal otherDeduction = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal netPay = BigDecimal.ZERO;

    private String month;

    @Builder.Default
    private String status = "DRAFT";

    private LocalDateTime createdAt;

    public void calculatePay() {
        if (baseSalary == null) baseSalary = BigDecimal.ZERO;
        if (bonus == null) bonus = BigDecimal.ZERO;
        if (allowance == null) allowance = BigDecimal.ZERO;
        if (overtime == null) overtime = BigDecimal.ZERO;
        if (socialInsurance == null) socialInsurance = BigDecimal.ZERO;
        if (housingFund == null) housingFund = BigDecimal.ZERO;
        if (tax == null) tax = BigDecimal.ZERO;
        if (otherDeduction == null) otherDeduction = BigDecimal.ZERO;

        grossPay = baseSalary.add(bonus).add(allowance).add(overtime);
        netPay = grossPay.subtract(socialInsurance).subtract(housingFund).subtract(tax).subtract(otherDeduction);
    }
}
