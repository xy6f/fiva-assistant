package com.xy6f.fiva_assistant.controller;

import com.xy6f.fiva_assistant.dto.ChatRequest;
import com.xy6f.fiva_assistant.dto.ChatResponse;
import com.xy6f.fiva_assistant.model.ChatMessage;
import com.xy6f.fiva_assistant.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final com.xy6f.fiva_assistant.service.AiCallLogService aiCallLogService;

    /**
     * AI 指标（累计调用/成功率/平均延迟/今日Token + 近7天趋势）
     */
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getMetrics() {
        Long userId = com.xy6f.fiva_assistant.util.UserContext.getUserId();
        Map<String, Object> result = new java.util.HashMap<>();
        var metrics = aiCallLogService.getMetrics(userId);
        result.put("totalCalls", metrics.getTotalCalls());
        result.put("successRate", metrics.getSuccessRate());
        result.put("avgLatencyMs", metrics.getAvgLatencyMs());
        result.put("todayTokens", metrics.getTodayTokens());
        result.put("tokenTrend", aiCallLogService.getDailyTokenTrend(userId));
        return ResponseEntity.ok(result);
    }

    private static final int MAX_MESSAGE_LENGTH = 2000;

    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(@RequestBody ChatRequest request) {
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "消息不能为空"));
        }
        if (request.getMessage().length() > MAX_MESSAGE_LENGTH) {
            return ResponseEntity.badRequest().body(Map.of("error", "消息长度不能超过" + MAX_MESSAGE_LENGTH + "字"));
        }
        ChatResponse response = chatService.chat(request.getSessionId(), request.getMessage());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    public ResponseEntity<List<ChatMessage>> getHistory(@RequestParam String sessionId) {
        return ResponseEntity.ok(chatService.getHistory(sessionId));
    }

    @DeleteMapping("/history")
    public ResponseEntity<?> clearHistory(@RequestParam String sessionId) {
        chatService.clearHistory(sessionId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<?> streamChat(@RequestBody ChatRequest request) {
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "消息不能为空"));
        }
        if (request.getMessage().length() > MAX_MESSAGE_LENGTH) {
            return ResponseEntity.badRequest().body(Map.of("error", "消息长度不能超过" + MAX_MESSAGE_LENGTH + "字"));
        }
        SseEmitter emitter = new SseEmitter(120_000L);
        chatService.chatStream(request.getSessionId(), request.getMessage(), emitter);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .header("X-Accel-Buffering", "no")
                .body(emitter);
    }
}
