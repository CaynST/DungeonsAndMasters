package com.dndmasters.engine.spells;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SpellCatalogTest {

    @Test
    void wizardGetsCantripsAndFirstLevelSpells() {
        Spellbook book = SpellCatalog.forClass("wizard");

        assertThat(book.slots()).isEqualTo(2);
        assertThat(book.spells()).extracting(Spell::id)
                .contains("fire_bolt", "ray_of_frost", "magic_missile");
        assertThat(book.spells()).anyMatch(spell -> spell.level() == 0);
    }

    @Test
    void clericGetsSacredFlameAndHealingSpells() {
        Spellbook book = SpellCatalog.forClass("cleric");

        assertThat(book.slots()).isEqualTo(2);
        assertThat(book.spells()).extracting(Spell::id)
                .contains("sacred_flame", "cure_wounds", "healing_word");
        assertThat(book.spells()).anyMatch(spell -> spell.bonusAction());
    }

    @Test
    void nonCastersHaveNoSpellbook() {
        assertThat(SpellCatalog.forClass("fighter").spells()).isEmpty();
        assertThat(SpellCatalog.forClass("fighter").slots()).isZero();
        assertThat(SpellCatalog.forClass(null).spells()).isEmpty();
    }

    @Test
    void allSpellsHaveUniqueIdsAndCompleteMetadata() {
        Set<String> ids = new HashSet<>();
        for (Spellbook book : SpellCatalog.spellbooks().values()) {
            for (Spell spell : book.spells()) {
                assertThat(ids.add(spell.id())).as("duplicate id " + spell.id()).isTrue();
                assertThat(spell.name()).isNotBlank();
                assertThat(spell.description()).isNotBlank();
                assertThat(spell.range()).isPositive();
                if (spell.level() == 0) {
                    assertThat(spell.damage()).isNotNull();
                }
            }
        }
    }

    @Test
    void spellbooksExposeTheStaticCatalog() {
        assertThat(SpellCatalog.spellbooks()).containsKeys("wizard", "cleric");
        assertThat(SpellCatalog.spellbooks().get("wizard").spells()).isNotEmpty();
        assertThat(List.of("fire_bolt", "magic_missile")).allMatch(id ->
                SpellCatalog.spellbooks().get("wizard").spells().stream()
                        .anyMatch(spell -> spell.id().equals(id)));
    }
}
