package com.dndmasters.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Verifies provider selection from configuration, including the safe mock fallback. */
class AiConfigTest {

    private final AiConfig config = new AiConfig();

    @Test
    void defaultsToMockWhenNoKey() {
        AIProvider provider = config.aiProvider("opencode-go", "", "space-bunny-free", "", "", "ua");
        assertThat(provider).isInstanceOf(MockAIProvider.class);
    }

    @Test
    void selectsOpenCodeGoWithDefaultBaseUrl() {
        AIProvider provider = config.aiProvider("opencode-go", "sk-test", "space-bunny-free",
                "", "deepseek-v4-flash,mimo-v2.5", "ua");
        assertThat(provider).isInstanceOf(OpenAiCompatibleProvider.class);
        OpenAiCompatibleProvider compatible = (OpenAiCompatibleProvider) provider;
        assertThat(compatible.models()).containsExactly("space-bunny-free", "deepseek-v4-flash", "mimo-v2.5");
    }

    @Test
    void unknownProviderFallsBackToMock() {
        AIProvider provider = config.aiProvider("something-else", "sk-test", "model", "", "", "ua");
        assertThat(provider).isInstanceOf(MockAIProvider.class);
    }

    @Test
    void parsesFallbackListTrimmed() {
        AIProvider provider = config.aiProvider("openai-compatible", "sk", "primary",
                "https://example.test/v1", "a, b ,c", "ua");
        assertThat(((OpenAiCompatibleProvider) provider).models())
                .containsExactly("primary", "a", " b ", "c");
    }
}
