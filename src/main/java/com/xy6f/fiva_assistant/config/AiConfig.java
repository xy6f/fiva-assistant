package com.xy6f.fiva_assistant.config;

import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * AI 模型配置。
 * 文本对话模型与流式模型已迁移至 Python LangChain 服务（ai-agent），Java 侧仅保留 OCR 使用的视觉模型。
 */
@Configuration
public class AiConfig {

    @Value("${ai.deepseek.base-url}")
    private String baseUrl;

    @Value("${ai.deepseek.api-key}")
    private String apiKey;

    @Value("${ai.deepseek.model}")
    private String modelName;

    @Value("${ai.vision.base-url:}")
    private String visionBaseUrl;

    @Value("${ai.vision.api-key:}")
    private String visionApiKey;

    @Value("${ai.vision.model:}")
    private String visionModel;

    /**
     * Vision model for OCR (supports image input)
     * Fallback to deepseek config if vision config is not set
     */
    @Bean
    @Qualifier("visionModel")
    public OpenAiChatModel visionModel() {
        String vBaseUrl = (visionBaseUrl != null && !visionBaseUrl.isBlank()) ? visionBaseUrl : baseUrl;
        String vApiKey = (visionApiKey != null && !visionApiKey.isBlank()) ? visionApiKey : apiKey;
        String vModel = (visionModel != null && !visionModel.isBlank()) ? visionModel : modelName;
        return OpenAiChatModel.builder()
                .baseUrl(vBaseUrl)
                .apiKey(vApiKey)
                .modelName(vModel)
                .timeout(Duration.ofSeconds(180))
                .maxRetries(2)
                .logRequests(true)
                .logResponses(true)
                .build();
    }
}
