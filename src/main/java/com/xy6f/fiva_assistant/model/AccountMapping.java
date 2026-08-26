package com.xy6f.fiva_assistant.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountMapping {
    private Long id;
    private String keyword;
    private String accountCode;
    private String direction;
    private Integer priority;
}
