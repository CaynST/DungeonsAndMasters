package com.dndmasters.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** The provider must tolerate reasoning models that return null content. */
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
    void fallsBackToReasoningContentWhenContentIsNull() throws Exception {
        String baseUrl = respondWith(
                "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":null,"
                        + "\"reasoning\":\"El orco gruñe y alza su hacha.\"}}]}");
        OpenAiCompatibleProvider provider = new OpenAiCompatibleProvider(baseUrl, "k", "m", List.of());
        AiResponse response = provider.complete(new AiRequest("s", "u", Map.of(), 0.5, 100));
        assertThat(response.text()).contains("orco").contains("hacha").doesNotContain("null");
    }

    @Test
    void usesReasoningContentFieldWhenProvided() throws Exception {
        String baseUrl = respondWith(
                "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":null,"
                        + "\"reasoning_content\":\"La cripta respira.\"}}]}");
        OpenAiCompatibleProvider provider = new OpenAiCompatibleProvider(baseUrl, "k", "m", List.of());
        AiResponse response = provider.complete(new AiRequest("s", "u", Map.of(), 0.5, 100));
        assertThat(response.text()).isEqualTo("La cripta respira.");
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
