package com.dndmasters.ai;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Selects the AI provider from environment configuration.
 *
 * <p>Providers:
 * <ul>
 *   <li>{@code mock} (default) — offline scripted DM, no network.</li>
 *   <li>{@code opencode-go} — OpenCode Go gateway ({@code https://opencode.ai/zen/go/v1}).
 *       Default model {@code space-bunny-free}; falls back through cheap models.</li>
 *   <li>{@code openai-compatible} — any OpenAI-compatible endpoint via {@code AI_BASE_URL}.</li>
 * </ul>
 */
@Configuration
public class AiConfig {

    private static final String GO_BASE_URL = "https://opencode.ai/zen/go/v1";
    private static final String ZEN_BASE_URL = "https://opencode.ai/zen/v1";

    @Bean
    public AIProvider aiProvider(
            @Value("${app.ai.provider:mock}") String provider,
            @Value("${app.ai.api-key:}") String apiKey,
            @Value("${app.ai.model:space-bunny-free}") String model,
            @Value("${app.ai.base-url:}") String configuredBaseUrl,
            @Value("${app.ai.fallback-models:}") String fallbackModels,
            @Value("${app.ai.user-agent:dungeons-and-masters/1.0}") String userAgent) {

        if (apiKey == null || apiKey.isBlank()) {
            // Never break the game: without a key we use the offline DM.
            return new MockAIProvider();
        }

        List<String> fallbacks = (fallbackModels == null || fallbackModels.isBlank())
                ? List.of()
                : List.of(fallbackModels.split(","));

        String baseUrl = switch (provider == null ? "mock" : provider.toLowerCase()) {
            case "opencode-go", "go" -> configuredBaseUrl == null || configuredBaseUrl.isBlank()
                    ? GO_BASE_URL : configuredBaseUrl;
            case "opencode-zen", "zen" -> configuredBaseUrl == null || configuredBaseUrl.isBlank()
                    ? ZEN_BASE_URL : configuredBaseUrl;
            case "openai-compatible" -> configuredBaseUrl;
            default -> null;
        };

        if (baseUrl == null || baseUrl.isBlank()) {
            return new MockAIProvider();
        }
        return new OpenAiCompatibleProvider(baseUrl, apiKey.trim(), model.trim(), fallbacks, userAgent);
    }
}
