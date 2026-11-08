package com.dndmasters.engine.combat;

import com.dndmasters.domain.GameEntity;

/**
 * Minimal surprise rule: a creature marked as surprised loses its first combat turn.
 * The flag lives in the entity's data map and is consumed exactly once by the turn flow.
 */
public final class SurpriseRules {

    public static final String FLAG = "surprised";

    private SurpriseRules() {
    }

    public static boolean isSurprised(GameEntity entity) {
        return entity != null && Boolean.TRUE.equals(entity.getData().get(FLAG));
    }

    public static void markSurprised(GameEntity entity) {
        if (entity != null) {
            entity.getData().put(FLAG, true);
        }
    }

    /** Clears the flag and reports whether the creature must skip its turn. */
    public static boolean consumeSurprised(GameEntity entity) {
        if (!isSurprised(entity)) {
            return false;
        }
        entity.getData().put(FLAG, false);
        return true;
    }
}
