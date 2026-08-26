package com.xy6f.fiva_assistant.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubjectTemplate {
    private Long id;
    private String code;
    private String name;
    private String type;
    private String balanceDirection;
    private LocalDateTime createdAt;
}
