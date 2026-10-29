package com.dndmasters.engine.rules;

import static org.assertj.core.api.Assertions.assertThat;

import com.dndmasters.domain.Cell;
import com.dndmasters.domain.Door;
import com.dndmasters.domain.GameMap;
import org.junit.jupiter.api.Test;

class CoverRulesTest {

    private static GameMap map() {
        GameMap map = new GameMap();
        map.setWidth(10);
        map.setHeight(10);
        return map;
    }

    private static CoverRules.Result check(GameMap map, Cell attacker, Cell target, boolean ranged) {
        return CoverRules.check(map, attacker, target, ranged);
    }

    @Test
    void openLineHasNoCover() {
        CoverRules.Result result = check(map(), new Cell(1, 2), new Cell(5, 2), true);

        assertThat(result.cover()).isEqualTo(CoverRules.Cover.NONE);
        assertThat(result.acBonus()).isZero();
        assertThat(result.blocksAttack()).isFalse();
    }

    @Test
    void oneObstacleGivesHalfCover() {
        GameMap map = map();
        map.getObstacles().add(new Cell(3, 2));

        CoverRules.Result result = check(map, new Cell(1, 2), new Cell(5, 2), true);

        assertThat(result.cover()).isEqualTo(CoverRules.Cover.HALF);
        assertThat(result.acBonus()).isEqualTo(2);
    }

    @Test
    void twoObstaclesGiveThreeQuartersCover() {
        GameMap map = map();
        map.getObstacles().add(new Cell(2, 2));
        map.getObstacles().add(new Cell(3, 2));

        CoverRules.Result result = check(map, new Cell(1, 2), new Cell(5, 2), true);

        assertThat(result.cover()).isEqualTo(CoverRules.Cover.THREE_QUARTERS);
        assertThat(result.acBonus()).isEqualTo(5);
    }

    @Test
    void wallBlocksTheLineCompletely() {
        GameMap map = map();
        map.getWalls().add(new Cell(3, 2));

        CoverRules.Result result = check(map, new Cell(1, 2), new Cell(5, 2), true);

        assertThat(result.cover()).isEqualTo(CoverRules.Cover.TOTAL);
        assertThat(result.blocksAttack()).isTrue();
    }

    @Test
    void closedDoorBlocksButOpenDoorDoesNot() {
        GameMap closed = map();
        closed.getDoors().add(new Door(3, 2, false));
        assertThat(check(closed, new Cell(1, 2), new Cell(5, 2), true).cover())
                .isEqualTo(CoverRules.Cover.TOTAL);

        GameMap open = map();
        open.getDoors().add(new Door(3, 2, true));
        assertThat(check(open, new Cell(1, 2), new Cell(5, 2), true).cover())
                .isEqualTo(CoverRules.Cover.NONE);
    }

    @Test
    void meleeAttacksIgnoreCover() {
        GameMap map = map();
        map.getWalls().add(new Cell(3, 2));

        CoverRules.Result result = check(map, new Cell(1, 2), new Cell(5, 2), false);

        assertThat(result.cover()).isEqualTo(CoverRules.Cover.NONE);
    }

    @Test
    void adjacentRangedAttackHasNoIntermediateCell() {
        GameMap map = map();
        map.getObstacles().add(new Cell(2, 2));

        CoverRules.Result result = check(map, new Cell(2, 2), new Cell(3, 2), true);

        assertThat(result.cover()).isEqualTo(CoverRules.Cover.NONE);
    }

    @Test
    void wallBehindTheTargetDoesNotBlock() {
        GameMap map = map();
        map.getWalls().add(new Cell(4, 2));

        CoverRules.Result result = check(map, new Cell(1, 2), new Cell(3, 2), true);

        assertThat(result.cover()).isEqualTo(CoverRules.Cover.NONE);
    }

    @Test
    void diagonalWallBlocksTheLine() {
        GameMap map = map();
        map.getWalls().add(new Cell(2, 2));

        CoverRules.Result result = check(map, new Cell(1, 1), new Cell(4, 4), true);

        assertThat(result.cover()).isEqualTo(CoverRules.Cover.TOTAL);
    }

    @Test
    void coverBonusesMatchThePhb() {
        assertThat(CoverRules.Cover.NONE.acBonus()).isZero();
        assertThat(CoverRules.Cover.HALF.acBonus()).isEqualTo(2);
        assertThat(CoverRules.Cover.THREE_QUARTERS.acBonus()).isEqualTo(5);
        assertThat(CoverRules.Cover.TOTAL.blocksAttack()).isTrue();
    }
}
