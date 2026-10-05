package com.dndmasters.domain;

import java.util.Objects;

/** A grid coordinate. Used for walls, obstacles, doors and entity positions. */
public record Cell(int x, int y) {

    public Cell {
        Objects.requireNonNull(x);
        Objects.requireNonNull(y);
    }

    public Cell plus(int dx, int dy) {
        return new Cell(x + dx, y + dy);
    }

    public int chebyshevTo(Cell other) {
        return Math.max(Math.abs(x - other.x), Math.abs(y - other.y));
    }

    public boolean isAdjacent(Cell other) {
        int dx = Math.abs(x - other.x);
        int dy = Math.abs(y - other.y);
        return (dx <= 1 && dy <= 1) && (dx + dy) > 0;
    }
}
