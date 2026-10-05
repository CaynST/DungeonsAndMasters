package com.dndmasters.engine.rules;

import java.util.List;

public record DamageResult(
        String expression,
        List<Integer> rolls,
        int diceTotal,
        int modifier,
        int total,
        int targetHpAfter,
        boolean dead) {
}
