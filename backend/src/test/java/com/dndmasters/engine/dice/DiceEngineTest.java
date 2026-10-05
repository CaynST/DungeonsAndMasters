package com.dndmasters.engine.dice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dndmasters.engine.dice.DiceParseException;
import com.dndmasters.testsupport.StubRandom;
import java.util.Random;
import org.junit.jupiter.api.Test;

class DiceEngineTest {

    static Random stubRandom(int... zeroBasedValues) {
        return StubRandom.of(zeroBasedValues);
    }

    @Test
    void rollsSingleD20() {
        DiceEngine engine = new DiceEngine(stubRandom(19));
        DiceRollResult result = engine.roll("1d20");
        assertThat(result.rolls()).containsExactly(20);
        assertThat(result.total()).isEqualTo(20);
        assertThat(result.natural20()).isTrue();
        assertThat(result.natural1()).isFalse();
    }

    @Test
    void flagsNaturalOne() {
        DiceEngine engine = new DiceEngine(stubRandom(0));
        DiceRollResult result = engine.roll("1d20");
        assertThat(result.rolls()).containsExactly(1);
        assertThat(result.natural1()).isTrue();
    }

    @Test
    void appliesPositiveModifier() {
        DiceEngine engine = new DiceEngine(stubRandom(14));
        DiceRollResult result = engine.roll("1d20 + 5");
        assertThat(result.rolls()).containsExactly(15);
        assertThat(result.modifier()).isEqualTo(5);
        assertThat(result.total()).isEqualTo(20);
        assertThat(result.expression()).isEqualTo("1d20+5");
    }

    @Test
    void rollsMultipleDiceWithModifier() {
        DiceEngine engine = new DiceEngine(stubRandom(0, 5));
        DiceRollResult result = engine.roll("2d6 + 3");
        assertThat(result.rolls()).containsExactly(1, 6);
        assertThat(result.total()).isEqualTo(10);
    }

    @Test
    void supportsNegativeModifier() {
        DiceEngine engine = new DiceEngine(stubRandom(4));
        DiceRollResult result = engine.roll("1d10-2");
        assertThat(result.rolls()).containsExactly(5);
        assertThat(result.total()).isEqualTo(3);
    }

    @Test
    void supportsSingleSidedDie() {
        DiceEngine engine = new DiceEngine(stubRandom(0));
        DiceRollResult result = engine.roll("1d1");
        assertThat(result.rolls()).containsExactly(1);
        assertThat(result.total()).isEqualTo(1);
    }

    @Test
    void treatsBareConstantAsValue() {
        DiceEngine engine = new DiceEngine();
        DiceRollResult result = engine.roll("7");
        assertThat(result.total()).isEqualTo(7);
        assertThat(result.rolls()).containsExactly(7);
    }

    @Test
    void rollsWithinRangeForRealRandom() {
        DiceEngine engine = new DiceEngine(new Random(42));
        for (int i = 0; i < 500; i++) {
            int value = engine.roll("1d20").rolls().get(0);
            assertThat(value).isBetween(1, 20);
        }
    }

    @Test
    void rejectsMalformedExpressions() {
        DiceEngine engine = new DiceEngine();
        assertThatThrownBy(() -> engine.roll("2d")).isInstanceOf(DiceParseException.class);
        assertThatThrownBy(() -> engine.roll("1d20+")).isInstanceOf(DiceParseException.class);
        assertThatThrownBy(() -> engine.roll("abc")).isInstanceOf(DiceParseException.class);
        assertThatThrownBy(() -> engine.roll("")).isInstanceOf(DiceParseException.class);
        assertThatThrownBy(() -> engine.roll(null)).isInstanceOf(DiceParseException.class);
    }
}
