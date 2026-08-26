package com.xy6f.fiva_assistant.config;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

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
     * Text chat model (DeepSeek)
     */
    @Bean
    @Qualifier("chatModel")
    public OpenAiChatModel chatModel() {
        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .timeout(Duration.ofSeconds(120))
                .maxRetries(2)
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    /**
     * Streaming chat model for SSE output
     */
    @Bean
    @Qualifier("streamingChatModel")
    public OpenAiStreamingChatModel streamingChatModel() {
        return OpenAiStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .timeout(Duration.ofSeconds(120))
                .build();
    }

    /**
     * Vision model for OCR (supports image input)
     * Fallback to chatModel if vision config is not set
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
