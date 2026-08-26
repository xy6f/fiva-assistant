package com.xy6f.fiva_assistant.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiCallLog {
    private Long id;
    private Long userId;
    private String callType;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer latencyMs;
    private Boolean success;
    private String errorMsg;
    private LocalDateTime createdAt;
}
