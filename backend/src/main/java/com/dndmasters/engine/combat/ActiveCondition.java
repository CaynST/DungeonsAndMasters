package com.dndmasters.engine.combat;

/**
 * One active condition instance. {@code rounds == null} means it lasts until removed;
 * otherwise it expires when its duration reaches zero at the start of the creature's turn.
 */
public record ActiveCondition(
        Condition condition,
        Long sourceId,
        Integer saveDc,
        String saveAbility,
        Integer rounds,
        int level) {

    public static ActiveCondition of(Condition condition) {
        return new ActiveCondition(condition, null, null, null, null, 0);
    }

    /** Decrements a finite duration. */
    public ActiveCondition tick() {
        if (rounds == null) {
            return this;
        }
        return new ActiveCondition(condition, sourceId, saveDc, saveAbility, rounds - 1, level);
    }

    public boolean expired() {
        return rounds != null && rounds <= 0;
    }
}
