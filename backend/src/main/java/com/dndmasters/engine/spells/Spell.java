package com.dndmasters.engine.spells;

/**
 * A castable spell. Ranges are expressed in grid squares (5 ft each). Supports single-target,
 * self-centred area, concentration, saves and healing. Component metadata is descriptive; the
 * engine currently enforces only material components through a focus/component pouch.
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
        String components,
        String material,
        boolean bonusAction,
        boolean concentration) {

    public enum Kind {
        ATTACK,
        SAVE,
        AUTO,
        HEAL,
        BUFF,
        AREA
    }

    public boolean isCantrip() {
        return level == 0;
    }

    public static Spell attack(String id, String name, int level, String school, String description,
                               int range, String damage, String damageType) {
        return new Spell(id, name, level, school, description, range, Kind.ATTACK,
                damage, damageType, null, null, null, "V, S", null, false, false);
    }

    public static Spell save(String id, String name, int level, String school, String description,
                             int range, String damage, String damageType, String saveAbility) {
        return new Spell(id, name, level, school, description, range, Kind.SAVE,
                damage, damageType, saveAbility, null, null, "V, S", null, false, false);
    }

    public static Spell auto(String id, String name, int level, String school, String description,
                             int range, String damage, String damageType) {
        return new Spell(id, name, level, school, description, range, Kind.AUTO,
                damage, damageType, null, null, null, "V, S", null, false, false);
    }

    public static Spell heal(String id, String name, int level, String school, String description,
                             int range, String heal, boolean bonusAction) {
        return new Spell(id, name, level, school, description, range, Kind.HEAL,
                null, null, null, heal, null, "V, S", null, bonusAction, false);
    }

    public static Spell buff(String id, String name, int level, String school, String description,
                             int range, String buff, boolean bonusAction, boolean concentration) {
        return new Spell(id, name, level, school, description, range, Kind.BUFF,
                null, null, null, null, buff, "V, S", null, bonusAction, concentration);
    }

    /** Area spell centred on the caster; {@code radius} is expressed in grid squares. */
    public static Spell area(String id, String name, int level, String school, String description,
                             int radius, String damage, String damageType, String saveAbility) {
        return new Spell(id, name, level, school, description, radius, Kind.AREA,
                damage, damageType, saveAbility, null, null, "V, S", null, false, false);
    }

    /** Returns a copy with component metadata; used by the catalog for spells with M components. */
    public Spell withComponents(String components, String material) {
        return new Spell(id, name, level, school, description, range, kind, damage, damageType,
                saveAbility, heal, buff, components, material, bonusAction, concentration);
    }
}
