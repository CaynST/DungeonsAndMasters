package com.dndmasters.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.dndmasters.domain.Cell;
import com.dndmasters.domain.GameMap;
import com.dndmasters.persistence.entity.MapEntity;
import java.util.List;
import org.junit.jupiter.api.Test;

class MapMapperTest {

    private final MapMapper mapper = new MapMapper();

    @Test
    void roundTripsDifficultTerrainThroughTheJsonColumn() {
        GameMap map = new GameMap();
        map.setName("Terreno difícil");
        map.setWidth(8);
        map.setHeight(6);
        map.setTileSize(48);
        map.setDifficultTerrain(List.of(new Cell(2, 3), new Cell(4, 1)));

        MapEntity entity = mapper.toEntity(map, 42L);

        assertThat(entity.getDifficultTerrainJson()).isNotNull().contains("\"x\":2", "\"y\":3");
        GameMap reloaded = mapper.toDomain(entity);
        assertThat(reloaded.getDifficultTerrain())
                .containsExactly(new Cell(2, 3), new Cell(4, 1));
    }

    @Test
    void defaultsToAnEmptyDifficultTerrainListWhenTheColumnIsEmpty() {
        MapEntity entity = new MapEntity();
        entity.setName("Sin terreno");
        entity.setWidth(5);
        entity.setHeight(5);
        entity.setDifficultTerrainJson("[]");

        GameMap map = mapper.toDomain(entity);

        assertThat(map.getDifficultTerrain()).isEmpty();
        assertThat(map.isDifficultTerrain(new Cell(1, 1))).isFalse();
    }
}
