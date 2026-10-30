package com.dndmasters.engine.rules;

import com.dndmasters.domain.EntityKind;
import com.dndmasters.domain.GameEntity;
import com.dndmasters.engine.combat.Conditions;
import java.util.List;
import java.util.Objects;

/**
 * Rogue's Sneak Attack (level 1: 1d6). It applies once per turn when the rogue has advantage
 * or a conscious ally is adjacent to the target, and never when the attack has disadvantage.
 * Weapon finesse is not modelled yet; any rogue attack qualifies.
 */
public final class SneakAttackRules {

    private SneakAttackRules() {
    }

    public static boolean isRogue(GameEntity entity) {
        return ClassFeatures.is(entity, "rogue");
    }

    public static int diceCount(int level) {
        return 1 + Math.max(0, level - 1) / 2;
    }

    public static boolean canUse(GameEntity attacker, boolean advantage,
                                 boolean disadvantage, boolean allyAdjacent) {
        if (!isRogue(attacker) || Boolean.TRUE.equals(attacker.getData().get("sneakAttackUsed"))) {
            return false;
        }
        if (disadvantage) {
            return false;
        }
        return advantage || allyAdjacent;
    }

    /** Finds a living, conscious party member within 5 ft of the target. */
    public static boolean allyAdjacent(GameEntity attacker, GameEntity target, List<GameEntity> sceneEntities) {
        return sceneEntities.stream().anyMatch(entity ->
                !Objects.equals(entity.getId(), attacker.getId())
                        && entity.getKind() == EntityKind.PLAYER
                        && entity.isAlive()
                        && entity.cell().chebyshevTo(target.cell()) <= 1
                        && !Conditions.of(entity).incapacitated());
    }
}
