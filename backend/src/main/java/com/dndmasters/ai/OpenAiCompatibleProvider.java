package com.dndmasters.ai;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import com.dndmasters.util.Json;

/** Calls an OpenAI-compatible chat completions endpoint. Configured entirely via env vars. */
public class OpenAiCompatibleProvider implements AIProvider {

    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    private final String baseUrl;
    private final String apiKey;
    private final String model;

    public OpenAiCompatibleProvider(String baseUrl, String apiKey, String model) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public String id() {
        return "openai-compatible";
    }

    @Override
    public AiResponse complete(AiRequest request) {
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
            throw new IllegalStateException("AI completion failed", e);
        }
    }
}
