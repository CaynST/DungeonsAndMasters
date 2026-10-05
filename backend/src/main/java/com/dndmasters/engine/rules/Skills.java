package com.dndmasters.engine.rules;

import com.dndmasters.domain.Ability;
import java.util.Locale;
import java.util.Map;

/** Maps free-form skill names (from the DM or the client) to their governing ability. */
public final class Skills {

    private static final Map<String, Ability> ABILITY_BY_SKILL = Map.ofEntries(
            Map.entry("athletics", Ability.STRENGTH),
            Map.entry("acrobatics", Ability.DEXTERITY),
            Map.entry("stealth", Ability.DEXTERITY),
            Map.entry("sleight_of_hand", Ability.DEXTERITY),
            Map.entry("sleight of hand", Ability.DEXTERITY),
            Map.entry("investigation", Ability.INTELLIGENCE),
            Map.entry("arcana", Ability.INTELLIGENCE),
            Map.entry("history", Ability.INTELLIGENCE),
            Map.entry("nature", Ability.INTELLIGENCE),
            Map.entry("religion", Ability.INTELLIGENCE),
            Map.entry("perception", Ability.WISDOM),
            Map.entry("insight", Ability.WISDOM),
            Map.entry("survival", Ability.WISDOM),
            Map.entry("medicine", Ability.WISDOM),
            Map.entry("animal_handling", Ability.WISDOM),
            Map.entry("persuasion", Ability.CHARISMA),
            Map.entry("deception", Ability.CHARISMA),
            Map.entry("intimidation", Ability.CHARISMA),
            Map.entry("performance", Ability.CHARISMA));

    private Skills() {
    }

    public static Ability abilityFor(String skill) {
        if (skill == null) {
            return Ability.INTELLIGENCE;
        }
        return ABILITY_BY_SKILL.getOrDefault(skill.toLowerCase(Locale.ROOT).trim(), Ability.INTELLIGENCE);
    }

    public static boolean isKnown(String skill) {
        return skill != null && ABILITY_BY_SKILL.containsKey(skill.toLowerCase(Locale.ROOT).trim());
    }
}
