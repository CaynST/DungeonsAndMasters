package com.dndmasters.content;

import com.dndmasters.domain.Cell;
import com.dndmasters.domain.Decor;
import com.dndmasters.domain.Door;
import com.dndmasters.domain.GameMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Builds playable, varied battle maps procedurally. Each theme changes the decor and the
 * kind of obstacles so two adventures never look the same.
 */
public final class MapGenerator {

    public record Layout(GameMap map, List<Cell> partySpawn, List<Cell> enemySpawns) {
    }

    private static final String[] THEMES = {"crypt", "forest", "cave", "ruins", "dungeon", "swamp"};

    private MapGenerator() {
    }

    public static String randomTheme(Random random) {
        return THEMES[random.nextInt(THEMES.length)];
    }

    public static Layout generate(String theme, int width, int height, Random random) {
        GameMap map = new GameMap();
        map.setTheme(theme);
        map.setName(themeLabel(theme));
        map.setWidth(width);
        map.setHeight(height);
        map.setTileSize(48);

        List<Cell> walls = new ArrayList<>();
        // Border.
        for (int x = 0; x < width; x++) {
            walls.add(new Cell(x, 0));
            walls.add(new Cell(x, height - 1));
        }
        for (int y = 0; y < height; y++) {
            walls.add(new Cell(0, y));
            walls.add(new Cell(width - 1, y));
        }

        List<Cell> obstacles = new ArrayList<>();
        List<Decor> decor = new ArrayList<>();
        List<Door> doors = new ArrayList<>();

        // Interior structure: a couple of pillar clusters away from the spawn rows.
        int clusters = 2 + random.nextInt(3);
        for (int i = 0; i < clusters; i++) {
            int cx = 3 + random.nextInt(Math.max(1, width - 7));
            int cy = 2 + random.nextInt(Math.max(1, height - 5));
            // Keep the horizontal corridor (middle row band) clear.
            if (Math.abs(cy - height / 2) <= 1) {
                continue;
            }
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (random.nextBoolean()) {
                        Cell c = new Cell(cx + dx, cy + dy);
                        if (inBounds(c, width, height) && !obstacles.contains(c)) {
                            obstacles.add(c);
                        }
                    }
                }
            }
        }

        // Scatter single obstacles.
        int scattered = 3 + random.nextInt(4);
        for (int i = 0; i < scattered; i++) {
            Cell c = new Cell(2 + random.nextInt(width - 4), 2 + random.nextInt(height - 4));
            if (inBounds(c, width, height) && !obstacles.contains(c) && Math.abs(c.y() - height / 2) > 1) {
                obstacles.add(c);
            }
        }

        // Decor: torches along the top wall, themed props elsewhere.
        for (int x = 2; x < width - 2; x += 3) {
            decor.add(new Decor(x, 1, "torch", "torch"));
        }
        String prop = propFor(theme);
        int props = 3 + random.nextInt(4);
        for (int i = 0; i < props; i++) {
            Cell c = new Cell(2 + random.nextInt(width - 4), 2 + random.nextInt(height - 4));
            decor.add(new Decor(c.x(), c.y(), prop, prop));
        }
        decor.add(new Decor(width / 2, 1, "rune", "rune"));

        // A door on the right wall as the way forward.
        doors.add(new Door(width - 1, height / 2, false));

        map.setWalls(walls);
        map.setObstacles(obstacles);
        map.setDecor(decor);
        map.setDoors(doors);

        List<Cell> partySpawn = List.of(
                new Cell(2, height / 2),
                new Cell(2, height / 2 - 1),
                new Cell(2, height / 2 + 1),
                new Cell(3, height / 2),
                new Cell(3, height / 2 - 1),
                new Cell(3, height / 2 + 1));

        List<Cell> enemySpawns = new ArrayList<>();
        int baseY = Math.max(2, height / 2 - 2);
        for (int i = 0; i < 8; i++) {
            int x = width - 2 - (i / 4);
            int y = baseY + (i % 4);
            Cell c = new Cell(x, Math.min(height - 2, Math.max(1, y)));
            if (!walls.contains(c) && !obstacles.contains(c)) {
                enemySpawns.add(c);
            }
        }
        if (enemySpawns.isEmpty()) {
            enemySpawns.add(new Cell(width - 2, height / 2));
        }
        return new Layout(map, partySpawn, enemySpawns);
    }

    private static boolean inBounds(Cell c, int width, int height) {
        return c.x() > 0 && c.y() > 0 && c.x() < width - 1 && c.y() < height - 1;
    }

    private static String propFor(String theme) {
        return switch (theme) {
            case "forest" -> "tree";
            case "cave" -> "rock";
            case "ruins" -> "rubble";
            case "dungeon" -> "barrel";
            case "swamp" -> "reed";
            default -> "bones";
        };
    }

    private static String themeLabel(String theme) {
        return switch (theme) {
            case "forest" -> "Claro del bosque";
            case "cave" -> "Caverna";
            case "ruins" -> "Ruinas";
            case "dungeon" -> "Mazmorra";
            case "swamp" -> "Pantano";
            default -> "Cripta";
        };
    }
}
