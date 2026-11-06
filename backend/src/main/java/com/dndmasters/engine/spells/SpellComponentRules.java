package com.dndmasters.engine.spells;

import com.dndmasters.domain.InventoryItem;
import com.dndmasters.domain.PlayerCharacter;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

/**
 * Component checks for spells. The prototype enforces material components through a focus or
 * component pouch; verbal/somatic requirements are descriptive until silence/free-hand rules exist.
 */
public final class SpellComponentRules {

    private SpellComponentRules() {
    }

    public static boolean requiresMaterial(Spell spell) {
        if (spell.components() == null) {
            return false;
        }
        return Arrays.stream(spell.components().split(","))
                .map(String::trim)
                .anyMatch("M"::equalsIgnoreCase);
    }

    public static boolean canCast(Spell spell, boolean hasFocus) {
        return !requiresMaterial(spell) || hasFocus;
    }

    /** A focus, component pouch, spellbook or instrument covers material components. */
    public static boolean hasFocus(PlayerCharacter character) {
        if (character == null || character.getInventory() == null) {
            return false;
        }
        return character.getInventory().stream()
                .map(InventoryItem::name)
                .filter(Objects::nonNull)
                .map(name -> name.toLowerCase(Locale.ROOT))
                .anyMatch(name -> name.contains("componentes")
                        || name.contains("foco")
                        || name.contains("símbolo")
                        || name.contains("simbolo")
                        || name.contains("grimorio")
                        || name.contains("instrumento"));
    }
}
