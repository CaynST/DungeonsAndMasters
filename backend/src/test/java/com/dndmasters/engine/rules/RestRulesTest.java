package com.dndmasters.engine.rules;

import static org.assertj.core.api.Assertions.assertThat;

import com.dndmasters.domain.GameEntity;
import org.junit.jupiter.api.Test;

/** Pure rest and hit-die rules, independent from Spring and from the dice source. */
class RestRulesTest {

    private GameEntity entity(String classId, Integer level) {
        GameEntity entity = new GameEntity();
        entity.getData().put("classId", classId);
        if (level != null) {
            entity.getData().put("level", level);
        }
        return entity;
    }

    @Test
    void classHitDiceFollowTheSrdTable() {
        assertThat(RestRules.hitDieForClass("fighter")).isEqualTo("d10");
        assertThat(RestRules.hitDieForClass("paladin")).isEqualTo("d10");
        assertThat(RestRules.hitDieForClass("ranger")).isEqualTo("d10");
        assertThat(RestRules.hitDieForClass("barbarian")).isEqualTo("d12");
        assertThat(RestRules.hitDieForClass("wizard")).isEqualTo("d6");
        assertThat(RestRules.hitDieForClass("sorcerer")).isEqualTo("d6");
        assertThat(RestRules.hitDieForClass("bard")).isEqualTo("d8");
        assertThat(RestRules.hitDieForClass("cleric")).isEqualTo("d8");
        assertThat(RestRules.hitDieForClass("mago")).isEqualTo("d6");
        assertThat(RestRules.hitDieForClass(null)).isEqualTo("d8");
        assertThat(RestRules.hitDieForClass("unknown")).isEqualTo("d8");
    }

    @Test
    void persistedHitDieWinsOverTheClassFallback() {
        GameEntity entity = entity("wizard", 1);
        entity.getData().put("hitDie", "d12");

        assertThat(RestRules.hitDieSides(entity)).isEqualTo(12);
        assertThat(RestRules.hitDieLabel(entity)).isEqualTo("d12");

        entity.getData().put("hitDie", "1d10");
        assertThat(RestRules.hitDieSides(entity)).isEqualTo(10);
        assertThat(RestRules.hitDieLabel(entity)).isEqualTo("d10");
    }

    @Test
    void legacyTokensFallBackToClassHitDieAndLevelDice() {
        GameEntity fighter = entity("fighter", null);
        assertThat(RestRules.hitDieSides(fighter)).isEqualTo(10);
        assertThat(RestRules.hitDiceMax(fighter)).isEqualTo(1);
        assertThat(RestRules.hitDiceRemaining(fighter)).isEqualTo(1);

        GameEntity unknown = entity(null, 3);
        assertThat(RestRules.hitDieSides(unknown)).isEqualTo(8);
        assertThat(RestRules.hitDiceMax(unknown)).isEqualTo(3);
    }

    @Test
    void persistedHitDiceAreClampedToTheMaximum() {
        GameEntity entity = entity("fighter", 2);
        entity.getData().put("hitDiceMax", 2);
        entity.getData().put("hitDiceRemaining", 5);
        assertThat(RestRules.hitDiceRemaining(entity)).isEqualTo(2);

        entity.getData().put("hitDiceRemaining", -3);
        assertThat(RestRules.hitDiceRemaining(entity)).isZero();
    }

    @Test
    void shortRestHealingAddsTheConstitutionModifier() {
        assertThat(RestRules.shortRestHeal(7, 2)).isEqualTo(9);
        assertThat(RestRules.shortRestHeal(1, -1)).isZero();
        assertThat(RestRules.shortRestHeal(10, 0)).isEqualTo(10);
    }

    @Test
    void longRestRestoresAtLeastHalfOfTheHitDiceWithAMinimumOfOne() {
        assertThat(RestRules.longRestHitDiceRemaining(1, 0)).isEqualTo(1);
        assertThat(RestRules.longRestHitDiceRemaining(2, 0)).isEqualTo(1);
        assertThat(RestRules.longRestHitDiceRemaining(3, 0)).isEqualTo(2);
        assertThat(RestRules.longRestHitDiceRemaining(4, 1)).isEqualTo(2);
        assertThat(RestRules.longRestHitDiceRemaining(4, 4)).isEqualTo(4);
    }
}
