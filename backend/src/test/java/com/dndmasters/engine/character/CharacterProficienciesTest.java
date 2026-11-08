package com.dndmasters.engine.character;

import static org.assertj.core.api.Assertions.assertThat;

import com.dndmasters.rules.Edition;
import com.dndmasters.rules.RuleSet;
import com.dndmasters.rules.RuleSetRegistry;
import java.util.List;
import org.junit.jupiter.api.Test;

class CharacterProficienciesTest {

    @Test
    void derivesSpeciesLanguagesAndClassTrainingIn2014() {
        CharacterProficiencies.Proficiencies proficiencies = CharacterProficiencies.derive(
                Edition.DND_5E_2014, "high-elf", "wizard", "sage");

        assertThat(proficiencies.languages())
                .contains("Común", "Élfico", "Un idioma adicional de especie (a elección)")
                .contains("Dos idiomas adicionales de trasfondo (a elección)");
        assertThat(proficiencies.armor()).isEmpty();
        assertThat(proficiencies.weapons()).contains("Bastones", "Ballestas ligeras", "Dagas", "Dardos", "Hondas");
        assertThat(proficiencies.tools()).isEmpty();
    }

    @Test
    void combinesBackgroundToolsAndLanguagesWithClassProficiencies() {
        CharacterProficiencies.Proficiencies proficiencies = CharacterProficiencies.derive(
                Edition.DND_5E_2014, "dwarf", "fighter", "soldier");

        assertThat(proficiencies.languages()).containsExactly("Común", "Enano");
        assertThat(proficiencies.armor()).containsExactly("Todas las armaduras", "Escudos");
        assertThat(proficiencies.weapons()).containsExactly("Armas simples", "Armas marciales");
        assertThat(proficiencies.tools())
                .containsExactly("Set de juego (a elección)", "Vehículos (terrestres)");
    }

    @Test
    void derivesToolsFromTheSixNew2014Backgrounds() {
        assertThat(tools("charlatan")).containsExactly("Kit de disfraz", "Kit de falsificación");
        assertThat(tools("entertainer")).containsExactly("Kit de disfraz", "Un tipo de instrumento musical (a elección)");
        assertThat(tools("guild-artisan")).containsExactly("Un tipo de herramientas de artesano (a elección)");
        assertThat(tools("hermit")).containsExactly("Kit de herboristería");
        assertThat(tools("outlander")).containsExactly("Un tipo de instrumento musical (a elección)");
        assertThat(tools("sailor")).containsExactly("Herramientas de navegación", "Vehículos (acuáticos)");
    }

    @Test
    void twoThousandTwentyFourUsesTheOriginLanguageRuleAndClassTraining() {
        CharacterProficiencies.Proficiencies proficiencies = CharacterProficiencies.derive(
                Edition.DND_5E_2024, "human", "rogue", "criminal");

        assertThat(proficiencies.languages()).containsExactly("Común", "Dos idiomas adicionales (a elección)");
        assertThat(proficiencies.armor()).containsExactly("Armadura ligera");
        assertThat(proficiencies.weapons()).contains("Armas simples");
        assertThat(proficiencies.tools()).containsExactly("Herramientas de ladrón");
    }

    @Test
    void everyTwoThousandFourteenOriginHasDerivedData() {
        RuleSet set = new RuleSetRegistry().get(Edition.DND_5E_2014);

        set.species().values().forEach(species -> assertThat(
                CharacterProficiencies.derive(Edition.DND_5E_2014, species.id(), "fighter", "soldier").languages())
                .as("languages of " + species.id())
                .isNotEmpty());

        set.classes().values().forEach(characterClass -> {
            CharacterProficiencies.Proficiencies proficiencies = CharacterProficiencies.derive(
                    Edition.DND_5E_2014, "human", characterClass.id(), "soldier");
            assertThat(proficiencies.armor().isEmpty() && proficiencies.weapons().isEmpty()
                    && proficiencies.tools().isEmpty())
                    .as("training of " + characterClass.id())
                    .isFalse();
        });

        set.backgrounds().values().forEach(background -> {
            CharacterProficiencies.Proficiencies proficiencies = CharacterProficiencies.derive(
                    Edition.DND_5E_2014, "human", "fighter", background.id());
            assertThat(proficiencies.tools().isEmpty() && proficiencies.languages().isEmpty())
                    .as("tools or languages of " + background.id())
                    .isFalse();
        });
    }

    @Test
    void unknownIdsYieldEmptyListsInsteadOfFailing() {
        CharacterProficiencies.Proficiencies proficiencies = CharacterProficiencies.derive(
                Edition.DND_5E_2014, "unknown-species", "unknown-class", "unknown-background");

        assertThat(proficiencies.languages()).isEmpty();
        assertThat(proficiencies.armor()).isEmpty();
        assertThat(proficiencies.weapons()).isEmpty();
        assertThat(proficiencies.tools()).isEmpty();
    }

    private List<String> tools(String backgroundId) {
        return CharacterProficiencies.derive(Edition.DND_5E_2014, "human", "fighter", backgroundId).tools();
    }
}
