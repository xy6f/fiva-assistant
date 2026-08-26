package com.xy6f.fiva_assistant.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 期间结转标记：某用户某期间已执行期末结转
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeriodClose {
    private Long id;
    private Long userId;
    private String period;
    private Long closeVoucherId;
    private LocalDateTime createdAt;
}
