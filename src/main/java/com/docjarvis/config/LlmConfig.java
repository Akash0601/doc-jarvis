package com.docjarvis.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;

@Configuration
public class LlmConfig {

    // ─── LOCAL PROFILE — uses Ollama ───────────────────────────────────────

    @Bean
    @Profile("local")
    public ChatLanguageModel ollamaChatModel(
            @Value("${llm.ollama.base-url}") String baseUrl,
            @Value("${llm.ollama.model-name}") String modelName) {

        return OllamaChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName)
                .timeout(Duration.ofMinutes(3))
                .build();
    }

    // ─── PROD PROFILE — uses OpenAI (Stage 6) ──────────────────────────────
    // Uncomment this in Stage 6 and add langchain4j-open-ai dependency
    //
    // @Bean
    // @Profile("prod")
    // public ChatLanguageModel openAiChatModel(
    //         @Value("${llm.openai.api-key}") String apiKey,
    //         @Value("${llm.openai.model-name}") String modelName) {
    //
    //     return OpenAiChatModel.builder()
    //             .apiKey(apiKey)
    //             .modelName(modelName)
    //             .build();
    // }
}