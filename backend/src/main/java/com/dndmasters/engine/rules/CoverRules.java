package com.dndmasters.engine.rules;

import com.dndmasters.domain.Cell;
import com.dndmasters.domain.Door;
import com.dndmasters.domain.GameMap;
import java.util.ArrayList;
import java.util.List;

/**
 * Grid approximation of 5e cover (PHB): half cover gives +2 AC, three-quarters gives +5 AC,
 * total cover prevents the attack. Walls and closed doors block the line completely; one
 * obstacle on the line gives half cover and two or more give three-quarters. Melee attacks
 * at reach 1 ignore cover because there is no intermediate square.
 *
 * <p>Dexterity saving throws are not modelled yet because the prototype has no generic save
 * action; the AC bonus is the part that affects attacks.
 */
public final class CoverRules {

    public enum Cover {
        NONE(0, false),
        HALF(2, false),
        THREE_QUARTERS(5, false),
        TOTAL(0, true);

        private final int acBonus;
        private final boolean blocksAttack;

        Cover(int acBonus, boolean blocksAttack) {
            this.acBonus = acBonus;
            this.blocksAttack = blocksAttack;
        }

        public int acBonus() {
            return acBonus;
        }

        public boolean blocksAttack() {
            return blocksAttack;
        }

        public String label() {
            return switch (this) {
                case HALF -> "cobertura media";
                case THREE_QUARTERS -> "cobertura de tres cuartos";
                case TOTAL -> "cobertura total";
                default -> "sin cobertura";
            };
        }
    }

    public record Result(Cover cover, int acBonus, String reason) {
        public boolean blocksAttack() {
            return cover.blocksAttack();
        }
    }

    private CoverRules() {
    }

    public static Result check(GameMap map, Cell attacker, Cell target, boolean ranged) {
        if (map == null || !ranged || attacker.equals(target) || attacker.isAdjacent(target)) {
            return new Result(Cover.NONE, 0, null);
        }
        int obstacles = 0;
        for (Cell cell : line(attacker, target)) {
            if (cell.equals(attacker) || cell.equals(target)) {
                continue;
            }
            if (map.isWall(cell)) {
                return new Result(Cover.TOTAL, 0, "El objetivo está tras un muro (cobertura total).");
            }
            Door door = map.doorAt(cell);
            if (door != null && !door.open()) {
                return new Result(Cover.TOTAL, 0, "El objetivo está tras una puerta cerrada (cobertura total).");
            }
            if (map.isObstacle(cell)) {
                obstacles++;
            }
        }
        if (obstacles >= 2) {
            return new Result(Cover.THREE_QUARTERS, 5,
                    "El objetivo tiene cobertura de tres cuartos (+5 CA).");
        }
        if (obstacles == 1) {
            return new Result(Cover.HALF, 2, "El objetivo tiene cobertura media (+2 CA).");
        }
        return new Result(Cover.NONE, 0, null);
    }

    /** Bresenham line between cell centres, including both endpoints. */
    static List<Cell> line(Cell from, Cell to) {
        List<Cell> cells = new ArrayList<>();
        int x0 = from.x();
        int y0 = from.y();
        int x1 = to.x();
        int y1 = to.y();
        int dx = Math.abs(x1 - x0);
        int sx = x0 < x1 ? 1 : -1;
        int dy = -Math.abs(y1 - y0);
        int sy = y0 < y1 ? 1 : -1;
        int err = dx + dy;
        while (true) {
            cells.add(new Cell(x0, y0));
            if (x0 == x1 && y0 == y1) {
                break;
            }
            int e2 = 2 * err;
            if (e2 >= dy) {
                err += dy;
                x0 += sx;
            }
            if (e2 <= dx) {
                err += dx;
                y0 += sy;
            }
        }
        return cells;
    }
}
