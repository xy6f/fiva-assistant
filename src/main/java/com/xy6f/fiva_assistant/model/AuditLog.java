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
public class AuditLog {
    private Long id;
    private Long userId;
    private String username;
    private String action;
    private String targetId;
    private String detail;
    private String ip;
    private Boolean success;
    private LocalDateTime createdAt;
}
