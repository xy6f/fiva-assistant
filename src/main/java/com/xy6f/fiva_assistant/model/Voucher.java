package com.xy6f.fiva_assistant.model;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Voucher {

    private Long id;
    private Long userId;
    private String voucherNo;
    private String type;
    private BigDecimal amount;
    private String category;
    private String description;
    private LocalDate voucherDate;
    private String counterparty;

    @Builder.Default
    private Boolean hasInvoice = false;

    private String invoiceImageUrl;

    @Builder.Default
    private String status = "DRAFT";

    private LocalDateTime createdAt;

    private List<VoucherEntry> entries;
}
