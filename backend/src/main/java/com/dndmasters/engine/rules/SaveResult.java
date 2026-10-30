package com.dndmasters.engine.rules;

/** Result of a saving throw. */
public record SaveResult(
        String ability,
        int dieRoll,
        int secondDieRoll,
        int modifier,
        int total,
        int dc,
        boolean success) {
}
