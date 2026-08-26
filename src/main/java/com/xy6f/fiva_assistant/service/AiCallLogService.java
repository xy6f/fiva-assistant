package com.xy6f.fiva_assistant.service;

import com.xy6f.fiva_assistant.dto.AiMetrics;
import com.xy6f.fiva_assistant.dto.DailyTokenUsage;
import com.xy6f.fiva_assistant.mapper.AiCallLogMapper;
import com.xy6f.fiva_assistant.model.AiCallLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AI 调用日志：埋点记录 + 指标聚合。
 * 记录操作永不抛异常、不阻塞主流程。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiCallLogService {

    private final AiCallLogMapper aiCallLogMapper;

    public static final String TYPE_CHAT = "CHAT";
    public static final String TYPE_OCR = "OCR";
    public static final String TYPE_TAX_SUGGEST = "TAX_SUGGEST";
    public static final String TYPE_TAX_ANALYSIS = "TAX_ANALYSIS";

    /**
     * 异步安全落库（同步执行但吞掉一切异常，避免影响主流程）
     */
    public void record(Long userId, String callType, Integer promptTokens, Integer completionTokens,
                       long latencyMs, boolean success, String errorMsg) {
        try {
            aiCallLogMapper.insert(AiCallLog.builder()
                    .userId(userId)
                    .callType(callType)
                    .promptTokens(promptTokens != null ? promptTokens : 0)
                    .completionTokens(completionTokens != null ? completionTokens : 0)
                    .latencyMs((int) Math.min(latencyMs, Integer.MAX_VALUE))
                    .success(success)
                    .errorMsg(errorMsg != null && errorMsg.length() > 500 ? errorMsg.substring(0, 500) : errorMsg)
                    .build());
        } catch (Exception e) {
            log.warn("Failed to record AI call log: {}", e.getMessage());
        }
    }

    public AiMetrics getMetrics(Long userId) {
        AiMetrics m = aiCallLogMapper.aggregateByUserId(userId);
        if (m == null) {
            m = AiMetrics.builder().totalCalls(0L).successRate(100.0).avgLatencyMs(0.0).todayTokens(0L).build();
        }
        if (m.getTotalCalls() == null) m.setTotalCalls(0L);
        if (m.getSuccessCalls() == null) {
            m.setSuccessRate(100.0);
        } else {
            m.setSuccessRate(m.getTotalCalls() > 0
                    ? Math.round(m.getSuccessCalls() * 1000.0 / m.getTotalCalls()) / 10.0
                    : 100.0);
        }
        if (m.getAvgLatencyMs() == null) m.setAvgLatencyMs(0.0);
        if (m.getTodayTokens() == null) m.setTodayTokens(0L);
        return m;
    }

    /**
     * 近 7 天每日 token 用量；缺失日期补零，保证前端连续展示
     */
    public List<DailyTokenUsage> getDailyTokenTrend(Long userId) {
        List<DailyTokenUsage> data = aiCallLogMapper.dailyTokenTrend(userId);
        List<DailyTokenUsage> result = new java.util.ArrayList<>();
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("MM-dd");
        for (int i = 6; i >= 0; i--) {
            String day = today.minusDays(i).format(fmt);
            result.add(data.stream()
                    .filter(d -> day.equals(d.getDay()))
                    .findFirst()
                    .orElse(new DailyTokenUsage(day, 0L)));
        }
        return result;
    }
}
