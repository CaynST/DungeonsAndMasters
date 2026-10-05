package com.dndmasters.engine.board;

import static org.assertj.core.api.Assertions.assertThat;

import com.dndmasters.domain.Cell;
import com.dndmasters.domain.GameEntity;
import com.dndmasters.domain.GameMap;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MovementValidatorTest {

    private GameMap map;
    private GameEntity entity;
    private final MovementValidator validator = new MovementValidator();

    @BeforeEach
    void setUp() {
        map = new GameMap();
        map.setWidth(10);
        map.setHeight(10);
        map.setWalls(List.of(new Cell(0, 1), new Cell(2, 0)));
        map.setObstacles(List.of(new Cell(1, 1)));

        entity = new GameEntity();
        entity.setId(1L);
        entity.setX(0);
        entity.setY(0);
        entity.setSpeed(6);
    }

    @Test
    void acceptsAdjacentStep() {
        MoveResult result = validator.validateTo(map, entity, new Cell(1, 0), Set.of());
        assertThat(result.ok()).isTrue();
        assertThat(result.cost()).isEqualTo(1);
    }

    @Test
    void rejectsWall() {
        MoveResult result = validator.validateTo(map, entity, new Cell(0, 1), Set.of());
        assertThat(result.ok()).isFalse();
        assertThat(result.reason()).contains("blocked");
    }

    @Test
    void rejectsPathThatPassesThroughWall() {
        MoveResult result = validator.validate(map, entity, List.of(new Cell(1, 0), new Cell(2, 0), new Cell(3, 0)), Set.of());
        assertThat(result.ok()).isFalse();
    }

    @Test
    void rejectsObstacle() {
        MoveResult result = validator.validateTo(map, entity, new Cell(1, 1), Set.of());
        assertThat(result.ok()).isFalse();
    }

    @Test
    void rejectsOccupiedCell() {
        MoveResult result = validator.validateTo(map, entity, new Cell(1, 0), Set.of(new Cell(1, 0)));
        assertThat(result.ok()).isFalse();
        assertThat(result.reason()).contains("occupied");
    }

    @Test
    void rejectsNonContiguousPath() {
        MoveResult result = validator.validate(map, entity, List.of(new Cell(1, 0), new Cell(3, 0)), Set.of());
        assertThat(result.ok()).isFalse();
        assertThat(result.reason()).contains("contiguous");
    }

    @Test
    void rejectsPathCostingMoreThanSpeed() {
        entity.setY(5);
        entity.setSpeed(6);
        MoveResult result = validator.validate(map, entity,
                List.of(new Cell(1, 5), new Cell(2, 5), new Cell(3, 5), new Cell(4, 5), new Cell(5, 5), new Cell(6, 5), new Cell(7, 5)),
                Set.of());
        assertThat(result.ok()).isFalse();
        assertThat(result.reason()).contains("Not enough movement");
    }

    @Test
    void rejectsEmptyPath() {
        assertThat(validator.validate(map, entity, List.of(), Set.of()).ok()).isFalse();
    }
}
