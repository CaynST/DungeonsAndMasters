package com.dndmasters.content;

import static org.assertj.core.api.Assertions.assertThat;

import com.dndmasters.domain.InventoryItem;
import com.dndmasters.domain.PlayerCharacter;
import org.junit.jupiter.api.Test;

class PredefinedCharactersTest {

    @Test
    void ariaCarriesArrowsForHerShortbow() {
        PlayerCharacter aria = PredefinedCharacters.aria();

        assertThat(aria.getInventory()).anySatisfy(item -> {
            assertThat(item.name()).isEqualTo("Flechas");
            assertThat(item.quantity()).isEqualTo(20);
            assertThat(item.weight()).isEqualTo(0.05);
        });
    }

    @Test
    void everyPredefinedHeroHasWeightedGearAndCapacity() {
        for (PlayerCharacter character : PredefinedCharacters.all()) {
            assertThat(character.getInventory()).isNotEmpty();
            assertThat(character.getInventory())
                    .allSatisfy(item -> assertThat(item.weight()).isNotNegative());
            assertThat(character.totalCarriedWeight()).isPositive();
            assertThat(character.carryingCapacity())
                    .isEqualTo(character.getStats().strength() * 15);
        }
    }

    @Test
    void inventoryItemsKeepTheirQuantityWhenWeightIsAdded() {
        PlayerCharacter kael = PredefinedCharacters.kael();

        InventoryItem javelins = kael.getInventory().stream()
                .filter(item -> item.name().equals("Jabalinas"))
                .findFirst()
                .orElseThrow();
        assertThat(javelins.quantity()).isEqualTo(3);
        assertThat(javelins.weight()).isEqualTo(2.0);
    }
}
