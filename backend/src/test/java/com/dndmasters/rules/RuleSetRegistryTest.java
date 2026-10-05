package com.dndmasters.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
