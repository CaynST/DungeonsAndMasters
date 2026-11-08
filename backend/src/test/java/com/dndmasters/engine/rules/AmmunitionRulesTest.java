package com.dndmasters.engine.rules;

import static org.assertj.core.api.Assertions.assertThat;

import com.dndmasters.domain.Attack;
import com.dndmasters.domain.InventoryItem;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Explicit R-18 mapping: bows use arrows, crossbows use bolts, thrown/spell attacks use nothing. */
class AmmunitionRulesTest {

    @Test
    void bowsConsumeArrows() {
        assertThat(AmmunitionRules.ammunitionFor(Attack.ranged("Arco corto", 5, "1d6+3", 16)))
                .contains(AmmunitionRules.ARROWS);
        assertThat(AmmunitionRules.ammunitionFor(Attack.ranged("Arco largo", 5, "1d8+3", 24)))
                .contains(AmmunitionRules.ARROWS);
        assertThat(AmmunitionRules.ammunitionFor(Attack.ranged("Shortbow", 5, "1d6", 16)))
                .contains(AmmunitionRules.ARROWS);
    }

    @Test
    void crossbowsConsumeBoltsEvenThoughTheEnglishNameContainsBow() {
        assertThat(AmmunitionRules.ammunitionFor(Attack.ranged("Ballesta ligera", 4, "1d8", 16)))
                .contains(AmmunitionRules.BOLTS);
        assertThat(AmmunitionRules.ammunitionFor(Attack.ranged("Light crossbow", 4, "1d8", 16)))
                .contains(AmmunitionRules.BOLTS);
    }

    @Test
    void thrownWeaponsUseNoTrackedAmmunition() {
        assertThat(AmmunitionRules.ammunitionFor(Attack.ranged("Jabalina", 4, "1d6+2", 8))).isEmpty();
        assertThat(AmmunitionRules.ammunitionFor(Attack.ranged("Daga", 4, "1d4", 4))).isEmpty();
    }

    @Test
    void meleeAndSpellLikeAttacksUseNoAmmunition() {
        assertThat(AmmunitionRules.ammunitionFor(Attack.melee("Longsword", 5, "1d8+3"))).isEmpty();
        assertThat(AmmunitionRules.ammunitionFor(Attack.ranged("Fuego feérico", 5, "1d10", 24))).isEmpty();
        assertThat(AmmunitionRules.ammunitionFor(Attack.ranged("Llama sagrada", 4, "1d8", 12))).isEmpty();
    }

    @Test
    void findsTheMatchingAmmunitionLineCaseAndAccentInsensitively() {
        List<InventoryItem> inventory = List.of(
                InventoryItem.of("Poción de Curación", "Cura.", "flask", 1, 0.5),
                InventoryItem.of("Flechas", "Munición.", "crosshair", 20, 0.05));

        int slot = AmmunitionRules.findAmmunitionSlot(inventory, AmmunitionRules.ARROWS).orElse(-1);

        assertThat(slot).isEqualTo(1);
        assertThat(AmmunitionRules.matchesAmmunitionItem("flecha", AmmunitionRules.ARROWS)).isTrue();
        assertThat(AmmunitionRules.findAmmunitionSlot(inventory, AmmunitionRules.BOLTS)).isEmpty();
    }
}
