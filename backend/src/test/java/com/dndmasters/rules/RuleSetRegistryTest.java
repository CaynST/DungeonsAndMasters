package com.dndmasters.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class RuleSetRegistryTest {

    private final RuleSetRegistry registry = new RuleSetRegistry();

    @Test
    void loadsBothEditions() {
        assertThat(registry.all()).containsExactlyInAnyOrder(Edition.DND_5E_2014, Edition.DND_5E_2024);
    }

    @Test
    void resolvesEditionFromVariousIds() {
        assertThat(Edition.fromId("dnd-5e-2024")).isEqualTo(Edition.DND_5E_2024);
        assertThat(Edition.fromId("2024")).isEqualTo(Edition.DND_5E_2024);
        assertThat(Edition.fromId("5e")).isEqualTo(Edition.DND_5E_2014);
        assertThat(Edition.fromId(null)).isEqualTo(Edition.DND_5E_2014);
    }

    @Test
    void twoThousandFourteenBonusesAreOnSpecies() {
        RuleSet set = registry.get(Edition.DND_5E_2014);
        RuleSet.Species dwarf = set.speciesOrThrow("dwarf");
        assertThat(dwarf.abilityBonuses()).containsEntry("constitution", 2);
        assertThat(dwarf.darkvision()).isTrue();
    }

    @Test
    void twoThousandTwentyFourMovesBonusesToBackground() {
        RuleSet set = registry.get(Edition.DND_5E_2024);
        RuleSet.Species human = set.speciesOrThrow("human");
        assertThat(human.abilityBonuses()).isEmpty();
        RuleSet.Background soldier = set.backgroundOrThrow("soldier");
        assertThat(soldier.abilityBonuses()).containsEntry("strength", 2);
    }

    @Test
    void twoThousandFourteenIncludesTheSixRemainingPlayerHandbookBackgrounds() {
        RuleSet set = registry.get(Edition.DND_5E_2014);

        assertThat(set.backgroundOrThrow("charlatan").name()).isEqualTo("Charlatán");
        assertThat(set.backgroundOrThrow("charlatan").skills()).containsExactly("deception", "sleight_of_hand");
        assertThat(set.backgroundOrThrow("entertainer").name()).isEqualTo("Artista");
        assertThat(set.backgroundOrThrow("entertainer").skills()).containsExactly("acrobatics", "performance");
        assertThat(set.backgroundOrThrow("guild-artisan").name()).isEqualTo("Artesano gremial");
        assertThat(set.backgroundOrThrow("guild-artisan").skills()).containsExactly("insight", "persuasion");
        assertThat(set.backgroundOrThrow("hermit").name()).isEqualTo("Ermitaño");
        assertThat(set.backgroundOrThrow("hermit").skills()).containsExactly("medicine", "religion");
        assertThat(set.backgroundOrThrow("outlander").name()).isEqualTo("Forastero");
        assertThat(set.backgroundOrThrow("outlander").skills()).containsExactly("athletics", "survival");
        assertThat(set.backgroundOrThrow("sailor").name()).isEqualTo("Marinero");
        assertThat(set.backgroundOrThrow("sailor").skills()).containsExactly("athletics", "perception");

        for (String id : List.of("charlatan", "entertainer", "guild-artisan", "hermit", "outlander", "sailor")) {
            RuleSet.Background background = set.backgroundOrThrow(id);
            assertThat(background.abilityBonuses()).as("2014 backgrounds do not grant ability bonuses").isEmpty();
            assertThat(background.description()).as("description of " + id).isNotBlank();
        }
    }

    @Test
    void normalizesIdsRegardlessOfFormat() {
        RuleSet set = registry.get(Edition.DND_5E_2014);
        assertThat(set.classOrThrow("Fighter").name()).isEqualTo("Guerrero");
        assertThat(set.backgroundOrThrow("folk_hero").name()).isEqualTo("Héroe Popular");
    }

    @Test
    void throwsOnUnknownRules() {
        RuleSet set = registry.get(Edition.DND_5E_2014);
        assertThatThrownBy(() -> set.classOrThrow("necromancer")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unknownEditionIdFallsBackToDefault() {
        // Lenient parsing: unknown ids resolve to 5e 2014 rather than failing the request.
        assertThat(registry.get("6e")).isSameAs(registry.get(Edition.DND_5E_2014));
    }
}
