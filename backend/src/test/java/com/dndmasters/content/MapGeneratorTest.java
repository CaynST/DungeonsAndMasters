package com.dndmasters.content;

import static org.assertj.core.api.Assertions.assertThat;

import com.dndmasters.domain.Cell;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MapGeneratorTest {

    @Test
    void generatesUniqueWallsAcrossThemes() {
        for (String theme : new String[]{"crypt", "forest", "cave", "ruins", "dungeon", "swamp"}) {
            for (int seed = 0; seed < 25; seed++) {
                MapGenerator.Layout layout = MapGenerator.generate(theme, 14, 11, new Random(seed));
                Set<Cell> seen = new HashSet<>();
                for (Cell wall : layout.map().getWalls()) {
                    assertThat(seen.add(wall))
                            .as("duplicate wall %s in theme %s (seed %d)", wall, theme, seed)
                            .isTrue();
                }
            }
        }
    }

    @Test
    void generatesUniqueObstacles() {
        MapGenerator.Layout layout = MapGenerator.generate("crypt", 14, 11, new Random(7));
        Set<Cell> seen = new HashSet<>();
        layout.map().getObstacles().forEach(cell -> assertThat(seen.add(cell)).isTrue());
    }

    @Test
    void keepsPartySpawnInsideBoundsAndFree() {
        MapGenerator.Layout layout = MapGenerator.generate("crypt", 12, 10, new Random(3));
        layout.partySpawn().forEach(spawn -> {
            assertThat(layout.map().inBounds(spawn)).isTrue();
            assertThat(layout.map().blocksMovement(spawn)).isFalse();
        });
    }
}
