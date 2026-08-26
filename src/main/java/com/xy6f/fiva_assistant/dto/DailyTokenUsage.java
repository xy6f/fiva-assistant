package com.xy6f.fiva_assistant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 每日 token 用量（近 7 天趋势）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyTokenUsage {
    private String day;      // MM-dd
    private Long tokens;     // 当日 token 总量
}
