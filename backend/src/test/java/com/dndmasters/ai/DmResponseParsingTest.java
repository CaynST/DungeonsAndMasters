package com.dndmasters.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.dndmasters.ai.dm.DmActionValidator;
import com.dndmasters.ai.dm.DmContext;
import com.dndmasters.ai.dm.DmResponse;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

/**
 * Parsing must survive the untidy output real LLMs produce: markdown fences, prose around
 * the JSON, and — critically — output truncated at max_tokens, which yields unbalanced JSON.
 */
class DmResponseParsingTest {

    /** Lightweight stub so tests can express provider behaviour inline. */
    private record StubProvider(Function<AiRequest, AiResponse> fn) implements AIProvider {
        @Override
        public String id() {
            return "stub";
        }

        @Override
        public AiResponse complete(AiRequest request) {
            return fn.apply(request);
        }
    }

    private DungeonMasterService service(AIProvider provider) {
        return new DungeonMasterService(provider, new DmActionValidator());
    }

    private DmContext context() {
        DmContext context = new DmContext();
        context.setTrigger("DIALOGUE");
        context.setCampaignName("Test");
        context.setPhase("EXPLORATION");
        return context;
    }

    @Test
    void parsesPlainJson() {
        AIProvider provider = new StubProvider(req -> new AiResponse(
                "{\"narration\":\"Hola\",\"actions\":[],\"sceneChanged\":false,\"imagePrompt\":null}"));
        assertThat(service(provider).respond(context()).narration()).isEqualTo("Hola");
    }

    @Test
    void parsesJsonInsideMarkdownFence() {
        String fenced = "Aquí tienes:\n```json\n"
                + "{\"narration\":\"Con fenced\",\"actions\":[],\"sceneChanged\":false,\"imagePrompt\":null}\n```";
        AIProvider provider = new StubProvider(req -> new AiResponse(fenced));
        assertThat(service(provider).respond(context()).narration()).isEqualTo("Con fenced");
    }

    @Test
    void recoversNarrationFromTruncatedJson() {
        // Simulates finish_reason=length: the string starts but never closes.
        String truncated = "{\"narration\":\"El Archivista baja la voz y señala la puerta. «El relicario permanece sellado";
        AIProvider provider = new StubProvider(req -> new AiResponse(truncated));
        String narration = service(provider).respond(context()).narration();
        assertThat(narration).isNotBlank();
        assertThat(narration).contains("relicario");
        // Must be the clean narration value, not the raw JSON scaffolding.
        assertThat(narration).doesNotContain("{\"narration\"");
        assertThat(narration).doesNotContain("\"actions\"");
    }

    @Test
    void fallsBackToOfflineScriptWhenProviderFails() {
        AIProvider provider = new StubProvider(req -> {
            throw new IllegalStateException("boom");
        });
        DmResponse response = service(provider).respond(context());
        assertThat(response.narration()).isNotBlank();
    }

    @Test
    void promptsTheModelToStayBounded() {
        // The request carries a bounded max_tokens so the DM doesn't overrun.
        final int[] seen = {0};
        AIProvider provider = new StubProvider(req -> {
            seen[0] = req.maxTokens();
            return new AiResponse("{\"narration\":\"ok\",\"actions\":[]}");
        });
        service(provider).respond(context());
        assertThat(seen[0]).isBetween(200, 2000);
    }
}
