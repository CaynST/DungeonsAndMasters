package com.dndmasters.engine.rules;

public record InitiativeEntry(
        long entityId,
        String name,
        String kind,
        int dieRoll,
        int dexterityModifier,
        int initiative,
        int orderIndex) {
}
