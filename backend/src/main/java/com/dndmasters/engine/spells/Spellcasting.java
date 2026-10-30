package com.dndmasters.engine.spells;

import com.dndmasters.domain.Ability;
import com.dndmasters.domain.GameEntity;
import com.dndmasters.engine.rules.ClassFeatures;
import com.dndmasters.engine.rules.Modifier;

/** Spell attack bonuses, save DCs, slots and casting abilities. */
public final class Spellcasting {

    private Spellcasting() {
    }

    public static Ability abilityFor(String classId) {
        if (classId == null) {
            return null;
        }
        return switch (classId) {
            case "wizard" -> Ability.INTELLIGENCE;
            case "cleric", "druid", "ranger" -> Ability.WISDOM;
            case "bard", "sorcerer", "warlock", "paladin" -> Ability.CHARISMA;
            default -> null;
        };
    }

    public static int proficiencyBonus(int level) {
        return 2 + Math.max(0, level - 1) / 4;
    }

    public static int attackBonus(GameEntity caster, String classId) {
        Ability ability = abilityFor(classId);
        if (ability == null) {
            return 0;
        }
        return proficiencyBonus(ClassFeatures.level(caster)) + Modifier.of(caster.getStats().get(ability));
    }

    public static int saveDc(GameEntity caster, String classId) {
        Ability ability = abilityFor(classId);
        if (ability == null) {
            return 8;
        }
        return 8 + proficiencyBonus(ClassFeatures.level(caster))
                + Modifier.of(caster.getStats().get(ability));
    }

    /** Level-1 full casters start with two slots; the pool grows with level up to four. */
    public static int maxSlots(int level) {
        return level <= 0 ? 0 : Math.min(4, level + 1);
    }

    public static boolean isCaster(String classId) {
        return !SpellCatalog.forClass(classId).spells().isEmpty();
    }
}
