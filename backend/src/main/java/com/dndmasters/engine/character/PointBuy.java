package com.dndmasters.engine.character;

import com.dndmasters.domain.Ability;
import java.util.EnumMap;
import java.util.Map;

/**
 * Standard D&D point-buy: 27 points, scores from 8 to 15 before racial/background
 * bonuses. Costs follow the SRD table (8→0, 9→1, ... 13→5, 14→7, 15→9).
 */
public final class PointBuy {

    public static final int TOTAL_POINTS = 27;
    public static final int MIN_SCORE = 8;
    public static final int MAX_SCORE = 15;

    private static final Map<Integer, Integer> COST = Map.of(
            8, 0, 9, 1, 10, 2, 11, 3, 12, 4, 13, 5, 14, 7, 15, 9);

    private PointBuy() {
    }

    public static int costOf(int score) {
        Integer cost = COST.get(score);
        if (cost == null) {
            throw new IllegalArgumentException("Score out of point-buy range: " + score);
        }
        return cost;
    }

    public static int totalCost(Map<Ability, Integer> scores) {
        int total = 0;
        for (Map.Entry<Ability, Integer> entry : scores.entrySet()) {
            total += costOf(entry.getValue());
        }
        return total;
    }

    public static int remaining(Map<Ability, Integer> scores) {
        return TOTAL_POINTS - totalCost(scores);
    }

    public static boolean isValid(Map<Ability, Integer> scores) {
        if (scores.size() != Ability.values().length) {
            return false;
        }
        return totalCost(scores) <= TOTAL_POINTS;
    }

    public static Map<Ability, Integer> standardArray() {
        Map<Ability, Integer> scores = new EnumMap<>(Ability.class);
        for (Ability ability : Ability.values()) {
            scores.put(ability, 10);
        }
        return scores;
    }
}
