package com.dndmasters.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.dndmasters.ai.dm.DmAction;
import com.dndmasters.ai.dm.DmResponse;
import com.dndmasters.util.Json;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MockAIProviderTest {

    @Test
    void failedCheckAppliesBlindedConditionToTheSpotlightCharacter() {
        MockAIProvider provider = new MockAIProvider();
        AiRequest request = new AiRequest("system", "prompt", Map.of(
                "trigger", "CHECK_RESULT",
                "sceneOrder", 0,
                "skill", "perception",
                "checkSuccess", false,
                "characters", List.of(Map.of("id", 42L, "name", "Heroína"))), 0.8, 700);

        DmResponse response = Json.read(provider.complete(request).text(), DmResponse.class);

        assertThat(response.actions()).anyMatch(action ->
                "APPLY_CONDITION".equals(action.type())
                        && "BLINDED".equals(action.condition())
                        && Long.valueOf(42L).equals(action.entityId()));
    }

    @Test
    void successfulCheckDoesNotApplyConditions() {
        MockAIProvider provider = new MockAIProvider();
        AiRequest request = new AiRequest("system", "prompt", Map.of(
                "trigger", "CHECK_RESULT",
                "sceneOrder", 0,
                "skill", "investigation",
                "checkSuccess", true,
                "characters", List.of(Map.of("id", 42L, "name", "Heroína"))), 0.8, 700);

        DmResponse response = Json.read(provider.complete(request).text(), DmResponse.class);

        assertThat(response.actions()).noneMatch(action -> action.type().contains("CONDITION"));
    }
}
