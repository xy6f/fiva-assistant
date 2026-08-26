package com.xy6f.fiva_assistant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 指标聚合结果（/api/chat/metrics）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiMetrics {
    private Long totalCalls;        // 累计调用次数
    private Long successCalls;      // 成功调用次数（内部计算用）
    private Double successRate;     // 成功率 %
    private Double avgLatencyMs;    // 平均延迟 ms
    private Long todayTokens;       // 今日 token 用量
}
