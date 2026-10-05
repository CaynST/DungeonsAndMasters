package com.dndmasters.content;

import com.dndmasters.domain.Attack;
import com.dndmasters.domain.Cell;
import com.dndmasters.domain.GameMap;
import com.dndmasters.domain.Stats;
import java.util.List;

/** Static definition of an adventure. Persisted into a campaign when it is created. */
public record AdventureDefinition(
        String key,
        String name,
        String setting,
        String description,
        String dmName,
        String objective,
        List<EnemyDefinition> enemies,
        List<NpcDefinition> npcs,
        List<ItemDefinition> items,
        List<SceneDefinition> scenes) {

    public record EnemyDefinition(
            String name,
            int hp,
            int ac,
            int speed,
            Stats stats,
            List<Attack> attacks,
            String behavior,
            String color,
            String icon,
            String portrait) {
    }

    public record NpcDefinition(String name, String description, String portrait, List<String> dialogue) {
    }

    public record ItemDefinition(String name, String description, String kind, String icon) {
    }

    public record Placement(String kind, String ref, int x, int y) {
    }

    public record SceneDefinition(
            int orderIndex,
            String title,
            String description,
            String kind,
            String imagePrompt,
            GameMap map,
            List<Cell> partySpawn,
            List<Placement> placements) {
    }
}
