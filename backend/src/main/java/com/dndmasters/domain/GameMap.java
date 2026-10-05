package com.dndmasters.domain;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GameMap {

    private Long id;
    private String name;
    private int width;
    private int height;
    private int tileSize = 48;
    private List<Cell> walls = new ArrayList<>();
    private List<Door> doors = new ArrayList<>();
    private List<Cell> obstacles = new ArrayList<>();
    private List<Decor> decor = new ArrayList<>();

    public boolean inBounds(Cell c) {
        return c.x() >= 0 && c.y() >= 0 && c.x() < width && c.y() < height;
    }

    public boolean isWall(Cell c) {
        return walls.contains(c);
    }

    public boolean isObstacle(Cell c) {
        return obstacles.contains(c);
    }

    public Door doorAt(Cell c) {
        return doors.stream().filter(d -> d.x() == c.x() && d.y() == c.y()).findFirst().orElse(null);
    }

    /** True when the cell cannot be entered by a walking creature. */
    public boolean blocksMovement(Cell c) {
        if (!inBounds(c)) {
            return true;
        }
        if (isWall(c) || isObstacle(c)) {
            return true;
        }
        Door door = doorAt(c);
        return door != null && door.blocksMovement();
    }
}
