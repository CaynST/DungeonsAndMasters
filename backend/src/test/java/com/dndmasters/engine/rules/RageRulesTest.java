package com.dndmasters.engine.rules;

import static org.assertj.core.api.Assertions.assertThat;

import com.dndmasters.domain.GameEntity;
import org.junit.jupiter.api.Test;

class RageRulesTest {

    private static GameEntity entity(String cls) {
        GameEntity entity = new GameEntity();
        entity.getData().put("class", cls);
        return entity;
    }

    @Test
    void onlyBarbariansCanRage() {
        assertThat(RageRules.canRage(entity("Barbarian"))).isTrue();
        assertThat(RageRules.canRage(entity("Fighter"))).isFalse();
    }

    @Test
    void rageCannotBeEnteredTwiceAndIsLimitedToTwoUses() {
        GameEntity barbarian = entity("Barbarian");
        barbarian.getData().put("raging", true);
        assertThat(RageRules.canRage(barbarian)).isFalse();

        barbarian.getData().put("raging", false);
        barbarian.getData().put("ragesUsed", 2);
        assertThat(RageRules.canRage(barbarian)).isFalse();
    }

    @Test
    void ragingAddsTwoMeleeDamageAndHalvesPhysicalDamageTaken() {
        GameEntity barbarian = entity("Barbarian");
        barbarian.getData().put("raging", true);

        assertThat(RageRules.damageBonus(barbarian, true)).isEqualTo(2);
        assertThat(RageRules.damageBonus(barbarian, false)).isZero();
        assertThat(RageRules.resistsPhysicalDamage(barbarian)).isTrue();
        assertThat(RageRules.resistsPhysicalDamage(entity("Fighter"))).isFalse();
    }
}
