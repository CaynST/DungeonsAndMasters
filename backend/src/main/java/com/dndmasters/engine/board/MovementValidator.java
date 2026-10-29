package com.dndmasters.engine.board;

import com.dndmasters.domain.Cell;
import com.dndmasters.domain.GameEntity;
import com.dndmasters.domain.GameMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Validates a movement path cell by cell. Walls, obstacles and closed doors block, as
 * do cells occupied by other creatures. The whole move is rejected if any step is
 * invalid — partial moves are never applied.
 */
public class MovementValidator {

    public MoveResult validate(GameMap map, GameEntity entity, List<Cell> path, Set<Cell> occupied) {
        if (path == null || path.isEmpty()) {
            return MoveResult.rejected("No destination provided");
        }
        Set<Cell> blocked = new HashSet<>(occupied);
        blocked.remove(entity.cell());

        Cell current = entity.cell();
        int cost = 0;
        for (Cell step : path) {
            if (!step.isAdjacent(current)) {
                return MoveResult.rejected("Path is not contiguous at " + step);
            }
            // Diagonal movement may not cut through the corner of a wall/obstacle (PHB).
            int dx = step.x() - current.x();
            int dy = step.y() - current.y();
            if (dx != 0 && dy != 0) {
                if (map.blocksMovement(new Cell(current.x() + dx, current.y()))
                        || map.blocksMovement(new Cell(current.x(), current.y() + dy))) {
                    return MoveResult.rejected("No puedes atravesar la esquina de un muro en " + step);
                }
            }
            if (map.blocksMovement(step)) {
                return MoveResult.rejected("Cell " + step + " is blocked");
            }
            if (!step.equals(current) && blocked.contains(step)) {
                return MoveResult.rejected("Cell " + step + " is occupied");
            }
            cost++;
            current = step;
        }
        if (cost > entity.getSpeed()) {
            return MoveResult.rejected("Not enough movement: needs " + cost + ", has " + entity.getSpeed());
        }
        return MoveResult.ok(cost, List.copyOf(path));
    }

    public MoveResult validateTo(GameMap map, GameEntity entity, Cell destination, Set<Cell> occupied) {
        return validate(map, entity, List.of(destination), occupied);
    }
}
