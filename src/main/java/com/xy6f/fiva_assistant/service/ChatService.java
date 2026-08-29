package com.xy6f.fiva_assistant.service;

import com.xy6f.fiva_assistant.ai.FivaAssistant;
import com.xy6f.fiva_assistant.ai.FivaDataTools;
import com.xy6f.fiva_assistant.dto.ChatResponse;
import com.xy6f.fiva_assistant.mapper.AccountMapper;
import com.xy6f.fiva_assistant.mapper.BudgetMapper;
import com.xy6f.fiva_assistant.mapper.ChatMessageMapper;
import com.xy6f.fiva_assistant.mapper.InvoiceMapper;
import com.xy6f.fiva_assistant.mapper.VoucherEntryMapper;
import com.xy6f.fiva_assistant.mapper.VoucherMapper;
import com.xy6f.fiva_assistant.model.ChatMessage;
import com.xy6f.fiva_assistant.util.UserContext;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class ChatService {

    private final ChatMessageMapper chatMessageMapper;
    private final OpenAiStreamingChatModel streamingChatModel;
    private final VoucherMapper voucherMapper;
    private final VoucherEntryMapper voucherEntryMapper;
    private final AccountMapper accountMapper;
    private final InvoiceMapper invoiceMapper;
    private final BudgetMapper budgetMapper;
    private final AiCallLogService aiCallLogService;

    /** 会话内存记忆：key=sessionId。带 TTL(30min) + maxSize(200) 淘汰 */
    private final ConcurrentHashMap<String, ChatMemory> memoryCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> memoryAccessTime = new ConcurrentHashMap<>();
    private static final long MEMORY_TTL_MS = 30 * 60 * 1000L;
    private static final int MEMORY_MAX_ENTRIES = 200;

    private static final int MEMORY_MAX_MESSAGES = 20;

    private static final String SYSTEM_PROMPT = """
            你是一个专业的财税AI助手，名叫"财税小助手"。你的职责是帮助企业进行财务管理和税务筹划。

            你的能力包括：
            1. 解答财务记账相关问题（如会计分录、凭证处理等）
            2. 解答税务申报相关问题（如增值税、企业所得税、个人所得税等）
            3. 提供税务筹划建议（合法合规的节税方案）
            4. 解读财税政策和法规
            5. 查询用户的真实账务数据并进行分析

            重要规则：
            - 当用户询问涉及他自己的真实财务数据时（如收入、支出、利润、凭证、发票、预算、科目余额等），你必须调用提供的查询工具获取真实数据，基于工具返回的数据回答，绝对不要编造数字。
            - 工具返回的数据已经是当前登录用户自己的数据，直接使用即可。
            - 如果涉及具体税率或政策，请提醒用户以当地税务机关最新规定为准。
            - 回答要专业、准确、简洁，使用中文。
            - 如果用户的问题不涉及财税领域，请礼貌地引导回财税话题。
            """;

    public ChatService(ChatMessageMapper chatMessageMapper,
                       @Qualifier("streamingChatModel") OpenAiStreamingChatModel streamingChatModel,
                       VoucherMapper voucherMapper,
                       VoucherEntryMapper voucherEntryMapper,
                       AccountMapper accountMapper,
                       InvoiceMapper invoiceMapper,
                       BudgetMapper budgetMapper,
                       AiCallLogService aiCallLogService) {
        this.chatMessageMapper = chatMessageMapper;
        this.streamingChatModel = streamingChatModel;
        this.voucherMapper = voucherMapper;
        this.voucherEntryMapper = voucherEntryMapper;
        this.accountMapper = accountMapper;
        this.invoiceMapper = invoiceMapper;
        this.budgetMapper = budgetMapper;
        this.aiCallLogService = aiCallLogService;
    }

    /**
     * 构造携带当前用户 ID 的助手实例（含只读数据查询工具）。
     * userId 在请求线程捕获，SSE 异步回调线程中不依赖 ThreadLocal。
     */
    private FivaAssistant buildAssistant(Long userId) {
        return AiServices.builder(FivaAssistant.class)
                .streamingChatModel(streamingChatModel)
                .systemMessage(SYSTEM_PROMPT)
                .tools(new FivaDataTools(userId, voucherMapper, voucherEntryMapper, accountMapper, invoiceMapper, budgetMapper))
                .chatMemoryProvider(memoryId -> getMemory((String) memoryId, userId))
                .build();
    }

    /**
     * 获取会话记忆；首次访问时用 DB 历史记录种子化。
     * 带 TTL 和 maxSize 淘汰防止内存泄漏。
     */
    private ChatMemory getMemory(String sessionId, Long userId) {
        evictStaleMemory();
        memoryAccessTime.put(sessionId, System.currentTimeMillis());
        return memoryCache.computeIfAbsent(sessionId, id -> {
            if (memoryCache.size() >= MEMORY_MAX_ENTRIES) {
                evictOldest();
            }
            MessageWindowChatMemory memory = MessageWindowChatMemory.withMaxMessages(MEMORY_MAX_MESSAGES);
            try {
                List<dev.langchain4j.data.message.ChatMessage> dbHistory = loadHistoryAsLangChainMessages(sessionId, userId);
                dbHistory.forEach(memory::add);
            } catch (Exception e) {
                log.warn("Failed to seed memory from DB for session {}: {}", sessionId, e.getMessage());
            }
            return memory;
        });
    }

    private void evictStaleMemory() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, Long>> it = memoryAccessTime.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Long> entry = it.next();
            if (now - entry.getValue() > MEMORY_TTL_MS) {
                memoryCache.remove(entry.getKey());
                it.remove();
            }
        }
    }

    private void evictOldest() {
        String oldestKey = memoryAccessTime.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
        if (oldestKey != null) {
            memoryCache.remove(oldestKey);
            memoryAccessTime.remove(oldestKey);
        }
    }

    private List<dev.langchain4j.data.message.ChatMessage> loadHistoryAsLangChainMessages(String sessionId, Long userId) {
        List<com.xy6f.fiva_assistant.model.ChatMessage> history =
                chatMessageMapper.findBySessionIdAndUserIdOrderByCreatedAtAsc(sessionId, userId);
        List<dev.langchain4j.data.message.ChatMessage> result = new ArrayList<>();
        int startIdx = Math.max(0, history.size() - MEMORY_MAX_MESSAGES);
        for (int i = startIdx; i < history.size(); i++) {
            com.xy6f.fiva_assistant.model.ChatMessage msg = history.get(i);
            if ("user".equals(msg.getRole())) {
                result.add(UserMessage.from(msg.getContent()));
            } else if ("assistant".equals(msg.getRole())) {
                result.add(AiMessage.from(msg.getContent()));
            }
        }
        return result;
    }

    /**
     * 保存一轮对话到数据库（成功后调用）。
     */
    private void persistTurn(String sessionId, Long userId, String userText, String assistantText) {
        chatMessageMapper.insert(ChatMessage.builder()
                .sessionId(sessionId).role("user").content(userText).userId(userId).build());
        chatMessageMapper.insert(ChatMessage.builder()
                .sessionId(sessionId).role("assistant").content(assistantText).userId(userId).build());
    }

    public ChatResponse chat(String sessionId, String message) {
        final String finalSessionId = (sessionId == null || sessionId.isBlank()) ? UUID.randomUUID().toString() : sessionId;
        final Long userId = UserContext.getUserId();

        StringBuffer fullResponse = new StringBuffer();
        CountDownLatch latch = new CountDownLatch(1);
        long startMs = System.currentTimeMillis();
        final Integer[] usage = {null, null};

        FivaAssistant assistant = buildAssistant(userId);
        TokenStream stream = assistant.chat(finalSessionId, message);
        stream.onPartialResponse(fullResponse::append)
                .onCompleteResponse(response -> {
                    captureUsage(response, usage);
                    latch.countDown();
                })
                .onError(error -> {
                    log.error("Chat AI error", error);
                    // 清空该会话内存记忆，下次从 DB 重新种子化（本轮未落库不会污染上下文）
                    memoryCache.remove(finalSessionId);
                    memoryAccessTime.remove(finalSessionId);
                    aiCallLogService.record(userId, AiCallLogService.TYPE_CHAT, null, null,
                            System.currentTimeMillis() - startMs, false, error.getMessage());
                    latch.countDown();
                })
                .start();

        try {
            if (!latch.await(120, TimeUnit.SECONDS)) {
                memoryCache.remove(finalSessionId);
                memoryAccessTime.remove(finalSessionId);
                aiCallLogService.record(userId, AiCallLogService.TYPE_CHAT, usage[0], usage[1],
                        System.currentTimeMillis() - startMs, false, "timeout");
                throw new RuntimeException("AI 回复超时，请稍后重试");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("请求被中断", e);
        }

        String aiResponse = fullResponse.toString();
        if (aiResponse.isBlank()) {
            throw new RuntimeException("AI 未返回内容，请稍后重试");
        }

        persistTurn(finalSessionId, userId, message, aiResponse);
        aiCallLogService.record(userId, AiCallLogService.TYPE_CHAT, usage[0], usage[1],
                System.currentTimeMillis() - startMs, true, null);

        return ChatResponse.builder()
                .sessionId(finalSessionId)
                .role("assistant")
                .content(aiResponse)
                .build();
    }

    public List<ChatMessage> getHistory(String sessionId) {
        return chatMessageMapper.findBySessionIdAndUserIdOrderByCreatedAtAsc(sessionId, UserContext.getUserId());
    }

    @Transactional
    public void clearHistory(String sessionId) {
        chatMessageMapper.deleteBySessionIdAndUserId(sessionId, UserContext.getUserId());
        memoryCache.remove(sessionId);
        memoryAccessTime.remove(sessionId);
    }

    /**
     * 流式聊天，通过 SSE 实时推送。支持工具调用：模型可按需查询用户真实账务数据。
     * 用户消息与 AI 回复在成功完成后一并落库；失败时清空会话记忆避免污染。
     */
    public void chatStream(String sessionId, String message, SseEmitter emitter) {
        final String finalSessionId = (sessionId == null || sessionId.isBlank()) ? UUID.randomUUID().toString() : sessionId;
        final Long userId = UserContext.getUserId();

        try {
            emitter.send(SseEmitter.event().name("session").data(finalSessionId));
        } catch (Exception e) {
            log.error("Failed to send session id", e);
            emitter.completeWithError(e);
            return;
        }

        StringBuffer fullResponse = new StringBuffer();
        long startMs = System.currentTimeMillis();
        final Integer[] usage = {null, null};

        FivaAssistant assistant = buildAssistant(userId);
        TokenStream stream = assistant.chat(finalSessionId, message);
        stream.onPartialResponse(partial -> {
                    fullResponse.append(partial);
                    try {
                        emitter.send(SseEmitter.event().name("token").data(partial));
                    } catch (Exception e) {
                        log.error("Failed to send token", e);
                    }
                })
                .onCompleteResponse(response -> {
                    String content = fullResponse.toString();
                    log.info("Chat AI stream response for session {}: {}", finalSessionId, content);

                    captureUsage(response, usage);
                    aiCallLogService.record(userId, AiCallLogService.TYPE_CHAT, usage[0], usage[1],
                            System.currentTimeMillis() - startMs, true, null);

                    persistTurn(finalSessionId, userId, message, content);

                    try {
                        emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                        emitter.complete();
                    } catch (Exception e) {
                        log.error("Failed to complete SSE", e);
                        emitter.completeWithError(e);
                    }
                })
                .onError(error -> {
                    log.error("Chat AI stream error", error);
                    memoryCache.remove(finalSessionId);
                    memoryAccessTime.remove(finalSessionId);
                    aiCallLogService.record(userId, AiCallLogService.TYPE_CHAT, usage[0], usage[1],
                            System.currentTimeMillis() - startMs, false, error.getMessage());
                    try {
                        String safeMsg = sanitizeErrorMessage(error);
                        emitter.send(SseEmitter.event().name("error").data(safeMsg));
                        emitter.completeWithError(error);
                    } catch (Exception e) {
                        emitter.completeWithError(error);
                    }
                })
                .start();
    }

    /**
     * 从响应元数据中尽力提取 token 用量（部分供应商不返回则为 null）
     */
    private void captureUsage(dev.langchain4j.model.chat.response.ChatResponse response, Integer[] usage) {
        try {
            if (response != null && response.metadata() != null && response.metadata().tokenUsage() != null) {
                usage[0] = response.metadata().tokenUsage().inputTokenCount();
                usage[1] = response.metadata().tokenUsage().outputTokenCount();
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * 脱敏错误信息，避免内部细节（SQL/API key/栈信息）泄露给前端
     */
    private String sanitizeErrorMessage(Throwable error) {
        String msg = error.getMessage();
        if (msg == null) return "AI 服务异常，请稍后重试";
        if (msg.contains("api") || msg.contains("key") || msg.contains("auth")) return "AI 服务认证异常，请联系管理员";
        if (msg.contains("SQL") || msg.contains("JDBC")) return "数据查询异常，请稍后重试";
        if (msg.contains("timeout") || msg.contains("Timeout")) return "AI 回复超时，请稍后重试";
        return "AI 服务异常，请稍后重试";
    }
}
