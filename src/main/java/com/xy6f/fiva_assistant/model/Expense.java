package com.xy6f.fiva_assistant.model;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Expense {

    private Long id;
    private Long userId;
    private String title;
    private String applicant;
    private String category;
    private BigDecimal amount;
    private LocalDate expenseDate;
    private String description;
    private String invoicePath;

    @Builder.Default
    private String status = "PENDING";

    private String approver;
    private String approvalOpinion;
    private LocalDateTime approvedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
