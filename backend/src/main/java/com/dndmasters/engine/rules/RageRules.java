package com.dndmasters.engine.rules;

import com.dndmasters.domain.GameEntity;

/**
 * Barbarian's Rage at level 1: bonus action, +2 melee damage, resistance to physical damage,
 * two uses per long rest and ten rounds of duration. The prototype resets uses at combat start.
 */
public final class RageRules {

    public static final int MAX_RAGES = 2;
    public static final int DURATION_ROUNDS = 10;

    private RageRules() {
    }

    public static boolean isRaging(GameEntity entity) {
        return Boolean.TRUE.equals(entity.getData().get("raging"));
    }

    public static int ragesUsed(GameEntity entity) {
        Object used = entity.getData().get("ragesUsed");
        return used instanceof Number number ? Math.max(0, number.intValue()) : 0;
    }

    public static boolean canRage(GameEntity entity) {
        return ClassFeatures.is(entity, "barbarian")
                && !isRaging(entity)
                && ragesUsed(entity) < MAX_RAGES;
    }

    public static int damageBonus(GameEntity entity, boolean melee) {
        return melee && isRaging(entity) ? 2 : 0;
    }

    /** All weapon attacks in the prototype are physical, so Rage resistance applies to them. */
    public static boolean resistsPhysicalDamage(GameEntity entity) {
        return isRaging(entity);
    }
}
