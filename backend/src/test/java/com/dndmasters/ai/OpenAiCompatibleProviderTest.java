package com.dndmasters.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Verifies the OpenAI-compatible provider walks its model fallback chain on failure. */
class OpenAiCompatibleProviderTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    private String startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.start();
        return "http://localhost:" + server.getAddress().getPort();
    }

    @Test
    void usesFirstModelWhenItSucceeds() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        String baseUrl = startServer();
        server.createContext("/chat/completions", exchange -> {
            calls.incrementAndGet();
            String body = "{\"choices\":[{\"message\":{\"content\":\"hola\"}}]}";
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });

        OpenAiCompatibleProvider provider = new OpenAiCompatibleProvider(
                baseUrl, "test-key", "space-bunny-free", List.of("deepseek-v4-flash"));
        AiResponse response = provider.complete(new AiRequest("sys", "user", java.util.Map.of(), 0.5, 100));

        assertThat(response.text()).isEqualTo("hola");
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void fallsBackToNextModelWhenFirstFails() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        String baseUrl = startServer();
        server.createContext("/chat/completions", exchange -> {
            int call = calls.incrementAndGet();
            if (call == 1) {
                // First model fails (e.g. rate limit).
                exchange.sendResponseHeaders(429, -1);
                exchange.close();
                return;
            }
            String body = "{\"choices\":[{\"message\":{\"content\":\"fallback ok\"}}]}";
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });

        OpenAiCompatibleProvider provider = new OpenAiCompatibleProvider(
                baseUrl, "test-key", "space-bunny-free", List.of("deepseek-v4-flash", "mimo-v2.5"));
        AiResponse response = provider.complete(new AiRequest("sys", "user", java.util.Map.of(), 0.5, 100));

        assertThat(response.text()).isEqualTo("fallback ok");
        assertThat(calls.get()).isEqualTo(2);
    }

    @Test
    void exposesModelChainWithPrimaryFirst() {
        OpenAiCompatibleProvider provider = new OpenAiCompatibleProvider(
                "https://example.test/v1", "key", "space-bunny-free",
                List.of("deepseek-v4-flash", "space-bunny-free"));
        // Duplicates are removed and the primary stays first.
        assertThat(provider.models()).containsExactly("space-bunny-free", "deepseek-v4-flash");
    }

    @Test
    void throwsWhenAllModelsFail() throws Exception {
        String baseUrl = startServer();
        server.createContext("/chat/completions", exchange -> {
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
        });
        OpenAiCompatibleProvider provider = new OpenAiCompatibleProvider(
                baseUrl, "key", "a", List.of("b"));
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> provider.complete(new AiRequest("s", "u", java.util.Map.of(), 0.5, 10)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("All AI models failed");
    }
}
