package com.dndmasters.adventure;

import java.util.List;

/** A generated adventure blueprint, before maps and stats are materialised. */
public record AdventurePlan(
        String title,
        String setting,
        String description,
        String objective,
        String villain,
        List<String> premise,
        List<ScenePlan> scenes) {

    public record ScenePlan(
            String title,
            String description,
            String kind,
            String objective,
            String theme,
            List<String> enemyIds) {
    }
}
