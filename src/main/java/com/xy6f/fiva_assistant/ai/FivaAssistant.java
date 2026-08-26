package com.xy6f.fiva_assistant.ai;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;

/**
 * 带 Tool Calling 的流式 AI 助手。
 * AiServices 动态代理生成实现，工具调用循环由框架自动完成。
 */
public interface FivaAssistant {

    TokenStream chat(@MemoryId String sessionId, @UserMessage String message);
}
