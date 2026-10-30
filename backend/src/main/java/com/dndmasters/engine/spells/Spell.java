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
        String buff,
        boolean bonusAction,
        boolean concentration) {

    public enum Kind {
        ATTACK,
        SAVE,
        AUTO,
        HEAL,
        BUFF
    }

    public boolean isCantrip() {
        return level == 0;
    }

    public static Spell attack(String id, String name, int level, String school, String description,
                               int range, String damage, String damageType) {
        return new Spell(id, name, level, school, description, range, Kind.ATTACK,
                damage, damageType, null, null, null, false, false);
    }

    public static Spell save(String id, String name, int level, String school, String description,
                             int range, String damage, String damageType, String saveAbility) {
        return new Spell(id, name, level, school, description, range, Kind.SAVE,
                damage, damageType, saveAbility, null, null, false, false);
    }

    public static Spell auto(String id, String name, int level, String school, String description,
                             int range, String damage, String damageType) {
        return new Spell(id, name, level, school, description, range, Kind.AUTO,
                damage, damageType, null, null, null, false, false);
    }

    public static Spell heal(String id, String name, int level, String school, String description,
                             int range, String heal, boolean bonusAction) {
        return new Spell(id, name, level, school, description, range, Kind.HEAL,
                null, null, null, heal, null, bonusAction, false);
    }

    public static Spell buff(String id, String name, int level, String school, String description,
                             int range, String buff, boolean bonusAction, boolean concentration) {
        return new Spell(id, name, level, school, description, range, Kind.BUFF,
                null, null, null, null, buff, bonusAction, concentration);
    }
}
