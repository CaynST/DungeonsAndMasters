package com.dndmasters.content;

import com.dndmasters.content.AdventureDefinition.EnemyDefinition;
import com.dndmasters.content.AdventureDefinition.ItemDefinition;
import com.dndmasters.content.AdventureDefinition.NpcDefinition;
import com.dndmasters.content.AdventureDefinition.Placement;
import com.dndmasters.content.AdventureDefinition.SceneDefinition;
import com.dndmasters.domain.Attack;
import com.dndmasters.domain.Cell;
import com.dndmasters.domain.Decor;
import com.dndmasters.domain.Door;
import com.dndmasters.domain.GameMap;
import com.dndmasters.domain.Stats;
import java.util.ArrayList;
import java.util.List;

/** The one-shot "La Cripta de Valdros", defined in code so it is type-safe and testable. */
public final class ValdrosAdventure {

    public static final String KEY = "valdros";

    private ValdrosAdventure() {
    }

    public static AdventureDefinition build() {
        return new AdventureDefinition(
                KEY,
                "La Cripta de Valdros",
                "Una antigua cripta bajo las ruinas de Valdros, sellada hace siglos.",
                "Una pequeña compañía de aventureros desciende a la cripta de Valdros tras la pista de un relicario perdido.",
                "El Archivista",
                "El Guardián de Valdros",
                List.of(
                        "Érase una vez una cripta que nadie osaba nombrar, sepultada bajo las ruinas de Valdros.",
                        "Siglos atrás, sus guardianes sellaron un relicario para que jamás viera la luz.",
                        "Ahora, el sello se debilita... y alguien tiene que bajar."),
                "Recupera el Relicario de Valdros y sella la cripta.",
                enemies(),
                npcs(),
                items(),
                scenes());
    }

    private static List<EnemyDefinition> enemies() {
        return List.of(
                new EnemyDefinition("Goblin", 7, 13, 6,
                        new Stats(10, 14, 10, 10, 8, 8),
                        List.of(Attack.melee("Cimitarra", 4, "1d6+2")),
                        "MELEE", "#7BA05B", "skull", "/images/enemies/goblin.png"),
                new EnemyDefinition("Goblin Arquero", 7, 12, 6,
                        new Stats(10, 15, 10, 10, 8, 8),
                        List.of(Attack.ranged("Arco corto", 4, "1d6+2", 16)),
                        "RANGED", "#9BBF7B", "crosshair", "/images/enemies/goblin-archer.png"),
                new EnemyDefinition("Guardián de Valdros", 45, 17, 5,
                        new Stats(18, 10, 16, 8, 10, 10),
                        List.of(Attack.melee("Golpe sepulcral", 6, "2d6+3"),
                                Attack.ranged("Descarga necrótica", 5, "2d8", 12)),
                        "AGGRESSIVE", "#8B5CF6", "shield", "/images/enemies/guardian.png"));
    }

    private static List<NpcDefinition> npcs() {
        return List.of(new NpcDefinition(
                "El Archivista",
                "Un espectro encadenado a los archivos de la cripta. Recuerda todos los secretos de Valdros, pero exige respeto.",
                "/images/npcs/archivista.png",
                List.of(
                        "Nadie ha pisado estas salas en trescientos años... y los que lo intentaron, se quedaron.",
                        "El Relicario está tras la puerta sellada. Los goblins han profanado la antesala.",
                        "Si el Guardián despierta, no habrá piedad. Preparáos.")));
    }

    private static List<ItemDefinition> items() {
        return List.of(new ItemDefinition("Relicario de Valdros", "Un relicario de plata que late con luz arcana.", "RELIC", "gem"),
                new ItemDefinition("Poción de Curación", "Recupera 2d4+2 HP.", "POTION", "flask"),
                new ItemDefinition("Llave de hueso", "Abre la puerta sellada de la cripta.", "KEY", "key"));
    }

    private static List<SceneDefinition> scenes() {
        List<SceneDefinition> scenes = new ArrayList<>();

        GameMap threshold = borderedMap("Umbral de Valdros", 12, 10);
        threshold.getObstacles().addAll(List.of(new Cell(3, 2), new Cell(3, 7), new Cell(8, 2), new Cell(8, 7)));
        threshold.getDecor().addAll(List.of(
                new Decor(2, 5, "torch", "torch"),
                new Decor(9, 5, "torch", "torch"),
                new Decor(5, 1, "rune", "rune")));
        threshold.getDoors().add(new Door(11, 5, false));
        scenes.add(new SceneDefinition(0, "El Umbral de Valdros",
                "Escaleras de piedra húmeda descienden hacia una antesala iluminada por antorchas marchitas. Una figura espectral hojea un tomo imposible.",
                "SOCIAL", "Ancient crypt entrance, stone stairs, flickering torches, spectral archivist",
                threshold,
                List.of(new Cell(2, 5), new Cell(2, 4), new Cell(3, 5), new Cell(2, 6)),
                List.of(new Placement("NPC", "El Archivista", 6, 3))));

        GameMap crypt = borderedMap("Cripta Profanada", 14, 10);
        crypt.getObstacles().addAll(List.of(
                new Cell(4, 3), new Cell(4, 6), new Cell(7, 2), new Cell(7, 7), new Cell(10, 4)));
        crypt.getDecor().addAll(List.of(
                new Decor(1, 1, "bones", "bones"),
                new Decor(12, 8, "bones", "bones"),
                new Decor(6, 5, "rune", "rune")));
        scenes.add(new SceneDefinition(1, "La Cripta Profanada",
                "La cripta se abre en una caverna de huesos. Goblins saquean los sarcófagos; uno de ellos os ve y grita.",
                "COMBAT", "Desecrated crypt, bones, goblins raiding sarcophagi, torchlight",
                crypt,
                List.of(new Cell(1, 5), new Cell(1, 4), new Cell(2, 5), new Cell(1, 6)),
                List.of(new Placement("ENEMY", "Goblin", 11, 3),
                        new Placement("ENEMY", "Goblin", 12, 6),
                        new Placement("ENEMY", "Goblin Arquero", 13, 4))));

        GameMap seal = borderedMap("Sello de Valdros", 12, 12);
        seal.getObstacles().addAll(List.of(
                new Cell(3, 3), new Cell(3, 8), new Cell(8, 3), new Cell(8, 8),
                new Cell(5, 5), new Cell(6, 6), new Cell(5, 6), new Cell(6, 5)));
        seal.getDecor().addAll(List.of(
                new Decor(5, 1, "rune", "rune"),
                new Decor(6, 10, "rune", "rune"),
                new Decor(1, 5, "torch", "torch"),
                new Decor(10, 6, "torch", "torch")));
        scenes.add(new SceneDefinition(2, "El Sello de Valdros",
                "Una cámara circular de ébano. En el centro, sobre un pedestal, el Relicario... y ante él, una armadura vacía que empieza a moverse.",
                "BOSS", "Dark circular crypt chamber, black stone, floating relic, towering animated armor",
                seal,
                List.of(new Cell(1, 5), new Cell(1, 6), new Cell(2, 5), new Cell(2, 6)),
                List.of(new Placement("ENEMY", "Guardián de Valdros", 9, 6),
                        new Placement("ITEM", "Relicario de Valdros", 6, 6))));

        return scenes;
    }

    private static GameMap borderedMap(String name, int width, int height) {
        GameMap map = new GameMap();
        map.setName(name);
        map.setWidth(width);
        map.setHeight(height);
        map.setTileSize(48);
        java.util.LinkedHashSet<Cell> walls = new java.util.LinkedHashSet<>();
        for (int x = 0; x < width; x++) {
            walls.add(new Cell(x, 0));
            walls.add(new Cell(x, height - 1));
        }
        for (int y = 0; y < height; y++) {
            walls.add(new Cell(0, y));
            walls.add(new Cell(width - 1, y));
        }
        map.setWalls(new ArrayList<>(walls));
        return map;
    }
}
