package com.dndmasters.engine.character;

import com.dndmasters.rules.Edition;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Derives display-only languages and armor/weapon/tool proficiencies from the chosen
 * species, class and background. The values are stored in
 * {@code PlayerCharacter.creationData} so the character sheet can show them without a
 * database migration; combat mechanics still use their dedicated rules.
 *
 * <p>2014 data follows the Player's Handbook. 2024 uses the origin rule that grants
 * Common plus two languages of choice, plus the class training from that edition.</p>
 */
public final class CharacterProficiencies {

    public record Proficiencies(
            List<String> languages,
            List<String> armor,
            List<String> weapons,
            List<String> tools) {

        public Proficiencies {
            languages = List.copyOf(languages);
            armor = List.copyOf(armor);
            weapons = List.copyOf(weapons);
            tools = List.copyOf(tools);
        }
    }

    private record ClassTraining(List<String> armor, List<String> weapons, List<String> tools) {
    }

    private record BackgroundTraining(List<String> tools, List<String> languages) {
    }

    private static final String SPECIES_EXTRA_LANGUAGE = "Un idioma adicional de especie (a elección)";
    private static final String BACKGROUND_ONE_LANGUAGE = "Un idioma adicional de trasfondo (a elección)";
    private static final String BACKGROUND_TWO_LANGUAGES = "Dos idiomas adicionales de trasfondo (a elección)";
    private static final String ORIGIN_TWO_LANGUAGES = "Dos idiomas adicionales (a elección)";

    private static final List<String> NONE = List.of();
    private static final List<String> ALL_ARMOR = List.of("Todas las armaduras", "Escudos");
    private static final List<String> LIGHT_ARMOR = List.of("Armadura ligera");
    private static final List<String> LIGHT_MEDIUM_SHIELD =
            List.of("Armadura ligera", "Armadura media", "Escudos");
    private static final List<String> SIMPLE = List.of("Armas simples");
    private static final List<String> SIMPLE_AND_MARTIAL = List.of("Armas simples", "Armas marciales");

    private static final ClassTraining NO_CLASS = new ClassTraining(NONE, NONE, NONE);
    private static final BackgroundTraining NO_BACKGROUND = new BackgroundTraining(NONE, NONE);

    private static final Map<String, List<String>> SPECIES_LANGUAGES_2014 = Map.ofEntries(
            language("human", "Común", SPECIES_EXTRA_LANGUAGE),
            language("elf", "Común", "Élfico"),
            language("high-elf", "Común", "Élfico", SPECIES_EXTRA_LANGUAGE),
            language("dwarf", "Común", "Enano"),
            language("halfling", "Común", "Mediano"),
            language("half-elf", "Común", "Élfico", SPECIES_EXTRA_LANGUAGE),
            language("half-orc", "Común", "Orco"),
            language("tiefling", "Común", "Infernal"),
            language("dragonborn", "Común", "Dracónico"),
            language("gnome", "Común", "Gnómico"),
            language("forest-gnome", "Común", "Gnómico"),
            language("rock-gnome", "Común", "Gnómico"),
            language("wood-elf", "Común", "Élfico"),
            language("drow", "Común", "Élfico"),
            language("hill-dwarf", "Común", "Enano"),
            language("mountain-dwarf", "Común", "Enano"),
            language("lightfoot-halfling", "Común", "Mediano"),
            language("stout-halfling", "Común", "Mediano"));

    private static final Map<String, ClassTraining> CLASS_TRAINING_2014 = Map.ofEntries(
            trainingEntry("fighter", ALL_ARMOR, SIMPLE_AND_MARTIAL, NONE),
            trainingEntry("wizard", NONE,
                    List.of("Bastones", "Ballestas ligeras", "Dagas", "Dardos", "Hondas"), NONE),
            trainingEntry("rogue", LIGHT_ARMOR,
                    List.of("Armas simples", "Ballesta de mano", "Espada larga", "Espada ropera", "Espada corta"),
                    List.of("Herramientas de ladrón")),
            trainingEntry("cleric", LIGHT_MEDIUM_SHIELD, SIMPLE, NONE),
            trainingEntry("barbarian", LIGHT_MEDIUM_SHIELD, SIMPLE_AND_MARTIAL, NONE),
            trainingEntry("bard", LIGHT_ARMOR,
                    List.of("Armas simples", "Ballesta de mano", "Espada larga", "Espada ropera", "Espada corta"),
                    List.of("Tres instrumentos musicales (a elección)")),
            trainingEntry("druid", List.of("Armadura ligera", "Armadura media", "Escudos (no de metal)"),
                    List.of("Bastones", "Cimitarras", "Clavas", "Dagas", "Dardos", "Hoces", "Hondas", "Lanzas", "Mazas"),
                    List.of("Kit de herboristería")),
            trainingEntry("monk", NONE, List.of("Armas simples", "Espadas cortas"),
                    List.of("Una herramienta de artesano o un instrumento musical (a elección)")),
            trainingEntry("paladin", ALL_ARMOR, SIMPLE_AND_MARTIAL, NONE),
            trainingEntry("ranger", LIGHT_MEDIUM_SHIELD, SIMPLE_AND_MARTIAL, NONE),
            trainingEntry("sorcerer", NONE, List.of("Dagas", "Dardos", "Hondas", "Bastones", "Ballestas ligeras"), NONE),
            trainingEntry("warlock", LIGHT_ARMOR, SIMPLE, NONE));

    private static final Map<String, ClassTraining> CLASS_TRAINING_2024 = Map.ofEntries(
            trainingEntry("fighter", ALL_ARMOR, SIMPLE_AND_MARTIAL, NONE),
            trainingEntry("wizard", NONE, SIMPLE, NONE),
            trainingEntry("rogue", LIGHT_ARMOR,
                    List.of("Armas simples", "Armas marciales con las propiedades ágil o ligera"),
                    List.of("Herramientas de ladrón")),
            trainingEntry("cleric", LIGHT_MEDIUM_SHIELD, SIMPLE, NONE),
            trainingEntry("barbarian", LIGHT_MEDIUM_SHIELD, SIMPLE_AND_MARTIAL, NONE),
            trainingEntry("bard", LIGHT_ARMOR, SIMPLE, List.of("Tres instrumentos musicales (a elección)")),
            trainingEntry("druid", LIGHT_MEDIUM_SHIELD, SIMPLE, List.of("Kit de herboristería")),
            trainingEntry("monk", NONE, List.of("Armas simples", "Armas marciales con la propiedad ligera"),
                    List.of("Una herramienta de artesano o un instrumento musical (a elección)")),
            trainingEntry("paladin", ALL_ARMOR, SIMPLE_AND_MARTIAL, NONE),
            trainingEntry("ranger", LIGHT_MEDIUM_SHIELD, SIMPLE_AND_MARTIAL, NONE),
            trainingEntry("sorcerer", NONE, SIMPLE, NONE),
            trainingEntry("warlock", LIGHT_ARMOR, SIMPLE, NONE));

    private static final Map<String, BackgroundTraining> BACKGROUND_TRAINING_2014 = Map.ofEntries(
            backgroundEntry("acolyte", NONE, List.of(BACKGROUND_TWO_LANGUAGES)),
            backgroundEntry("criminal", List.of("Set de juego (dados de hueso o cartas)", "Herramientas de ladrón"), NONE),
            backgroundEntry("folk-hero",
                    List.of("Un tipo de herramientas de artesano (a elección)", "Vehículos (terrestres)"), NONE),
            backgroundEntry("noble", List.of("Set de juego (a elección)"), List.of(BACKGROUND_ONE_LANGUAGE)),
            backgroundEntry("sage", NONE, List.of(BACKGROUND_TWO_LANGUAGES)),
            backgroundEntry("soldier", List.of("Set de juego (a elección)", "Vehículos (terrestres)"), NONE),
            backgroundEntry("urchin", List.of("Kit de disfraz", "Herramientas de ladrón"), NONE),
            backgroundEntry("charlatan", List.of("Kit de disfraz", "Kit de falsificación"), NONE),
            backgroundEntry("entertainer",
                    List.of("Kit de disfraz", "Un tipo de instrumento musical (a elección)"), NONE),
            backgroundEntry("guild-artisan",
                    List.of("Un tipo de herramientas de artesano (a elección)"), List.of(BACKGROUND_ONE_LANGUAGE)),
            backgroundEntry("hermit", List.of("Kit de herboristería"), List.of(BACKGROUND_ONE_LANGUAGE)),
            backgroundEntry("outlander",
                    List.of("Un tipo de instrumento musical (a elección)"), List.of(BACKGROUND_ONE_LANGUAGE)),
            backgroundEntry("sailor", List.of("Herramientas de navegación", "Vehículos (acuáticos)"), NONE));

    private static final Map<String, BackgroundTraining> BACKGROUND_TRAINING_2024 = Map.ofEntries(
            backgroundEntry("acolyte", List.of("Herramientas de calígrafo"), NONE),
            backgroundEntry("criminal", List.of("Herramientas de ladrón"), NONE),
            backgroundEntry("sage", List.of("Herramientas de calígrafo"), NONE),
            backgroundEntry("soldier", List.of("Set de juego (a elección)"), NONE),
            backgroundEntry("wayfarer", List.of("Herramientas de ladrón"), NONE),
            backgroundEntry("noble", List.of("Set de juego (a elección)"), NONE),
            backgroundEntry("guide", List.of("Herramientas de cartógrafo"), NONE));

    private CharacterProficiencies() {
    }

    /** Derives the display lists; unknown ids produce empty lists instead of failing. */
    public static Proficiencies derive(Edition edition, String speciesId, String classId, String backgroundId) {
        boolean modern = edition == Edition.DND_5E_2024;
        List<String> languages = new ArrayList<>(modern
                ? List.of("Común", ORIGIN_TWO_LANGUAGES)
                : SPECIES_LANGUAGES_2014.getOrDefault(normalize(speciesId), NONE));

        BackgroundTraining background = (modern ? BACKGROUND_TRAINING_2024 : BACKGROUND_TRAINING_2014)
                .getOrDefault(normalize(backgroundId), NO_BACKGROUND);
        ClassTraining training = (modern ? CLASS_TRAINING_2024 : CLASS_TRAINING_2014)
                .getOrDefault(normalize(classId), NO_CLASS);

        languages.addAll(background.languages());
        return new Proficiencies(
                dedupe(languages),
                training.armor(),
                training.weapons(),
                merge(training.tools(), background.tools()));
    }

    private static Map.Entry<String, List<String>> language(String id, String... values) {
        return Map.entry(id, List.of(values));
    }

    private static Map.Entry<String, ClassTraining> trainingEntry(
            String id, List<String> armor, List<String> weapons, List<String> tools) {
        return Map.entry(id, new ClassTraining(armor, weapons, tools));
    }

    private static Map.Entry<String, BackgroundTraining> backgroundEntry(
            String id, List<String> tools, List<String> languages) {
        return Map.entry(id, new BackgroundTraining(tools, languages));
    }

    private static List<String> merge(List<String> first, List<String> second) {
        List<String> all = new ArrayList<>(first);
        all.addAll(second);
        return dedupe(all);
    }

    private static List<String> dedupe(List<String> values) {
        return List.copyOf(new LinkedHashSet<>(values));
    }

    private static String normalize(String id) {
        return id == null ? "" : id.trim().toLowerCase(Locale.ROOT).replace(" ", "-").replace("_", "-");
    }
}
