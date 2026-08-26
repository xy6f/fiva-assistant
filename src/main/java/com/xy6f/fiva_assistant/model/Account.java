package com.xy6f.fiva_assistant.model;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    private Long id;
    private Long userId;
    private String code;
    private String name;
    private String type;
    private Long parentId;

    @Builder.Default
    private String balanceDirection = "DEBIT";

    @Builder.Default
    private Boolean enabled = true;

    private LocalDateTime createdAt;
}
