package com.dndmasters.ai;

/**
 * Provider-agnostic text completion. Implementations: {@link MockAIProvider} (default,
 * offline) and {@link OpenAiCompatibleProvider} (any OpenAI-compatible endpoint).
 */
public interface AIProvider {

    String id();

    AiResponse complete(AiRequest request);
}
