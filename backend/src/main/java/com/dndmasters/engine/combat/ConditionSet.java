package com.dndmasters.engine.combat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Immutable set of active conditions. It is persisted on an entity's data map as
 * {@code conditions: [{code, sourceId?, saveDc?, saveAbility?, rounds?, level?}]}.
 */
public final class ConditionSet {

    private final Map<Condition, ActiveCondition> active;

    private ConditionSet(Map<Condition, ActiveCondition> active) {
        this.active = active;
    }

    public static ConditionSet empty() {
        return new ConditionSet(Collections.emptyMap());
    }

    /** Reads both the current map representation and the legacy list-of-codes format. */
    public static ConditionSet from(Map<String, Object> data) {
        Object raw = data.get("conditions");
        if (!(raw instanceof List<?> list)) {
            return empty();
        }
        Map<Condition, ActiveCondition> parsed = new EnumMap<>(Condition.class);
        for (Object item : list) {
            if (item instanceof Map<?, ?> row) {
                Condition condition = Condition.fromCode(stringValue(row.get("code")));
                if (condition == null) {
                    continue;
                }
                parsed.put(condition, new ActiveCondition(
                        condition,
                        longValue(row.get("sourceId")),
                        integerValue(row.get("saveDc")),
                        stringValue(row.get("saveAbility")),
                        integerValue(row.get("rounds")),
                        intValue(row.get("level"))));
            } else if (item instanceof String code) {
                Condition condition = Condition.fromCode(code);
                if (condition != null) {
                    parsed.put(condition, ActiveCondition.of(condition));
                }
            }
        }
        return new ConditionSet(Collections.unmodifiableMap(parsed));
    }

    public boolean has(Condition condition) {
        return active.containsKey(condition);
    }

    public ActiveCondition get(Condition condition) {
        return active.get(condition);
    }

    public Set<Condition> all() {
        return active.keySet();
    }

    public List<String> codes() {
        return active.keySet().stream().map(Enum::name).toList();
    }

    public int exhaustionLevel() {
        ActiveCondition exhaustion = active.get(Condition.EXHAUSTION);
        return exhaustion == null ? 0 : Math.max(0, exhaustion.level());
    }

    /** Adds or refreshes a condition. */
    public ConditionSet with(ActiveCondition condition) {
        Map<Condition, ActiveCondition> copy = new EnumMap<>(Condition.class);
        copy.putAll(active);
        copy.put(condition.condition(), condition);
        return new ConditionSet(Collections.unmodifiableMap(copy));
    }

    public ConditionSet without(Condition condition) {
        if (!active.containsKey(condition)) {
            return this;
        }
        Map<Condition, ActiveCondition> copy = new EnumMap<>(Condition.class);
        copy.putAll(active);
        copy.remove(condition);
        return new ConditionSet(Collections.unmodifiableMap(copy));
    }

    /** Decrements finite durations and drops conditions whose duration reached zero. */
    public ConditionSet tick() {
        Map<Condition, ActiveCondition> copy = new EnumMap<>(Condition.class);
        for (ActiveCondition condition : active.values()) {
            ActiveCondition next = condition.tick();
            if (!next.expired()) {
                copy.put(next.condition(), next);
            }
        }
        return new ConditionSet(Collections.unmodifiableMap(copy));
    }

    /** Conditions present here but absent in {@code after}, used to report expirations. */
    public List<Condition> removedBy(ConditionSet after) {
        return active.keySet().stream().filter(condition -> !after.has(condition)).toList();
    }

    public void writeTo(Map<String, Object> data) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (ActiveCondition condition : active.values()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("code", condition.condition().name());
            if (condition.sourceId() != null) {
                row.put("sourceId", condition.sourceId());
            }
            if (condition.saveDc() != null) {
                row.put("saveDc", condition.saveDc());
            }
            if (condition.saveAbility() != null) {
                row.put("saveAbility", condition.saveAbility());
            }
            if (condition.rounds() != null) {
                row.put("rounds", condition.rounds());
            }
            if (condition.level() > 0) {
                row.put("level", condition.level());
            }
            rows.add(row);
        }
        data.put("conditions", rows);
    }

    public boolean incapacitated() {
        return any(Condition::incapacitating);
    }

    public boolean speedZero() {
        return any(Condition::speedZero);
    }

    public boolean autoFailsStrDexSaves() {
        return any(Condition::autoFailsStrDexSaves);
    }

    public boolean attackerDisadvantage() {
        return any(Condition::attackerDisadvantage);
    }

    public boolean grantsAdvantageToAttackers() {
        return any(Condition::grantsAdvantageToAttackers);
    }

    public boolean meleeCritAgainst() {
        return any(Condition::meleeCritAgainst);
    }

    private boolean any(Predicate<Condition> predicate) {
        return active.keySet().stream().anyMatch(predicate);
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static Integer integerValue(Object value) {
        return value instanceof Number number ? number.intValue() : null;
    }

    private static Long longValue(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }

    private static int intValue(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }
}
