package com.xy6f.fiva_assistant.model;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice {

    private Long id;
    private Long userId;
    private String invoiceNo;
    private LocalDate invoiceDate;
    private String sellerName;
    private String buyerName;
    private BigDecimal amount;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private String category;
    private String imagePath;
    private String ocrRawText;

    @Builder.Default
    private String status = "PENDING";

    private String invoiceCode;
    private String checkCode;
    private Boolean verified;
    private LocalDateTime verifiedAt;
    private String verifyMessage;
    private LocalDateTime createdAt;
}
