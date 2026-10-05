package com.dndmasters.rules;

import com.dndmasters.rules.RuleSet.Background;
import com.dndmasters.rules.RuleSet.CharacterClass;
import com.dndmasters.rules.RuleSet.Species;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** D&D 5e (2014) SRD reference data. */
public final class RuleSet2014 {

    private RuleSet2014() {
    }

    public static RuleSet build() {
        return new RuleSet(Edition.DND_5E_2014, species(), classes(), backgrounds());
    }

    private static Map<String, Species> species() {
        Map<String, Species> map = new LinkedHashMap<>();
        register(map, new Species("human", "Humano", 30, "Mediano",
                Map.of("strength", 1, "dexterity", 1, "constitution", 1, "intelligence", 1, "wisdom", 1, "charisma", 1),
                false, "Versátiles y ambiciosos; +1 a todas las características."));
        register(map, new Species("elf", "Elfo", 30, "Mediano",
                Map.of("dexterity", 2), true, "Gráciles y longevos; +2 DES, visión en la oscuridad."));
        register(map, new Species("high-elf", "Alto Elfo", 30, "Mediano",
                Map.of("dexterity", 2, "intelligence", 1), true, "Elfos instruidos en la magia; +2 DES, +1 INT."));
        register(map, new Species("dwarf", "Enano", 25, "Mediano",
                Map.of("constitution", 2), true, "Robustos y tercos; +2 CON, visión en la oscuridad."));
        register(map, new Species("halfling", "Mediano", 25, "Pequeño",
                Map.of("dexterity", 2), false, "Pequeños y afortunados; +2 DES."));
        register(map, new Species("half-elf", "Semielfo", 30, "Mediano",
                Map.of("charisma", 2, "dexterity", 1, "constitution", 1), true, "Carismáticos y versátiles; +2 CAR y +1 a dos más."));
        register(map, new Species("half-orc", "Semiorco", 30, "Mediano",
                Map.of("strength", 2, "constitution", 1), true, "Fuertes y tenaces; +2 FUE, +1 CON."));
        register(map, new Species("tiefling", "Tiefling", 30, "Mediano",
                Map.of("charisma", 2, "intelligence", 1), true, "Linaje infernal; +2 CAR, +1 INT, visión en la oscuridad."));
        return map;
    }

    private static Map<String, CharacterClass> classes() {
        Map<String, CharacterClass> map = new LinkedHashMap<>();
        register(map, new CharacterClass("fighter", "Guerrero", "d10", 2,
                List.of("acrobatics", "animal_handling", "athletics", "history", "insight", "intimidation", "perception", "survival"),
                List.of("strength", "constitution"), "cadena", List.of("longsword", "shield", "javelin"),
                "strength", "Maestro del combate con armas y armaduras."));
        register(map, new CharacterClass("wizard", "Mago", "d6", 2,
                List.of("arcana", "history", "insight", "investigation", "medicine", "religion"),
                List.of("intelligence", "wisdom"), "ninguna", List.of("quarterstaff", "dagger", "spellbook"),
                "intelligence", "Lanzador de conjuros arcanos."));
        register(map, new CharacterClass("rogue", "Pícaro", "d8", 4,
                List.of("acrobatics", "athletics", "deception", "insight", "intimidation", "investigation", "perception", "performance", "persuasion", "sleight_of_hand", "stealth"),
                List.of("dexterity", "intelligence"), "cuero", List.of("shortsword", "shortbow", "dagger"),
                "dexterity", "Especialista en sigilo, precisión y trucos."));
        register(map, new CharacterClass("cleric", "Clérigo", "d8", 2,
                List.of("history", "insight", "medicine", "persuasion", "religion"),
                List.of("wisdom", "charisma"), "cadena", List.of("warhammer", "shield", "holy-symbol"),
                "wisdom", "Canaliza el poder divino de su deidad."));
        register(map, new CharacterClass("barbarian", "Bárbaro", "d12", 2,
                List.of("animal_handling", "athletics", "intimidation", "nature", "perception", "survival"),
                List.of("strength", "constitution"), "ninguna", List.of("greataxe", "handaxe"),
                "strength", "Furia primigenia y aguante brutal."));
        register(map, new CharacterClass("bard", "Bardo", "d8", 3,
                List.of("acrobatics", "animal_handling", "arcana", "athletics", "deception", "history", "insight", "intimidation", "investigation", "medicine", "nature", "perception", "performance", "persuasion", "religion", "sleight_of_hand", "stealth", "survival"),
                List.of("dexterity", "charisma"), "cuero", List.of("rapier", "dagger", "lute"),
                "charisma", "Artista versátil que inspira y lanza conjuros."));
        register(map, new CharacterClass("druid", "Druida", "d8", 2,
                List.of("arcana", "animal_handling", "insight", "medicine", "nature", "perception", "religion", "survival"),
                List.of("intelligence", "wisdom"), "cuero", List.of("scimitar", "shield", "druidic-focus"),
                "wisdom", "Guardiana del equilibrio natural y la magia salvaje."));
        register(map, new CharacterClass("monk", "Monje", "d8", 2,
                List.of("acrobatics", "athletics", "history", "insight", "religion", "stealth"),
                List.of("strength", "dexterity"), "ninguna", List.of("shortsword", "quarterstaff"),
                "dexterity", "Maestro del cuerpo y el ki."));
        register(map, new CharacterClass("paladin", "Paladín", "d10", 2,
                List.of("athletics", "insight", "intimidation", "medicine", "persuasion", "religion"),
                List.of("wisdom", "charisma"), "cadena", List.of("longsword", "shield", "holy-symbol"),
                "strength", "Guerrero sagrado ligado a un juramento."));
        register(map, new CharacterClass("ranger", "Explorador", "d10", 3,
                List.of("animal_handling", "athletics", "insight", "investigation", "nature", "perception", "stealth", "survival"),
                List.of("strength", "dexterity"), "cuero", List.of("longbow", "shortsword"),
                "dexterity", "Cazador de las tierras salvajes."));
        register(map, new CharacterClass("sorcerer", "Hechicero", "d6", 2,
                List.of("arcana", "deception", "insight", "intimidation", "persuasion", "religion"),
                List.of("constitution", "charisma"), "ninguna", List.of("dagger", "arcane-focus"),
                "charisma", "Magia innata que fluye en su sangre."));
        register(map, new CharacterClass("warlock", "Brujo", "d8", 2,
                List.of("arcana", "deception", "history", "intimidation", "investigation", "nature", "religion"),
                List.of("wisdom", "charisma"), "cuero", List.of("dagger", "arcane-focus"),
                "charisma", "Poder concedido por un pacto sobrenatural."));
        return map;
    }

    private static Map<String, Background> backgrounds() {
        Map<String, Background> map = new LinkedHashMap<>();
        register(map, new Background("acolyte", "Acólito", List.of("insight", "religion"), Map.of(),
                "Serviste en un templo. Conoces ritos y jerarquías religiosas."));
        register(map, new Background("criminal", "Criminal", List.of("deception", "stealth"), Map.of(),
                "Vives al margen de la ley y conoces el hampa."));
        register(map, new Background("folk-hero", "Héroe Popular", List.of("animal_handling", "survival"), Map.of(),
                "El pueblo te considera un símbolo de esperanza."));
        register(map, new Background("noble", "Noble", List.of("history", "persuasion"), Map.of(),
                "Nacido entre el poder; conoces las cortes y sus intrigas."));
        register(map, new Background("sage", "Sabio", List.of("arcana", "history"), Map.of(),
                "Dedicaste tu vida al estudio y los secretos del mundo."));
        register(map, new Background("soldier", "Soldado", List.of("athletics", "intimidation"), Map.of(),
                "Serviste en un ejército; conoces la disciplina y la guerra."));
        register(map, new Background("urchin", "Golfillo", List.of("sleight_of_hand", "stealth"), Map.of(),
                "Creciste en las calles; sabes sobrevivir y moverte sin ser visto."));
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
