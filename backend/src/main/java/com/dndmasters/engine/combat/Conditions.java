package com.dndmasters.engine.combat;

import com.dndmasters.domain.GameEntity;
import java.util.List;

/** Adapter between persisted entities and the pure {@link ConditionSet} model. */
public final class Conditions {

    private Conditions() {
    }

    public static ConditionSet of(GameEntity entity) {
        return ConditionSet.from(entity.getData());
    }

    /**
     * Adds or refreshes a condition. Exhaustion accumulates one level per application,
     * capped at 6 as the PHB requires.
     */
    public static void add(GameEntity entity, Condition condition, Long sourceId,
                           Integer saveDc, String saveAbility, Integer rounds) {
        ConditionSet set = of(entity);
        int level = condition == Condition.EXHAUSTION
                ? Math.min(6, set.exhaustionLevel() + 1)
                : 0;
        set = set.with(new ActiveCondition(condition, sourceId, saveDc, saveAbility, rounds, level));
        // An unconscious creature also falls prone, as the PHB states.
        if (condition == Condition.UNCONSCIOUS) {
            set = set.with(ActiveCondition.of(Condition.PRONE));
        }
        set.writeTo(entity.getData());
    }

    /** Removes a condition; returns true when it was present. */
    public static boolean remove(GameEntity entity, Condition condition) {
        ConditionSet set = of(entity);
        if (!set.has(condition)) {
            return false;
        }
        set.without(condition).writeTo(entity.getData());
        return true;
    }

    /** Ticks finite durations and returns the conditions that expired. */
    public static List<Condition> tick(GameEntity entity) {
        ConditionSet before = of(entity);
        ConditionSet after = before.tick();
        after.writeTo(entity.getData());
        return before.removedBy(after);
    }

    public static List<String> codes(GameEntity entity) {
        return of(entity).codes();
    }
}
