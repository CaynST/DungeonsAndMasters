package com.dndmasters.ai;

import java.util.Map;

/** Request to a language model. {@code context} is structured data a mock can reason about. */
public record AiRequest(
        String system,
        String user,
        Map<String, Object> context,
        double temperature,
        int maxTokens) {
}
