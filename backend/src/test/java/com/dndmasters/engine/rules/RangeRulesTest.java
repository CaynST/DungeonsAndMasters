package com.dndmasters.engine.rules;

import static org.assertj.core.api.Assertions.assertThat;

import com.dndmasters.domain.Attack;
import org.junit.jupiter.api.Test;

class RangeRulesTest {

    private final Attack melee = Attack.melee("Espada larga", 5, "1d8+3");
    private final Attack bow = Attack.ranged("Arco corto", 4, "1d6+2", 16);

    @Test
    void meleeReachesOnlyAdjacentSquares() {
        assertThat(RangeRules.check(melee, 1).allowed()).isTrue();
        assertThat(RangeRules.check(melee, 1).disadvantage()).isFalse();

        RangeRules.Result far = RangeRules.check(melee, 2);
        assertThat(far.allowed()).isFalse();
        assertThat(far.reason()).contains("cuerpo a cuerpo");
    }

    @Test
    void rangedWithinNormalRangeHasNoDisadvantage() {
        RangeRules.Result result = RangeRules.check(bow, 10);
        assertThat(result.allowed()).isTrue();
        assertThat(result.disadvantage()).isFalse();
    }

    @Test
    void rangedWithinLongRangeHasDisadvantage() {
        RangeRules.Result result = RangeRules.check(bow, 20); // 16 normal, 32 long
        assertThat(result.allowed()).isTrue();
        assertThat(result.disadvantage()).isTrue();
    }

    @Test
    void rangedBeyondLongRangeIsRejected() {
        RangeRules.Result result = RangeRules.check(bow, 40);
        assertThat(result.allowed()).isFalse();
        assertThat(result.reason()).contains("fuera de alcance");
    }

    @Test
    void adjacentRangedAttackIsAllowed() {
        assertThat(RangeRules.check(bow, 1).allowed()).isTrue();
    }
}
