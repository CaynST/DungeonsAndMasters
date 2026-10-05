package com.dndmasters.engine.rules;

/**
 * Ability modifier table used by 5e: {@code floor((score - 10) / 2)}.
 */
public final class Modifier {

    private Modifier() {
    }

    public static int of(int score) {
        return Math.floorDiv(score - 10, 2);
    }
}
