package com.dndmasters.engine.combat;

import static org.assertj.core.api.Assertions.assertThat;

import com.dndmasters.domain.GameEntity;
import org.junit.jupiter.api.Test;

class SurpriseRulesTest {

    @Test
    void aCreatureWithoutTheFlagIsNotSurprised() {
        GameEntity entity = new GameEntity();

        assertThat(SurpriseRules.isSurprised(entity)).isFalse();
        assertThat(SurpriseRules.consumeSurprised(entity)).isFalse();
    }

    @Test
    void markingACreatureSurprisedMakesItLoseExactlyOneTurn() {
        GameEntity entity = new GameEntity();
        SurpriseRules.markSurprised(entity);

        assertThat(SurpriseRules.isSurprised(entity)).isTrue();
        assertThat(SurpriseRules.consumeSurprised(entity)).isTrue();
        assertThat(SurpriseRules.isSurprised(entity)).isFalse();
        assertThat(SurpriseRules.consumeSurprised(entity)).isFalse();
    }
}
