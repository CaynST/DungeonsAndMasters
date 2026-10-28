package com.dndmasters.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * A reasoning model may return {@code content: null}. Its {@code reasoning} field is private
 * chain-of-thought, not an answer: it must never be surfaced to the player. If content is
 * missing the provider fails so the caller can try another model or fall back.
 */
class ProviderNullContentTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    private String respondWith(String body) throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.start();
        server.createContext("/chat/completions", exchange -> {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });
        return "http://localhost:" + server.getAddress().getPort();
    }

    @Test
    void neverSurfacesChainOfThoughtWhenContentIsNull() throws Exception {
        String baseUrl = respondWith(
                "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":null,"
                        + "\"reasoning\":\"I should tell the player that the orc attacks.\"}}]}");
        OpenAiCompatibleProvider provider = new OpenAiCompatibleProvider(baseUrl, "k", "m", List.of());
        assertThatThrownBy(() -> provider.complete(new AiRequest("s", "u", Map.of(), 0.5, 100)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("All AI models failed");
    }

    @Test
    void neverSurfacesReasoningContentWhenContentIsNull() throws Exception {
        String baseUrl = respondWith(
                "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":null,"
                        + "\"reasoning_content\":\"The crypt breathes.\"}}]}");
        OpenAiCompatibleProvider provider = new OpenAiCompatibleProvider(baseUrl, "k", "m", List.of());
        assertThatThrownBy(() -> provider.complete(new AiRequest("s", "u", Map.of(), 0.5, 100)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void prefersContentWhenPresent() throws Exception {
        String baseUrl = respondWith(
                "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"texto final\","
                        + "\"reasoning\":\"pensamiento interno\"}}]}");
        OpenAiCompatibleProvider provider = new OpenAiCompatibleProvider(baseUrl, "k", "m", List.of());
        assertThat(provider.complete(new AiRequest("s", "u", Map.of(), 0.5, 100)).text())
                .isEqualTo("texto final");
    }
}
