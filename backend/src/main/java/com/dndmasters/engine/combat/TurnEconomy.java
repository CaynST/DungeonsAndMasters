package com.dndmasters.engine.combat;

import java.util.Map;

/**
 * Per-turn action economy stored on an entity's data map:
 * one action, movement up to speed (double with Dash), one bonus action and one reaction.
 * Flags reset at the start of the creature's turn, as the PHB requires.
 */
public final class TurnEconomy {

    private TurnEconomy() {
    }

    public static void reset(Map<String, Object> data, int speed) {
        data.put("actionUsed", false);
        data.put("bonusUsed", false);
        data.put("movementUsed", 0);
        data.put("movementMax", speed);
        data.put("dodging", false);
        data.put("disengaged", false);
        data.put("reactionUsed", false);
        data.put("dashUsed", false);
    }

    public static int movementAllowance(Map<String, Object> data, int speed) {
        return data.get("movementMax") instanceof Number n ? n.intValue() : speed;
    }

    public static int movementUsed(Map<String, Object> data) {
        return data.get("movementUsed") instanceof Number n ? n.intValue() : 0;
    }

    public static boolean flag(Map<String, Object> data, String key) {
        return Boolean.TRUE.equals(data.get(key));
    }
}
