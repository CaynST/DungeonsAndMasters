package com.dndmasters.engine.spells;

import static org.assertj.core.api.Assertions.assertThat;

import com.dndmasters.domain.InventoryItem;
import com.dndmasters.domain.PlayerCharacter;
import java.util.List;
import org.junit.jupiter.api.Test;

class SpellComponentRulesTest {

    private static PlayerCharacter character(String... items) {
        PlayerCharacter character = new PlayerCharacter();
        character.setInventory(java.util.Arrays.stream(items)
                .map(item -> InventoryItem.of(item, ""))
                .toList());
        return character;
    }

    @Test
    void verbalAndSomaticSpellsDoNotNeedAFocus() {
        Spell fireBolt = SpellCatalog.byId("wizard", "fire_bolt").orElseThrow();

        assertThat(SpellComponentRules.requiresMaterial(fireBolt)).isFalse();
        assertThat(SpellComponentRules.canCast(fireBolt, false)).isTrue();
    }

    @Test
    void materialSpellRequiresAFocus() {
        Spell bless = SpellCatalog.byId("cleric", "bless").orElseThrow();

        assertThat(SpellComponentRules.requiresMaterial(bless)).isTrue();
        assertThat(SpellComponentRules.canCast(bless, false)).isFalse();
        assertThat(SpellComponentRules.canCast(bless, true)).isTrue();
    }

    @Test
    void focusDetectionLooksAtInventoryNames() {
        assertThat(SpellComponentRules.hasFocus(character("Símbolo sagrado"))).isTrue();
        assertThat(SpellComponentRules.hasFocus(character("Foco arcano"))).isTrue();
        assertThat(SpellComponentRules.hasFocus(character("Componentes"))).isTrue();
        assertThat(SpellComponentRules.hasFocus(character("Raciones"))).isFalse();
        assertThat(SpellComponentRules.hasFocus(null)).isFalse();
    }
}
