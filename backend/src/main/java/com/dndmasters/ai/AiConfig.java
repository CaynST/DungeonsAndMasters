package com.dndmasters.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Selects the AI provider from environment configuration. Defaults to the offline mock. */
@Configuration
public class AiConfig {

    @Bean
    public AIProvider aiProvider(
            @Value("${app.ai.provider:mock}") String provider,
            @Value("${app.ai.api-key:}") String apiKey,
            @Value("${app.ai.model:gpt-4o-mini}") String model,
            @Value("${app.ai.base-url:https://api.openai.com/v1}") String baseUrl) {
        if ("openai-compatible".equalsIgnoreCase(provider) && apiKey != null && !apiKey.isBlank()) {
            return new OpenAiCompatibleProvider(baseUrl, apiKey, model);
        }
        return new MockAIProvider();
    }
}
