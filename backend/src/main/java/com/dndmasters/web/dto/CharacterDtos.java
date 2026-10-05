package com.dndmasters.web.dto;

import com.dndmasters.domain.Ability;
import com.dndmasters.rules.Edition;
import com.dndmasters.rules.RuleSet;
import java.util.List;
import java.util.Map;

/** DTOs for the character creation wizard. */
public final class CharacterDtos {

    private CharacterDtos() {
    }

    public record AbilityScores(
            int strength, int dexterity, int constitution, int intelligence, int wisdom, int charisma) {

        public Map<Ability, Integer> toMap() {
            return Map.of(
                    Ability.STRENGTH, strength,
                    Ability.DEXTERITY, dexterity,
                    Ability.CONSTITUTION, constitution,
                    Ability.INTELLIGENCE, intelligence,
                    Ability.WISDOM, wisdom,
                    Ability.CHARISMA, charisma);
        }

        public static AbilityScores from(Map<Ability, Integer> scores) {
            return new AbilityScores(
                    scores.getOrDefault(Ability.STRENGTH, 10),
                    scores.getOrDefault(Ability.DEXTERITY, 10),
                    scores.getOrDefault(Ability.CONSTITUTION, 10),
                    scores.getOrDefault(Ability.INTELLIGENCE, 10),
                    scores.getOrDefault(Ability.WISDOM, 10),
                    scores.getOrDefault(Ability.CHARISMA, 10));
        }
    }

    public record CreateCharacterRequest(
            String name,
            String edition,
            String speciesId,
            String classId,
            String backgroundId,
            AbilityScores scores,
            List<String> skills,
            String armor,
            String weapon) {
    }

    // ---- Ruleset catalogue served to the frontend ----

    public record RulesetView(String edition, String displayName, List<SpeciesView> species,
                              List<ClassView> classes, List<BackgroundView> backgrounds) {
    }

    public record SpeciesView(String id, String name, int speed, String size,
                              Map<String, Integer> abilityBonuses, boolean darkvision, String description) {
    }

    public record ClassView(String id, String name, String hitDie, int skillChoiceCount,
                            List<String> skillChoices, List<String> savingThrows, String startingArmor,
                            List<String> weapons, String primaryAbility, String description) {
    }

    public record BackgroundView(String id, String name, List<String> skills,
                                 Map<String, Integer> abilityBonuses, String description) {
    }

    public static RulesetView toView(RuleSet set) {
        List<SpeciesView> species = set.species().values().stream()
                .map(s -> new SpeciesView(s.id(), s.name(), s.speed(), s.size(), s.abilityBonuses(),
                        s.darkvision(), s.description()))
                .toList();
        List<ClassView> classes = set.classes().values().stream()
                .map(c -> new ClassView(c.id(), c.name(), c.hitDie(), c.skillChoiceCount(), c.skillChoices(),
                        c.savingThrows(), c.startingArmor(), c.weapons(), c.primaryAbility(), c.description()))
                .toList();
        List<BackgroundView> backgrounds = set.backgrounds().values().stream()
                .map(b -> new BackgroundView(b.id(), b.name(), b.skills(), b.abilityBonuses(), b.description()))
                .toList();
        return new RulesetView(set.edition().name(), set.edition().displayName(), species, classes, backgrounds);
    }
}
