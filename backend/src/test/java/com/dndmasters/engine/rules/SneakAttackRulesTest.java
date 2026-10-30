package com.dndmasters.engine.rules;

import static org.assertj.core.api.Assertions.assertThat;

import com.dndmasters.domain.EntityKind;
import com.dndmasters.domain.GameEntity;
import java.util.List;
import org.junit.jupiter.api.Test;

class SneakAttackRulesTest {

    private static GameEntity entity(String cls) {
        GameEntity entity = new GameEntity();
        entity.getData().put("class", cls);
        entity.setAlive(true);
        return entity;
    }

    @Test
    void appliesWithAdvantageOrAnAdjacentAlly() {
        GameEntity rogue = entity("Rogue");

        assertThat(SneakAttackRules.canUse(rogue, true, false, false)).isTrue();
        assertThat(SneakAttackRules.canUse(rogue, false, false, true)).isTrue();
        assertThat(SneakAttackRules.canUse(rogue, false, false, false)).isFalse();
    }

    @Test
    void disadvantageBlocksSneakAttackEvenWithAnAlly() {
        GameEntity rogue = entity("Rogue");

        assertThat(SneakAttackRules.canUse(rogue, false, true, true)).isFalse();
    }

    @Test
    void onlyRoguesAndOnlyOncePerTurn() {
        GameEntity rogue = entity("Rogue");
        assertThat(SneakAttackRules.canUse(entity("Fighter"), true, false, true)).isFalse();

        rogue.getData().put("sneakAttackUsed", true);
        assertThat(SneakAttackRules.canUse(rogue, true, false, true)).isFalse();
    }

    @Test
    void findsALivingConsciousAllyAdjacentToTheTarget() {
        GameEntity rogue = entity("Rogue");
        rogue.setId(1L);
        GameEntity target = entity("Enemy");
        target.setId(2L);
        target.setKind(EntityKind.ENEMY);
        target.moveTo(new com.dndmasters.domain.Cell(5, 5));

        GameEntity ally = entity("Fighter");
        ally.setId(3L);
        ally.setKind(EntityKind.PLAYER);
        ally.moveTo(new com.dndmasters.domain.Cell(6, 5));

        assertThat(SneakAttackRules.allyAdjacent(rogue, target, List.of(rogue, ally))).isTrue();

        ally.moveTo(new com.dndmasters.domain.Cell(9, 9));
        assertThat(SneakAttackRules.allyAdjacent(rogue, target, List.of(rogue, ally))).isFalse();
    }
}
