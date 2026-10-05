package com.dndmasters.engine.character;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dndmasters.domain.Ability;
import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PointBuyTest {

    private static Map<Ability, Integer> scores(int str, int dex, int con, int intel, int wis, int cha) {
        Map<Ability, Integer> map = new EnumMap<>(Ability.class);
        map.put(Ability.STRENGTH, str);
        map.put(Ability.DEXTERITY, dex);
        map.put(Ability.CONSTITUTION, con);
        map.put(Ability.INTELLIGENCE, intel);
        map.put(Ability.WISDOM, wis);
        map.put(Ability.CHARISMA, cha);
        return map;
    }

    @Test
    void costTableMatchesSrd() {
        assertThat(PointBuy.costOf(8)).isZero();
        assertThat(PointBuy.costOf(13)).isEqualTo(5);
        assertThat(PointBuy.costOf(14)).isEqualTo(7);
        assertThat(PointBuy.costOf(15)).isEqualTo(9);
    }

    @Test
    void rejectsScoresOutsideRange() {
        assertThatThrownBy(() -> PointBuy.costOf(7)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PointBuy.costOf(16)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void standardArrayCostsExactlyTwentySeven() {
        // 15,15,14,10,8,8 → 9+9+7+2+0+0 = 27
        Map<Ability, Integer> scores = scores(15, 15, 14, 10, 8, 8);
        assertThat(PointBuy.totalCost(scores)).isEqualTo(27);
        assertThat(PointBuy.remaining(scores)).isZero();
        assertThat(PointBuy.isValid(scores)).isTrue();
    }

    @Test
    void rejectsOverBudgetBuild() {
        // all 15s = 54
        Map<Ability, Integer> scores = scores(15, 15, 15, 15, 15, 15);
        assertThat(PointBuy.totalCost(scores)).isEqualTo(54);
        assertThat(PointBuy.isValid(scores)).isFalse();
        assertThat(PointBuy.remaining(scores)).isEqualTo(-27);
    }

    @Test
    void rejectsIncompleteAbilitySet() {
        Map<Ability, Integer> incomplete = new EnumMap<>(Ability.class);
        incomplete.put(Ability.STRENGTH, 15);
        assertThat(PointBuy.isValid(incomplete)).isFalse();
    }

    @Test
    void defaultStandardArrayIsAllTens() {
        Map<Ability, Integer> scores = PointBuy.standardArray();
        assertThat(scores).hasSize(6);
        assertThat(scores.values()).allMatch(value -> value == 10);
        assertThat(PointBuy.totalCost(scores)).isEqualTo(12); // 6 x 2
    }
}
