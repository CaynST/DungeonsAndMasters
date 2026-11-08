package com.dndmasters.engine.rules;

import com.dndmasters.domain.Attack;
import com.dndmasters.domain.InventoryItem;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * R-18 ammunition consumption for ranged weapon attacks.
 *
 * <p>The prototype does not model a generic item taxonomy, so the mapping is deliberately small
 * and explicit: bows (including test/custom names containing "arco" or "bow") consume
 * {@value #ARROWS}; crossbows consume {@value #BOLTS}. Thrown weapons (javelins, daggers) are not
 * tracked and consume nothing, matching {@code CharacterBuilder}'s starting-equipment rule.
 * Names that only look ranged (spell attacks such as "Fuego feérico" or "Llama sagrada") also
 * consume nothing; spell attacks are resolved through a separate engine path with
 * {@code weaponAttack=false}.</p>
 */
public final class AmmunitionRules {

    public static final String ARROWS = "Flechas";
    public static final String BOLTS = "Virotes";

    private static final Map<String, List<String>> ITEM_ALIASES = Map.of(
            ARROWS.toLowerCase(Locale.ROOT),
            List.of("flechas", "flecha", "arrows", "arrow"),
            BOLTS.toLowerCase(Locale.ROOT),
            List.of("virotes", "virote", "bolts", "bolt"));

    private AmmunitionRules() {
    }

    /** The ammunition item required by a ranged weapon attack, or empty when none is tracked. */
    public static Optional<String> ammunitionFor(Attack attack) {
        if (attack == null || !attack.ranged()) {
            return Optional.empty();
        }
        String normalized = normalize(attack.name());
        // "crossbow" contains "bow", so crossbows must be checked first.
        if (normalized.contains("ballesta") || normalized.contains("crossbow")) {
            return Optional.of(BOLTS);
        }
        if (normalized.contains("arco") || normalized.contains("bow")) {
            return Optional.of(ARROWS);
        }
        return Optional.empty();
    }

    /** Index of the matching ammunition line in the character inventory, if any. */
    public static OptionalInt findAmmunitionSlot(List<InventoryItem> inventory, String ammunition) {
        if (inventory == null || ammunition == null) {
            return OptionalInt.empty();
        }
        for (int index = 0; index < inventory.size(); index++) {
            InventoryItem item = inventory.get(index);
            if (item != null && matchesAmmunitionItem(item.name(), ammunition)) {
                return OptionalInt.of(index);
            }
        }
        return OptionalInt.empty();
    }

    /** Case/accent-insensitive comparison with the explicit Spanish and English item names. */
    public static boolean matchesAmmunitionItem(String itemName, String ammunition) {
        if (itemName == null || ammunition == null) {
            return false;
        }
        List<String> aliases = ITEM_ALIASES.get(normalize(ammunition));
        return aliases != null && aliases.contains(normalize(itemName));
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }
}
