package com.dndmasters.engine.spells;

import static org.assertj.core.api.Assertions.assertThat;

import com.dndmasters.domain.Cell;
import com.dndmasters.domain.EntityKind;
import com.dndmasters.domain.GameEntity;
import java.util.List;
import org.junit.jupiter.api.Test;

class SpellTargetingTest {

    private static GameEntity entity(long id, EntityKind kind, int x, int y, boolean alive) {
        GameEntity entity = new GameEntity();
        entity.setId(id);
        entity.setKind(kind);
        entity.setAlive(alive);
        entity.moveTo(new Cell(x, y));
        return entity;
    }

    @Test
    void cubeIncludesLivingCreaturesWithinRadiusAndExcludesCasterDeadAndItems() {
        GameEntity caster = entity(1, EntityKind.PLAYER, 5, 5, true);
        GameEntity near = entity(2, EntityKind.ENEMY, 6, 6, true);
        GameEntity ally = entity(3, EntityKind.PLAYER, 5, 7, true);
        GameEntity far = entity(4, EntityKind.ENEMY, 9, 9, true);
        GameEntity dead = entity(5, EntityKind.ENEMY, 5, 6, false);
        GameEntity item = entity(6, EntityKind.ITEM, 5, 6, true);

        List<GameEntity> targets = SpellTargeting.creaturesInCube(caster,
                List.of(caster, near, ally, far, dead, item), 2);

        assertThat(targets).extracting(GameEntity::getId)
                .containsExactlyInAnyOrder(2L, 3L);
    }
}
