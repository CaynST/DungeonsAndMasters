package com.dndmasters.ai;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.dndmasters.util.Json;

/**
 * Calls an OpenAI-compatible chat completions endpoint (OpenAI, OpenCode Zen/Go, etc).
 * Configured entirely via env vars. Supports a fallback chain of models so a rate-limited
 * or unavailable model degrades to the next cheapest one instead of failing the turn.
 */
public class OpenAiCompatibleProvider implements AIProvider {

    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    private final String baseUrl;
    private final String apiKey;
    private final List<String> models;
    private final String userAgent;
    private final String sessionId = UUID.randomUUID().toString();

    public OpenAiCompatibleProvider(String baseUrl, String apiKey, String model, List<String> fallbackModels) {
        this(baseUrl, apiKey, model, fallbackModels, "dungeons-and-masters/1.0");
    }

    public OpenAiCompatibleProvider(String baseUrl, String apiKey, String model, List<String> fallbackModels,
                                    String userAgent) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.apiKey = apiKey;
        this.userAgent = userAgent;
        List<String> all = new ArrayList<>();
        if (model != null && !model.isBlank()) {
            all.add(model);
        }
        if (fallbackModels != null) {
            fallbackModels.stream().filter(m -> m != null && !m.isBlank() && !all.contains(m)).forEach(all::add);
        }
        this.models = all.isEmpty() ? List.of("gpt-4o-mini") : List.copyOf(all);
    }

    @Override
    public String id() {
        return "openai-compatible";
    }

    public List<String> models() {
        return models;
    }

    @Override
    public AiResponse complete(AiRequest request) {
        RuntimeException lastError = null;
        for (String model : models) {
            try {
                return callModel(model, request);
            } catch (RuntimeException e) {
                lastError = e;
                // Try the next model in the fallback chain.
            }
        }
        throw new IllegalStateException("All AI models failed: " + models, lastError);
    }

    private AiResponse callModel(String model, AiRequest request) {
        Map<String, Object> body = Map.of(
                "model", model,
                "temperature", request.temperature(),
                "max_tokens", request.maxTokens(),
                "messages", List.of(
                        Map.of("role", "system", "content", request.system()),
                        Map.of("role", "user", "content", request.user())));

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/chat/completions"))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .header("User-Agent", userAgent)
                .header("x-opencode-session", sessionId)
                .POST(HttpRequest.BodyPublishers.ofString(Json.write(body)))
                .build();
        try {
            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                throw new IllegalStateException("AI provider error " + response.statusCode() + ": " + response.body());
            }
            Map<String, Object> parsed = Json.readMap(response.body());
            List<Map<String, Object>> choices = (List<Map<String, Object>>) parsed.get("choices");
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            return new AiResponse(String.valueOf(message.get("content")));
        } catch (Exception e) {
            throw new IllegalStateException("AI completion failed for model " + model + ": " + e.getMessage(), e);
        }
    }
}
