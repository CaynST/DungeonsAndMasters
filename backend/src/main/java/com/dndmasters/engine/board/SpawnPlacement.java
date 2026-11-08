package com.dndmasters.engine.board;

import com.dndmasters.domain.Cell;
import com.dndmasters.domain.EntityKind;
import com.dndmasters.domain.GameEntity;
import com.dndmasters.domain.GameMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Chooses where a DM-spawned creature appears. Candidates are free, in-bounds cells: never a
 * wall, obstacle, closed door or occupied cell. Existing enemies are reinforced first; otherwise
 * the creature appears as far as possible from the party. Remaining ties are resolved by a
 * row-major scan so the same board always yields the same cell.
 */
public final class SpawnPlacement {

    private SpawnPlacement() {
    }

    public static Optional<Cell> choose(GameMap map, List<GameEntity> entities) {
        if (map == null) {
            return Optional.empty();
        }
        Set<Cell> occupied = new HashSet<>();
        List<Cell> enemies = entities == null ? List.of() : entities.stream()
                .filter(e -> e.getKind() == EntityKind.ENEMY && e.isAlive())
                .map(GameEntity::cell)
                .toList();
        List<Cell> party = entities == null ? List.of() : entities.stream()
                .filter(e -> e.getKind() == EntityKind.PLAYER && e.isAlive())
                .map(GameEntity::cell)
                .toList();
        if (entities != null) {
            entities.forEach(e -> occupied.add(e.cell()));
        }

        Cell best = null;
        int bestEnemyDistance = Integer.MAX_VALUE;
        int bestPartyDistance = -1;
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                Cell candidate = new Cell(x, y);
                if (map.blocksMovement(candidate) || occupied.contains(candidate)) {
                    continue;
                }
                int enemyDistance = nearestDistance(candidate, enemies);
                int partyDistance = nearestDistance(candidate, party);
                boolean better = best == null
                        || enemyDistance < bestEnemyDistance
                        || (enemyDistance == bestEnemyDistance && partyDistance > bestPartyDistance);
                if (better) {
                    best = candidate;
                    bestEnemyDistance = enemyDistance;
                    bestPartyDistance = partyDistance;
                }
            }
        }
        return Optional.ofNullable(best);
    }

    private static int nearestDistance(Cell candidate, List<Cell> anchors) {
        int best = Integer.MAX_VALUE;
        for (Cell anchor : anchors) {
            best = Math.min(best, candidate.chebyshevTo(anchor));
        }
        return best;
    }
}
