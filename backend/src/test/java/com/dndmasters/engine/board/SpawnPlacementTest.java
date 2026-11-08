package com.dndmasters.engine.board;

import static org.assertj.core.api.Assertions.assertThat;

import com.dndmasters.domain.Cell;
import com.dndmasters.domain.Door;
import com.dndmasters.domain.EntityKind;
import com.dndmasters.domain.GameEntity;
import com.dndmasters.domain.GameMap;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Deterministic, in-bounds placement for DM-spawned creatures. */
class SpawnPlacementTest {

    @Test
    void prefersACellNextToExistingEnemiesAndAwayFromTheParty() {
        GameMap map = borderedMap(7, 5);
        List<GameEntity> entities = List.of(
                entity(EntityKind.PLAYER, 1, 2),
                entity(EntityKind.ENEMY, 4, 2));

        assertThat(SpawnPlacement.choose(map, entities)).contains(new Cell(5, 1));
    }

    @Test
    void withoutEnemiesChoosesTheFarthestFreeCellFromTheParty() {
        GameMap map = borderedMap(7, 5);
        List<GameEntity> entities = List.of(entity(EntityKind.PLAYER, 1, 2));

        assertThat(SpawnPlacement.choose(map, entities)).contains(new Cell(5, 1));
    }

    @Test
    void scanningIsDeterministicWhenNoPartyOrEnemiesExist() {
        GameMap map = borderedMap(5, 4);

        assertThat(SpawnPlacement.choose(map, List.of())).contains(new Cell(1, 1));
    }

    @Test
    void neverUsesWallsObstaclesClosedDoorsOrOccupiedCells() {
        GameMap map = borderedMap(4, 4);
        map.setObstacles(List.of(new Cell(1, 1)));
        map.setDoors(List.of(new Door(2, 1, false)));
        List<GameEntity> entities = List.of(entity(EntityKind.ITEM, 1, 2));

        assertThat(SpawnPlacement.choose(map, entities)).contains(new Cell(2, 2));
    }

    @Test
    void returnsEmptyWhenEveryCellIsBlockedOrOccupied() {
        GameMap map = borderedMap(3, 3);
        List<GameEntity> entities = new ArrayList<>();
        for (int y = 1; y < 2; y++) {
            for (int x = 1; x < 2; x++) {
                entities.add(entity(EntityKind.PLAYER, x, y));
            }
        }
        map.setObstacles(List.of(new Cell(1, 1)));

        assertThat(SpawnPlacement.choose(map, entities)).isEmpty();
    }

    private GameMap borderedMap(int width, int height) {
        GameMap map = new GameMap();
        map.setWidth(width);
        map.setHeight(height);
        List<Cell> walls = new ArrayList<>();
        for (int x = 0; x < width; x++) {
            walls.add(new Cell(x, 0));
            walls.add(new Cell(x, height - 1));
        }
        for (int y = 0; y < height; y++) {
            walls.add(new Cell(0, y));
            walls.add(new Cell(width - 1, y));
        }
        map.setWalls(walls);
        return map;
    }

    private GameEntity entity(EntityKind kind, int x, int y) {
        GameEntity entity = new GameEntity();
        entity.setKind(kind);
        entity.setAlive(true);
        entity.moveTo(new Cell(x, y));
        return entity;
    }
}
