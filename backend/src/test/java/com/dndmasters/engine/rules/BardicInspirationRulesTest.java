package com.dndmasters.engine.rules;

import static org.assertj.core.api.Assertions.assertThat;

import com.dndmasters.domain.Cell;
import com.dndmasters.domain.EntityKind;
import com.dndmasters.domain.GameEntity;
import com.dndmasters.domain.Stats;
import org.junit.jupiter.api.Test;

class BardicInspirationRulesTest {

    private static GameEntity entity(long id, EntityKind kind, String cls, int charisma, int x, int y) {
        GameEntity entity = new GameEntity();
        entity.setId(id);
        entity.setKind(kind);
        entity.setAlive(true);
        entity.getData().put("class", cls);
        entity.setStats(new Stats(10, 10, 10, 10, 10, charisma));
        entity.moveTo(new Cell(x, y));
        return entity;
    }

    @Test
    void usesAreBasedOnCharismaModifierWithAMinimumOfOne() {
        assertThat(BardicInspirationRules.maxUses(8)).isEqualTo(1);
        assertThat(BardicInspirationRules.maxUses(10)).isEqualTo(1);
        assertThat(BardicInspirationRules.maxUses(16)).isEqualTo(3);
    }

    @Test
    void bardCanInspireAnotherLivingAllyInRange() {
        GameEntity bard = entity(1, EntityKind.PLAYER, "Bard", 16, 5, 5);
        GameEntity ally = entity(2, EntityKind.PLAYER, "Fighter", 10, 6, 6);

        assertThat(BardicInspirationRules.canInspire(bard, ally, 0)).isTrue();
        assertThat(BardicInspirationRules.canInspire(bard, ally, 3)).isFalse();
    }

    @Test
    void cannotInspireSelfOutOfRangeEnemiesOrWithoutUsesLeft() {
        GameEntity bard = entity(1, EntityKind.PLAYER, "Bard", 16, 5, 5);
        GameEntity self = bard;
        GameEntity enemy = entity(3, EntityKind.ENEMY, "Fighter", 10, 6, 5);
        GameEntity far = entity(4, EntityKind.PLAYER, "Fighter", 10, 30, 30);

        assertThat(BardicInspirationRules.canInspire(bard, self, 0)).isFalse();
        assertThat(BardicInspirationRules.canInspire(bard, enemy, 0)).isFalse();
        assertThat(BardicInspirationRules.canInspire(bard, far, 0)).isFalse();
        assertThat(BardicInspirationRules.canInspire(bard, far, 3)).isFalse();
    }
}
