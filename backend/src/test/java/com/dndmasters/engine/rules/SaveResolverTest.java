package com.dndmasters.engine.rules;

import static org.assertj.core.api.Assertions.assertThat;

import com.dndmasters.domain.Ability;
import com.dndmasters.domain.Stats;
import com.dndmasters.engine.dice.DiceEngine;
import com.dndmasters.testsupport.StubRandom;
import org.junit.jupiter.api.Test;

class SaveResolverTest {

    private static Combatant combatant(int dexterity) {
        return new Combatant(1L, "Target", "ENEMY", 20, 20, 12,
                new Stats(10, dexterity, 10, 10, 10, 10), true);
    }

    @Test
    void succeedsWhenTotalMeetsTheDc() {
        SaveResolver resolver = new SaveResolver(new DiceEngine(StubRandom.of(11))); // d20 = 12
        Combatant target = combatant(14); // DEX +2

        SaveResult success = resolver.resolve(target, Ability.DEXTERITY, 14);
        SaveResult failure = resolver.resolve(target, Ability.DEXTERITY, 15);

        assertThat(success.total()).isEqualTo(14);
        assertThat(success.success()).isTrue();
        assertThat(failure.success()).isFalse();
    }

    @Test
    void advantageTakesTheHigherDie() {
        SaveResolver resolver = new SaveResolver(new DiceEngine(StubRandom.of(3, 17))); // 4, 18
        Combatant target = combatant(10); // DEX +0

        SaveResult result = resolver.resolve(target, Ability.DEXTERITY, 15, Advantage.ADVANTAGE);

        assertThat(result.dieRoll()).isEqualTo(18);
        assertThat(result.total()).isEqualTo(18);
        assertThat(result.success()).isTrue();
    }

    @Test
    void disadvantageTakesTheLowerDie() {
        SaveResolver resolver = new SaveResolver(new DiceEngine(StubRandom.of(17, 3))); // 18, 4
        Combatant target = combatant(10);

        SaveResult result = resolver.resolve(target, Ability.DEXTERITY, 15, Advantage.DISADVANTAGE);

        assertThat(result.dieRoll()).isEqualTo(4);
        assertThat(result.success()).isFalse();
    }

    @Test
    void flatBonusIsAddedToTheTotal() {
        SaveResolver resolver = new SaveResolver(new DiceEngine(StubRandom.of(11))); // 12
        Combatant target = combatant(14); // DEX +2

        SaveResult result = resolver.resolve(target, Ability.DEXTERITY, 15, Advantage.NONE, 3);

        assertThat(result.total()).isEqualTo(17);
        assertThat(result.success()).isTrue();
    }
}
