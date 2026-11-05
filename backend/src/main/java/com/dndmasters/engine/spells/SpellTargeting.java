package com.dndmasters.engine.spells;

import com.dndmasters.domain.EntityKind;
import com.dndmasters.domain.GameEntity;
import java.util.List;
import java.util.Objects;

/** Area targeting helpers. The grid uses Chebyshev distance, matching the rest of the engine. */
public final class SpellTargeting {

    private SpellTargeting() {
    }

    /**
     * Approximates a 5e cube centred on the caster: living creatures (players, enemies, NPCs)
     * within {@code radius} squares, excluding the caster and items.
     */
    public static List<GameEntity> creaturesInCube(GameEntity caster, List<GameEntity> entities, int radius) {
        return entities.stream()
                .filter(GameEntity::isAlive)
                .filter(entity -> entity.getKind() != EntityKind.ITEM)
                .filter(entity -> !Objects.equals(entity.getId(), caster.getId()))
                .filter(entity -> entity.cell().chebyshevTo(caster.cell()) <= radius)
                .toList();
    }
}
