package com.dndmasters.engine.rules;

import com.dndmasters.domain.GameEntity;
import java.util.Locale;

/**
 * Short/long rest and hit-die rules. The class is pure: it derives hit dice from persisted
 * entity data (or class/level fallbacks for tokens created before hit dice existed) and exposes
 * the arithmetic used by {@code GameService}. Randomness stays in the injected dice engine.
 */
public final class RestRules {

    public static final String DEFAULT_HIT_DIE = "d8";
    private static final int DEFAULT_HIT_DIE_SIDES = 8;

    private RestRules() {
    }

    /** SRD hit die per class id; unknown or missing classes use the d8 fallback. */
    public static String hitDieForClass(String classId) {
        String id = ClassFeatures.idOf(classId);
        if (id == null) {
            return DEFAULT_HIT_DIE;
        }
        return switch (id) {
            case "barbarian" -> "d12";
            case "fighter", "paladin", "ranger" -> "d10";
            case "wizard", "sorcerer" -> "d6";
            default -> DEFAULT_HIT_DIE;
        };
    }

    /** Faces of the entity's hit die, preferring {@code data.hitDie} over the class fallback. */
    public static int hitDieSides(GameEntity entity) {
        return sidesOf(hitDieLabel(entity));
    }

    /** Normalised hit-die label (for example {@code d10}) used in events and the UI. */
    public static String hitDieLabel(GameEntity entity) {
        Object persisted = entity.getData().get("hitDie");
        if (persisted instanceof String label && !label.isBlank()) {
            return "d" + sidesOf(label);
        }
        return hitDieForClass(ClassFeatures.idOf(entity));
    }

    /** Total hit dice: the persisted maximum, or the character level for legacy tokens. */
    public static int hitDiceMax(GameEntity entity) {
        if (entity.getData().get("hitDiceMax") instanceof Number number && number.intValue() > 0) {
            return number.intValue();
        }
        return ClassFeatures.level(entity);
    }

    /** Remaining hit dice are clamped to [0, max]; legacy tokens start with their full pool. */
    public static int hitDiceRemaining(GameEntity entity) {
        int max = hitDiceMax(entity);
        if (entity.getData().get("hitDiceRemaining") instanceof Number number) {
            return Math.max(0, Math.min(max, number.intValue()));
        }
        return max;
    }

    /** One hit die plus the Constitution modifier; negative modifiers never heal below zero. */
    public static int shortRestHeal(int dieRoll, int conModifier) {
        return Math.max(0, dieRoll + conModifier);
    }

    /** A long rest recovers spent dice up to half the pool, with at least one die. */
    public static int longRestHitDiceRemaining(int max, int remaining) {
        int restored = Math.max(1, (max + 1) / 2);
        return Math.max(remaining, Math.min(max, restored));
    }

    private static int sidesOf(String label) {
        String normalized = label.toLowerCase(Locale.ROOT).trim();
        int marker = normalized.indexOf('d');
        int start = marker >= 0 ? marker + 1 : 0;
        while (start < normalized.length() && !Character.isDigit(normalized.charAt(start))) {
            start++;
        }
        int end = start;
        while (end < normalized.length() && Character.isDigit(normalized.charAt(end))) {
            end++;
        }
        if (start == end) {
            return DEFAULT_HIT_DIE_SIDES;
        }
        try {
            int sides = Integer.parseInt(normalized.substring(start, end));
            return sides > 0 ? sides : DEFAULT_HIT_DIE_SIDES;
        } catch (NumberFormatException e) {
            return DEFAULT_HIT_DIE_SIDES;
        }
    }
}
