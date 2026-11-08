package com.dndmasters.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.dndmasters.util.Json;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PlayerCharacterTest {

    private PlayerCharacter character(int strength, List<InventoryItem> inventory) {
        PlayerCharacter character = new PlayerCharacter();
        character.setName("Kael");
        character.setStats(new Stats(strength, 12, 14, 10, 10, 8));
        character.setInventory(inventory);
        return character;
    }

    @Test
    void derivesCarriedWeightAndCapacityFromStats() {
        PlayerCharacter character = character(10, List.of(
                new InventoryItem("Espada larga", "", "sword", 1, 3),
                new InventoryItem("Flechas", "", "crosshair", 20, 0.05)));

        assertThat(character.totalCarriedWeight()).isEqualTo(4.0);
        assertThat(character.carryingCapacity()).isEqualTo(150);
        assertThat(character.isEncumbered()).isFalse();
    }

    @Test
    void carryingCapacityIsFifteenPoundsPerStrengthPoint() {
        assertThat(character(10, List.of()).carryingCapacity()).isEqualTo(150);
        assertThat(character(16, List.of()).carryingCapacity()).isEqualTo(240);
        assertThat(character(0, List.of()).carryingCapacity()).isZero();
    }

    @Test
    void inventoryTotalMultipliesUnitWeightByQuantity() {
        PlayerCharacter character = character(10, List.of(
                new InventoryItem("Espada larga", "", "sword", 1, 3),
                new InventoryItem("Flechas", "", "crosshair", 20, 0.05)));

        assertThat(character.totalCarriedWeight()).isEqualTo(4.0);
    }

    @Test
    void emptyInventoryWeighsNothing() {
        assertThat(character(10, List.of()).totalCarriedWeight()).isZero();
    }

    @Test
    void encumberedOnlyWhenWeightExceedsCapacity() {
        assertThat(character(10, List.of(
                new InventoryItem("Peso exacto", "", "package", 1, 150))).isEncumbered()).isFalse();
        assertThat(character(10, List.of(
                new InventoryItem("Peso excesivo", "", "package", 1, 150.1))).isEncumbered()).isTrue();
    }

    @Test
    void encumberedWhenCarriedWeightExceedsStrengthTimesFifteen() {
        PlayerCharacter character = character(10, List.of(
                new InventoryItem("Armadura de placas", "", "shield", 1, 65),
                new InventoryItem("Cota de mallas", "", "shield", 1, 55),
                new InventoryItem("Piedras", "", "package", 1, 31)));

        assertThat(character.totalCarriedWeight()).isEqualTo(151);
        assertThat(character.isEncumbered()).isTrue();
    }

    @Test
    void missingStatsOrInventoryDoNotBreakDerivedValues() {
        PlayerCharacter character = new PlayerCharacter();

        assertThat(character.totalCarriedWeight()).isZero();
        assertThat(character.carryingCapacity()).isZero();
        assertThat(character.isEncumbered()).isFalse();
    }

    @Test
    void serializedCharacterExposesDerivedWeightFields() {
        PlayerCharacter character = character(10, List.of(
                new InventoryItem("Espada larga", "", "sword", 1, 3)));

        Map<String, Object> json = Json.readMap(Json.write(character));

        assertThat(json).containsEntry("carriedWeight", 3.0);
        assertThat(json).containsEntry("carryingCapacity", 150);
        assertThat(json).containsEntry("encumbered", false);
    }
}
