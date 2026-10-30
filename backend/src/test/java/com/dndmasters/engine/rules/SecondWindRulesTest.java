package com.dndmasters.engine.rules;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SecondWindRulesTest {

    @Test
    void healsOneD10PlusFighterLevel() {
        assertThat(SecondWindRules.healAmount(5, 1)).isEqualTo(6);
        assertThat(SecondWindRules.healAmount(10, 3)).isEqualTo(13);
    }

    @Test
    void hasOneUseAtLevelOne() {
        assertThat(SecondWindRules.maxUses(1)).isEqualTo(1);
        assertThat(SecondWindRules.maxUses(5)).isEqualTo(1);
    }
}
