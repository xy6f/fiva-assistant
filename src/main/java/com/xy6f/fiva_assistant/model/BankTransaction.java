package com.xy6f.fiva_assistant.model;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankTransaction {

    private Long id;
    private Long userId;
    private String bankName;
    private String accountNo;
    private LocalDate transactionDate;
    private String counterparty;
    private String summary;

    @Builder.Default
    private BigDecimal debitAmount = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal creditAmount = BigDecimal.ZERO;

    private BigDecimal balance;

    @Builder.Default
    private String status = "UNMATCHED";

    private Long voucherId;
    private LocalDateTime createdAt;
}
