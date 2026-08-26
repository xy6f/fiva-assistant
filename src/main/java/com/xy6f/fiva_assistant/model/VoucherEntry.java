package com.xy6f.fiva_assistant.model;

import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoucherEntry {

    private Long id;
    private Long voucherId;
    private Long accountId;
    private String summary;

    @Builder.Default
    private BigDecimal debitAmount = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal creditAmount = BigDecimal.ZERO;

    @Builder.Default
    private Integer sortOrder = 0;

    private Account account;
}
