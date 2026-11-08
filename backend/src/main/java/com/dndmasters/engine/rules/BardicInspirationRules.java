package com.dndmasters.engine.rules;

import com.dndmasters.domain.EntityKind;
import com.dndmasters.domain.GameEntity;
import java.util.Objects;

/** Bardic Inspiration: bonus action, d6, CHA modifier uses, 60 ft range. */
public final class BardicInspirationRules {

    public static final int DIE_SIDES = 6;
    public static final int RANGE = 12;
    public static final int ROUNDS = 10;

    private BardicInspirationRules() {
    }

    public static int maxUses(int charisma) {
        return Math.max(1, Modifier.of(charisma));
    }

    public static boolean canInspire(GameEntity bard, GameEntity target, int used) {
        return ClassFeatures.is(bard, "bard")
                && target != null
                && !Objects.equals(target.getId(), bard.getId())
                && target.getKind() == EntityKind.PLAYER
                && target.isAlive()
                && !(target.getData().get("inspirationDie") instanceof Number die && die.intValue() > 0)
                && bard.cell().chebyshevTo(target.cell()) <= RANGE
                && used < maxUses(bard.getStats().charisma());
    }
}
