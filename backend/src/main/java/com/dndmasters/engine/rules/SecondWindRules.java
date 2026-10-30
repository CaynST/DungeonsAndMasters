package com.dndmasters.engine.rules;

/** Fighter's Second Wind: bonus action, 1d10 + fighter level healing, one use per rest. */
public final class SecondWindRules {

    private SecondWindRules() {
    }

    public static int healAmount(int d10, int level) {
        return Math.max(1, d10) + Math.max(1, level);
    }

    public static int maxUses(int level) {
        return 1;
    }
}
