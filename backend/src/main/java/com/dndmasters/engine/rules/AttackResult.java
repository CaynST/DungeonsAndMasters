package com.dndmasters.engine.rules;

public record AttackResult(
        String attackName,
        int dieRoll,
        int attackBonus,
        int total,
        int targetAc,
        boolean hit,
        boolean critical,
        boolean fumble) {
}
