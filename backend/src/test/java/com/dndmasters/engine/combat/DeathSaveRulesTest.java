package com.dndmasters.engine.combat;

import static org.assertj.core.api.Assertions.assertThat;

import com.dndmasters.engine.combat.DeathSaveRules.State;
import org.junit.jupiter.api.Test;

class DeathSaveRulesTest {

    @Test
    void naturalTwentyRestoresOneHitPoint() {
        State result = DeathSaveRules.roll(State.startDying(), 20);
        assertThat(result.hpAfter()).isEqualTo(1);
        assertThat(result.dying()).isFalse();
        assertThat(result.stable()).isFalse();
        assertThat(result.dead()).isFalse();
    }

    @Test
    void naturalOneCountsAsTwoFailures() {
        State result = DeathSaveRules.roll(State.startDying(), 1);
        assertThat(result.failures()).isEqualTo(2);
        assertThat(result.dying()).isTrue();
        assertThat(result.dead()).isFalse();
    }

    @Test
    void tenOrMoreIsASuccessBelowTenAFailure() {
        assertThat(DeathSaveRules.roll(State.startDying(), 10).successes()).isEqualTo(1);
        assertThat(DeathSaveRules.roll(State.startDying(), 9).failures()).isEqualTo(1);
    }

    @Test
    void threeSuccessesStabilise() {
        State state = new State(true, false, 2, 0, false, 0);
        State result = DeathSaveRules.roll(state, 15);
        assertThat(result.stable()).isTrue();
        assertThat(result.dying()).isFalse();
        assertThat(result.dead()).isFalse();
    }

    @Test
    void threeFailuresKill() {
        State state = new State(true, false, 0, 2, false, 0);
        State result = DeathSaveRules.roll(state, 5);
        assertThat(result.dead()).isTrue();
    }

    @Test
    void damageWhileDyingAddsAFailure() {
        State result = DeathSaveRules.onDamage(State.startDying(), 5, 20, false);
        assertThat(result.failures()).isEqualTo(1);
        assertThat(result.dead()).isFalse();
    }

    @Test
    void criticalDamageWhileDyingAddsTwoFailures() {
        State result = DeathSaveRules.onDamage(State.startDying(), 5, 20, true);
        assertThat(result.failures()).isEqualTo(2);
    }

    @Test
    void damageEqualToMaxHpKillsInstantly() {
        State result = DeathSaveRules.onDamage(State.startDying(), 20, 20, false);
        assertThat(result.dead()).isTrue();
    }
}
