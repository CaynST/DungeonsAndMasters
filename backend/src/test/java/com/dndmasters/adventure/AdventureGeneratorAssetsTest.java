package com.dndmasters.adventure;

import static org.assertj.core.api.Assertions.assertThat;

import com.dndmasters.content.AdventureDefinition;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Materialised content must point at assets that actually exist. */
class AdventureGeneratorAssetsTest {

    @Test
    void generatedNpcPortraitsResolveToBundledAssets() {
        AdventureGenerator generator = new AdventureGenerator(null);
        AdventurePlan plan = new AdventurePlan(
                "Test", "Setting", "Description", "Objective", "Villain",
                List.of("Once upon a time"),
                List.of(new AdventurePlan.ScenePlan("Scene", "Description", "SOCIAL", "Objective", "crypt", List.of())));

        AdventureDefinition definition = generator.materialize(plan, new Random(1));

        for (AdventureDefinition.NpcDefinition npc : definition.npcs()) {
            String resource = "static" + npc.portrait();
            assertThat(getClass().getClassLoader().getResource(resource))
                    .as(npc.portrait())
                    .isNotNull();
        }
    }
}
