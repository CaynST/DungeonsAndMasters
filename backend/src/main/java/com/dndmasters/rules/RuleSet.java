package com.dndmasters.rules;

import java.util.List;
import java.util.Map;

/**
 * Static SRD reference data for an edition: species/races, classes, backgrounds and
 * their derived bonuses. Data is code (not DB) so it is versioned, testable and typed.
 */
public record RuleSet(
        Edition edition,
        Map<String, Species> species,
        Map<String, CharacterClass> classes,
        Map<String, Background> backgrounds,
        Map<String, List<String>> subclasses) {

    public List<String> subclassesOf(String classId) {
        return subclasses.getOrDefault(normalize(classId), List.of());
    }

    public Species speciesOrThrow(String id) {
        Species value = species.get(normalize(id));
        if (value == null) {
            throw new IllegalArgumentException("Unknown species/race: " + id);
        }
        return value;
    }

    public CharacterClass classOrThrow(String id) {
        CharacterClass value = classes.get(normalize(id));
        if (value == null) {
            throw new IllegalArgumentException("Unknown class: " + id);
        }
        return value;
    }

    public Background backgroundOrThrow(String id) {
        Background value = backgrounds.get(normalize(id));
        if (value == null) {
            throw new IllegalArgumentException("Unknown background: " + id);
        }
        return value;
    }

    static String normalize(String id) {
        return id == null ? "" : id.trim().toLowerCase().replace(" ", "-").replace("_", "-");
    }

    /**
     * A playable species. Ability bonuses are applied at creation; in 5e 2024 they
     * usually come from the background instead and this map is empty.
     */
    public record Species(
            String id,
            String name,
            int speed,
            String size,
            Map<String, Integer> abilityBonuses,
            boolean darkvision,
            String description) {
    }

    /** A character class: hit die, saves, skill choices, starting armor and weapons. */
    public record CharacterClass(
            String id,
            String name,
            String hitDie,
            int skillChoiceCount,
            java.util.List<String> skillChoices,
            java.util.List<String> savingThrows,
            String startingArmor,
            java.util.List<String> weapons,
            String primaryAbility,
            String description) {
    }

    /** A background granting skills, tools and (in 2024) ability score increases. */
    public record Background(
            String id,
            String name,
            java.util.List<String> skills,
            Map<String, Integer> abilityBonuses,
            String description) {
    }
}
