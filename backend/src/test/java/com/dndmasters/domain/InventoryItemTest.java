package com.dndmasters.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.dndmasters.util.Json;
import java.util.List;
import org.junit.jupiter.api.Test;

class InventoryItemTest {

    @Test
    void legacyJsonWithoutWeightLoadsAsZero() {
        String legacy = """
                [{"name":"Cuerda","description":"Cuerda de cáñamo.","icon":"key","quantity":2}]
                """;

        List<InventoryItem> items = Json.readList(legacy, InventoryItem.class);

        assertThat(items).hasSize(1);
        assertThat(items.get(0).name()).isEqualTo("Cuerda");
        assertThat(items.get(0).quantity()).isEqualTo(2);
        assertThat(items.get(0).weight()).isZero();
    }

    @Test
    void fourArgumentConstructorDefaultsWeightToZero() {
        InventoryItem item = new InventoryItem("Antorcha", "Ilumina.", "flame", 3);

        assertThat(item.weight()).isZero();
    }

    @Test
    void roundTripsWeightAndComputesLineWeight() {
        InventoryItem arrows = new InventoryItem("Flechas", "Munición.", "crosshair", 20, 0.05);

        List<InventoryItem> read = Json.readList(Json.write(List.of(arrows)), InventoryItem.class);

        assertThat(read).containsExactly(arrows);
        assertThat(read.get(0).weight()).isEqualTo(0.05);
        assertThat(read.get(0).totalWeight()).isEqualTo(1.0);
    }
}
