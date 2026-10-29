package com.dndmasters.engine.combat;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ConditionTest {

    @Test
    void fromCodeIsCaseInsensitiveAndRejectsUnknown() {
        assertThat(Condition.fromCode("poisoned")).isEqualTo(Condition.POISONED);
        assertThat(Condition.fromCode(" PRONE ")).isEqualTo(Condition.PRONE);
        assertThat(Condition.fromCode("banana")).isNull();
        assertThat(Condition.fromCode(null)).isNull();
    }

    @Test
    void everyConditionHasLabelAndDescription() {
        for (Condition condition : Condition.values()) {
            assertThat(condition.label()).isNotBlank();
            assertThat(condition.description()).isNotBlank();
        }
    }

    @Test
    void incapacitatingConditionsBlockActions() {
        assertThat(Condition.PARALYZED.incapacitating()).isTrue();
        assertThat(Condition.STUNNED.incapacitating()).isTrue();
        assertThat(Condition.UNCONSCIOUS.incapacitating()).isTrue();
        assertThat(Condition.INCAPACITATED.incapacitating()).isTrue();
        assertThat(Condition.PRONE.incapacitating()).isFalse();
        assertThat(Condition.POISONED.incapacitating()).isFalse();
    }

    @Test
    void speedZeroConditionsStopMovement() {
        assertThat(Condition.GRAPPLED.speedZero()).isTrue();
        assertThat(Condition.RESTRAINED.speedZero()).isTrue();
        assertThat(Condition.PARALYZED.speedZero()).isTrue();
        assertThat(Condition.PRONE.speedZero()).isFalse();
    }

    @Test
    void onlyParalysisAndUnconsciousnessMakeMeleeHitsCritical() {
        assertThat(Condition.PARALYZED.meleeCritAgainst()).isTrue();
        assertThat(Condition.UNCONSCIOUS.meleeCritAgainst()).isTrue();
        assertThat(Condition.STUNNED.meleeCritAgainst()).isFalse();
        assertThat(Condition.PRONE.meleeCritAgainst()).isFalse();
    }
}
