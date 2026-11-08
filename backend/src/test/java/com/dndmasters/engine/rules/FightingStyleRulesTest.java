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
    void greatWeaponFightingOnlyRerollsTwoHandedMeleeWeaponDice() {
        assertThat(FightingStyleRules.rerollsLowDamageDice("great-weapon", true, true)).isTrue();
        assertThat(FightingStyleRules.rerollsLowDamageDice("Great-Weapon", true, true)).isTrue();
        // One-handed melee, ranged attacks and other styles keep their damage dice.
        assertThat(FightingStyleRules.rerollsLowDamageDice("great-weapon", true, false)).isFalse();
        assertThat(FightingStyleRules.rerollsLowDamageDice("great-weapon", false, true)).isFalse();
        assertThat(FightingStyleRules.rerollsLowDamageDice("dueling", true, true)).isFalse();
    }

    @Test
    void protectionIsRecognisedAsAReactiveStyle() {
        assertThat(FightingStyleRules.hasProtection("protection")).isTrue();
        assertThat(FightingStyleRules.hasProtection("Protection")).isTrue();
        assertThat(FightingStyleRules.hasProtection("defense")).isFalse();
        assertThat(FightingStyleRules.hasProtection(null)).isFalse();
    }

    @Test
    void onlySupportedStylesAreKnown() {
        assertThat(FightingStyleRules.isKnown("defense")).isTrue();
        assertThat(FightingStyleRules.isKnown("Archery")).isTrue();
        assertThat(FightingStyleRules.isKnown("dueling")).isTrue();
        assertThat(FightingStyleRules.isKnown("great-weapon")).isTrue();
        assertThat(FightingStyleRules.isKnown("protection")).isTrue();
        assertThat(FightingStyleRules.isKnown("tunnel-fighter")).isFalse();
        assertThat(FightingStyleRules.isKnown(null)).isFalse();
        assertThat(FightingStyleRules.label("dueling")).isEqualTo("Duelo");
        assertThat(FightingStyleRules.label("great-weapon")).isEqualTo("Gran arma");
        assertThat(FightingStyleRules.label("protection")).isEqualTo("Protección");
    }
}
