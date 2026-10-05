package com.dndmasters.engine.rules;

import com.dndmasters.domain.GameEntity;
import com.dndmasters.domain.PlayerCharacter;

/** Adapts domain models to the engine's {@link Combatant} view. */
public final class Combatants {

    private Combatants() {
    }

    public static Combatant of(GameEntity entity) {
        return new Combatant(
                entity.getId(),
                entity.getName(),
                entity.getKind().name(),
                entity.getHp(),
                entity.getMaxHp(),
                entity.getAc(),
                entity.getStats(),
                entity.isAlive());
    }

    public static Combatant of(PlayerCharacter character) {
        return new Combatant(
                character.getId(),
                character.getName(),
                "PLAYER",
                character.getHp(),
                character.getMaxHp(),
                character.getAc(),
                character.getStats(),
                character.alive());
    }
}
