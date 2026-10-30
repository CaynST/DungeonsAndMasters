package com.dndmasters.engine.rules;

import com.dndmasters.domain.GameEntity;
import java.util.Locale;
import java.util.Map;

/** Normalises class names (English ids and Spanish display names) for feature gating. */
public final class ClassFeatures {

    private static final Map<String, String> IDS = Map.ofEntries(
            Map.entry("fighter", "fighter"),
            Map.entry("guerrero", "fighter"),
            Map.entry("barbarian", "barbarian"),
            Map.entry("bárbaro", "barbarian"),
            Map.entry("barbaro", "barbarian"),
            Map.entry("rogue", "rogue"),
            Map.entry("pícaro", "rogue"),
            Map.entry("picaro", "rogue"),
            Map.entry("wizard", "wizard"),
            Map.entry("mago", "wizard"),
            Map.entry("cleric", "cleric"),
            Map.entry("clérigo", "cleric"),
            Map.entry("clerigo", "cleric"),
            Map.entry("bard", "bard"),
            Map.entry("bardo", "bard"),
            Map.entry("monk", "monk"),
            Map.entry("monje", "monk"),
            Map.entry("paladin", "paladin"),
            Map.entry("paladín", "paladin"),
            Map.entry("ranger", "ranger"),
            Map.entry("explorador", "ranger"),
            Map.entry("sorcerer", "sorcerer"),
            Map.entry("hechicero", "sorcerer"),
            Map.entry("warlock", "warlock"),
            Map.entry("brujo", "warlock"),
            Map.entry("druid", "druid"),
            Map.entry("druida", "druid"));

    private ClassFeatures() {
    }

    public static String idOf(String cls) {
        if (cls == null) {
            return null;
        }
        return IDS.get(cls.toLowerCase(Locale.ROOT).trim());
    }

    /** Prefers persisted classId, then derives it from the display name. */
    public static String idOf(GameEntity entity) {
        Object explicit = entity.getData().get("classId");
        if (explicit instanceof String value && !value.isBlank()) {
            return value.toLowerCase(Locale.ROOT);
        }
        return idOf(String.valueOf(entity.getData().getOrDefault("class", "")));
    }

    public static boolean is(GameEntity entity, String classId) {
        return classId.equals(idOf(entity));
    }

    public static int level(GameEntity entity) {
        Object level = entity.getData().get("level");
        return level instanceof Number number ? Math.max(1, number.intValue()) : 1;
    }
}
