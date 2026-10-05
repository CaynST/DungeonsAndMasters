package com.dndmasters.engine.rules;

public record CheckResult(
        String skill,
        int dc,
        int dieRoll,
        int secondDieRoll,
        int modifier,
        int total,
        boolean success,
        boolean natural20,
        boolean natural1) {
}
