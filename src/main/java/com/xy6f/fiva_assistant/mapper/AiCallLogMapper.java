package com.xy6f.fiva_assistant.mapper;

import com.xy6f.fiva_assistant.dto.AiMetrics;
import com.xy6f.fiva_assistant.dto.DailyTokenUsage;
import com.xy6f.fiva_assistant.model.AiCallLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface AiCallLogMapper {

    @Insert("INSERT INTO t_ai_call_log(user_id, call_type, prompt_tokens, completion_tokens, latency_ms, success, error_msg) " +
            "VALUES(#{userId}, #{callType}, #{promptTokens}, #{completionTokens}, #{latencyMs}, #{success}, #{errorMsg})")
    int insert(AiCallLog log);

    @Select("SELECT COUNT(*) AS totalCalls, " +
            "SUM(CASE WHEN success = 1 THEN 1 ELSE 0 END) AS successCalls, " +
            "AVG(latency_ms) AS avgLatencyMs, " +
            "SUM(CASE WHEN DATE(created_at) = CURDATE() THEN prompt_tokens + completion_tokens ELSE 0 END) AS todayTokens " +
            "FROM t_ai_call_log WHERE user_id = #{userId}")
    AiMetrics aggregateByUserId(Long userId);

    @Select("SELECT DATE_FORMAT(created_at, '%m-%d') AS day, SUM(prompt_tokens + completion_tokens) AS tokens " +
            "FROM t_ai_call_log " +
            "WHERE user_id = #{userId} AND created_at >= CURDATE() - INTERVAL 6 DAY " +
            "GROUP BY DATE_FORMAT(created_at, '%m-%d'), DATE(created_at) ORDER BY DATE(created_at)")
    List<DailyTokenUsage> dailyTokenTrend(Long userId);
}
