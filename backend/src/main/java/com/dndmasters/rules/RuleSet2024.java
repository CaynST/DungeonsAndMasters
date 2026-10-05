package com.dndmasters.rules;

import com.dndmasters.rules.RuleSet.Background;
import com.dndmasters.rules.RuleSet.CharacterClass;
import com.dndmasters.rules.RuleSet.Species;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * D&D 5e (2024) SRD reference data. Key differences from 2014: "species" instead of
 * "race", ability score increases come from the background, and backgrounds also grant
 * an origin feat.
 */
public final class RuleSet2024 {

    private RuleSet2024() {
    }

    public static RuleSet build() {
        return new RuleSet(Edition.DND_5E_2024, species(), classes(), backgrounds());
    }

    private static Map<String, Species> species() {
        Map<String, Species> map = new LinkedHashMap<>();
        // In 2024 the ability bonuses are granted by the background, so they are empty here.
        register(map, new Species("human", "Humano", 30, "Mediano", Map.of(), false,
                "Versátiles y ambiciosos. En 2024 las características provienen del trasfondo."));
        register(map, new Species("elf", "Elfo", 30, "Mediano", Map.of(), true,
                "Gráciles y longevos, con linaje feérico y visión en la oscuridad."));
        register(map, new Species("dwarf", "Enano", 30, "Mediano", Map.of(), true,
                "Resistentes y firmes, con visión en la oscuridad y vigor enano."));
        register(map, new Species("halfling", "Mediano", 30, "Pequeño", Map.of(), false,
                "Pequeños, valientes y afortunados."));
        register(map, new Species("gnome", "Gnomo", 30, "Pequeño", Map.of(), true,
                "Curiosos e ingeniosos, con visión en la oscuridad."));
        register(map, new Species("goliath", "Goliat", 35, "Mediano", Map.of(), false,
                "Gigantes de las tierras altas, fuertes y resistentes."));
        register(map, new Species("orc", "Orco", 30, "Mediano", Map.of(), true,
                "Impetuosos y resistentes, con visión en la oscuridad."));
        register(map, new Species("tiefling", "Tiefling", 30, "Mediano", Map.of(), true,
                "Linaje infernal que otorga magia innata y visión en la oscuridad."));
        return map;
    }

    private static Map<String, CharacterClass> classes() {
        Map<String, CharacterClass> map = new LinkedHashMap<>();
        register(map, new CharacterClass("fighter", "Guerrero", "d10", 2,
                List.of("acrobatics", "animal_handling", "athletics", "history", "insight", "intimidation", "perception", "survival"),
                List.of("strength", "constitution"), "cadena", List.of("longsword", "shield", "javelin"),
                "strength", "Maestro del combate y las armas."));
        register(map, new CharacterClass("wizard", "Mago", "d6", 2,
                List.of("arcana", "history", "insight", "investigation", "medicine", "religion"),
                List.of("intelligence", "wisdom"), "ninguna", List.of("quarterstaff", "dagger", "spellbook"),
                "intelligence", "Erudito de la magia arcana."));
        register(map, new CharacterClass("rogue", "Pícaro", "d8", 4,
                List.of("acrobatics", "athletics", "deception", "insight", "intimidation", "investigation", "perception", "performance", "persuasion", "sleight_of_hand", "stealth"),
                List.of("dexterity", "intelligence"), "cuero", List.of("shortsword", "shortbow", "dagger"),
                "dexterity", "Experto en sigilo y precisión."));
        register(map, new CharacterClass("cleric", "Clérigo", "d8", 2,
                List.of("history", "insight", "medicine", "persuasion", "religion"),
                List.of("wisdom", "charisma"), "cadena", List.of("mace", "shield", "holy-symbol"),
                "wisdom", "Canalizador del poder divino."));
        register(map, new CharacterClass("barbarian", "Bárbaro", "d12", 2,
                List.of("animal_handling", "athletics", "intimidation", "nature", "perception", "survival"),
                List.of("strength", "constitution"), "ninguna", List.of("greataxe", "handaxe"),
                "strength", "Furia salvaje y resistencia brutal."));
        register(map, new CharacterClass("bard", "Bardo", "d8", 3,
                List.of("acrobatics", "animal_handling", "arcana", "athletics", "deception", "history", "insight", "intimidation", "investigation", "medicine", "nature", "perception", "performance", "persuasion", "religion", "sleight_of_hand", "stealth", "survival"),
                List.of("dexterity", "charisma"), "cuero", List.of("rapier", "dagger", "lute"),
                "charisma", "Artista y lanzador de conjuros versátil."));
        register(map, new CharacterClass("druid", "Druida", "d8", 2,
                List.of("arcana", "animal_handling", "insight", "medicine", "nature", "perception", "religion", "survival"),
                List.of("intelligence", "wisdom"), "cuero", List.of("scimitar", "shield", "druidic-focus"),
                "wisdom", "Guardián de la naturaleza."));
        register(map, new CharacterClass("monk", "Monje", "d8", 2,
                List.of("acrobatics", "athletics", "history", "insight", "religion", "stealth"),
                List.of("strength", "dexterity"), "ninguna", List.of("shortsword", "quarterstaff"),
                "dexterity", "Maestro del cuerpo y la disciplina."));
        register(map, new CharacterClass("paladin", "Paladín", "d10", 2,
                List.of("athletics", "insight", "intimidation", "medicine", "persuasion", "religion"),
                List.of("wisdom", "charisma"), "cadena", List.of("longsword", "shield", "holy-symbol"),
                "strength", "Guerrero sagrado de un juramento."));
        register(map, new CharacterClass("ranger", "Explorador", "d10", 3,
                List.of("animal_handling", "athletics", "insight", "investigation", "nature", "perception", "stealth", "survival"),
                List.of("strength", "dexterity"), "cuero", List.of("longbow", "shortsword"),
                "dexterity", "Cazador y rastreador de las tierras salvajes."));
        register(map, new CharacterClass("sorcerer", "Hechicero", "d6", 2,
                List.of("arcana", "deception", "insight", "intimidation", "persuasion", "religion"),
                List.of("constitution", "charisma"), "ninguna", List.of("dagger", "arcane-focus"),
                "charisma", "Magia innata en su sangre."));
        register(map, new CharacterClass("warlock", "Brujo", "d8", 2,
                List.of("arcana", "deception", "history", "intimidation", "investigation", "nature", "religion"),
                List.of("wisdom", "charisma"), "cuero", List.of("dagger", "arcane-focus"),
                "charisma", "Vinculado a un pacto sobrenatural."));
        return map;
    }

    private static Map<String, Background> backgrounds() {
        Map<String, Background> map = new LinkedHashMap<>();
        // 2024 backgrounds grant +2/+1 (or +1/+1/+1) across three abilities; encoded here.
        register(map, new Background("acolyte", "Acólito", List.of("insight", "religion"),
                Map.of("intelligence", 1, "wisdom", 2), "Sirviente de un templo; conoce ritos. +2 SAB, +1 INT y dote de origen."));
        register(map, new Background("criminal", "Criminal", List.of("sleight_of_hand", "stealth"),
                Map.of("dexterity", 2, "constitution", 1), "Vida al margen de la ley. +2 DES, +1 CON y dote de origen."));
        register(map, new Background("sage", "Sabio", List.of("arcana", "history"),
                Map.of("intelligence", 2, "wisdom", 1), "Erudito de bibliotecas. +2 INT, +1 SAB y dote de origen."));
        register(map, new Background("soldier", "Soldado", List.of("athletics", "intimidation"),
                Map.of("strength", 2, "constitution", 1), "Veterano de guerra. +2 FUE, +1 CON y dote de origen."));
        register(map, new Background("wayfarer", "Viajero", List.of("insight", "stealth"),
                Map.of("dexterity", 2, "wisdom", 1), "Caminante de los senderos. +2 DES, +1 SAB y dote de origen."));
        register(map, new Background("noble", "Noble", List.of("history", "persuasion"),
                Map.of("charisma", 2, "intelligence", 1), "Nacido en la corte. +2 CAR, +1 INT y dote de origen."));
        register(map, new Background("guide", "Guía", List.of("stealth", "survival"),
                Map.of("dexterity", 2, "wisdom", 1), "Conocedor de tierras salvajes. +2 DES, +1 SAB y dote de origen."));
        return map;
    }

    private static <T> void register(Map<String, T> map, T value) {
        String id = switch (value) {
            case Species s -> s.id();
            case CharacterClass c -> c.id();
            case Background b -> b.id();
            default -> throw new IllegalStateException("Unknown rule type");
        };
        map.put(RuleSet.normalize(id), value);
    }
}
