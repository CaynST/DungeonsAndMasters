package com.dndmasters.engine.character;

import com.dndmasters.domain.Ability;
import com.dndmasters.rules.Edition;
import com.dndmasters.rules.RuleSet;
import java.util.List;
import java.util.Map;

/**
 * Player input for character creation. Everything derived (HP, AC, saves, attacks) is
 * computed by the builder from the ruleset, never sent by the client.
 */
public record CharacterBuildRequest(
        String name,
        Edition edition,
        String speciesId,
        String classId,
        String backgroundId,
        Map<Ability, Integer> baseScores,
        List<String> chosenSkills,
        String armor,
        String weapon) {

    public CharacterBuildRequest {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        if (baseScores == null) {
            throw new IllegalArgumentException("Faltan las características base");
        }
        chosenSkills = chosenSkills == null ? List.of() : List.copyOf(chosenSkills);
    }

    /** Convenience: an all-10 base with the given primary score bumped. */
    public static CharacterBuildRequest simple(String name, Edition edition, String speciesId,
                                               String classId, String backgroundId) {
        return new CharacterBuildRequest(name, edition, speciesId, classId, backgroundId,
                PointBuy.standardArray(), List.of(), null, null);
    }

    public RuleSet.Species species(RuleSet set) {
        return set.speciesOrThrow(speciesId);
    }

    public RuleSet.CharacterClass characterClass(RuleSet set) {
        return set.classOrThrow(classId);
    }

    public RuleSet.Background background(RuleSet set) {
        return set.backgroundOrThrow(backgroundId);
    }
}
