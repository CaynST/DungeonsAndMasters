package com.dndmasters.engine.rules;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FightingStyleRulesTest {

    @Test
    void defenseAddsOneAcOnlyWhileWearingArmor() {
        assertThat(FightingStyleRules.defenseBonus("defense", true)).isEqualTo(1);
        assertThat(FightingStyleRules.defenseBonus("defense", false)).isZero();
        assertThat(FightingStyleRules.defenseBonus("archery", true)).isZero();
    }

    @Test
    void archeryAddsTwoToRangedAttacks() {
        assertThat(FightingStyleRules.archeryBonus("archery", true)).isEqualTo(2);
        assertThat(FightingStyleRules.archeryBonus("archery", false)).isZero();
        assertThat(FightingStyleRules.archeryBonus("defense", true)).isZero();
    }

    @Test
    void duelingAddsTwoToOneHandedMeleeAttacks() {
        assertThat(FightingStyleRules.duelingBonus("dueling", true, false)).isEqualTo(2);
        assertThat(FightingStyleRules.duelingBonus("dueling", true, true)).isZero();
        assertThat(FightingStyleRules.duelingBonus("dueling", false, false)).isZero();
    }

    @Test
    void onlySupportedStylesAreKnown() {
        assertThat(FightingStyleRules.isKnown("defense")).isTrue();
        assertThat(FightingStyleRules.isKnown("Archery")).isTrue();
        assertThat(FightingStyleRules.isKnown("dueling")).isTrue();
        assertThat(FightingStyleRules.isKnown("great-weapon")).isFalse();
        assertThat(FightingStyleRules.isKnown(null)).isFalse();
        assertThat(FightingStyleRules.label("dueling")).isEqualTo("Duelo");
    }
}
