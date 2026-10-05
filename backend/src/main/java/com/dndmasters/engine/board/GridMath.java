package com.dndmasters.engine.board;

import com.dndmasters.domain.Cell;
import java.util.ArrayList;
import java.util.List;

public final class GridMath {

    private static final int[][] DIRECTIONS = {
            {-1, -1}, {0, -1}, {1, -1},
            {-1, 0}, {1, 0},
            {-1, 1}, {0, 1}, {1, 1}
    };

    private GridMath() {
    }

    public static List<Cell> neighbors(Cell cell) {
        List<Cell> result = new ArrayList<>(8);
        for (int[] d : DIRECTIONS) {
            result.add(cell.plus(d[0], d[1]));
        }
        return result;
    }

    public static int chebyshev(Cell a, Cell b) {
        return a.chebyshevTo(b);
    }

    public static boolean isStep(Cell from, Cell to) {
        return from.isAdjacent(to);
    }
}
