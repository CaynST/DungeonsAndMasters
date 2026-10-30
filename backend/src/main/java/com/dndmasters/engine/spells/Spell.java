package com.dndmasters.engine.spells;

/**
 * A castable spell. Ranges are expressed in grid squares (5 ft each). The prototype models
 * single-target spells only; area spells and concentration are intentionally deferred.
 */
public record Spell(
        String id,
        String name,
        int level,
        String school,
        String description,
        int range,
        Kind kind,
        String damage,
        String damageType,
        String saveAbility,
        String heal,
        boolean bonusAction,
        boolean concentration) {

    public enum Kind {
        ATTACK,
        SAVE,
        AUTO,
        HEAL
    }

    public boolean isCantrip() {
        return level == 0;
    }

    public static Spell attack(String id, String name, int level, String school, String description,
                               int range, String damage, String damageType) {
        return new Spell(id, name, level, school, description, range, Kind.ATTACK,
                damage, damageType, null, null, false, false);
    }

    public static Spell save(String id, String name, int level, String school, String description,
                             int range, String damage, String damageType, String saveAbility) {
        return new Spell(id, name, level, school, description, range, Kind.SAVE,
                damage, damageType, saveAbility, null, false, false);
    }

    public static Spell auto(String id, String name, int level, String school, String description,
                             int range, String damage, String damageType) {
        return new Spell(id, name, level, school, description, range, Kind.AUTO,
                damage, damageType, null, null, false, false);
    }

    public static Spell heal(String id, String name, int level, String school, String description,
                             int range, String heal, boolean bonusAction) {
        return new Spell(id, name, level, school, description, range, Kind.HEAL,
                null, null, null, heal, bonusAction, false);
    }
}
