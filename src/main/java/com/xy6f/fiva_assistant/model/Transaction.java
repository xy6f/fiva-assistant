package com.xy6f.fiva_assistant.model;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    private Long id;
    private Long userId;
    private String type;
    private BigDecimal amount;
    private String category;
    private String description;
    private LocalDate transactionDate;
    private String counterparty;
    private Long invoiceId;
    private LocalDateTime createdAt;
}
