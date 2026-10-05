package com.dndmasters.engine.board;

import com.dndmasters.domain.Cell;
import java.util.List;

public record MoveResult(boolean ok, String reason, int cost, List<Cell> path) {

    public static MoveResult rejected(String reason) {
        return new MoveResult(false, reason, 0, List.of());
    }

    public static MoveResult ok(int cost, List<Cell> path) {
        return new MoveResult(true, null, cost, path);
    }
}
