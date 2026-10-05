package com.dndmasters.engine.dice;

import java.util.List;

/**
 * Result of rolling a dice expression. {@code natural20}/{@code natural1} are only
 * meaningful when the expression is a single d20.
 */
public record DiceRollResult(
        String expression,
        List<Integer> rolls,
        int modifier,
        int total,
        boolean natural20,
        boolean natural1) {

    public String describe() {
        String dice = rolls.toString();
        if (modifier == 0) {
            return expression + " → " + dice + " = " + total;
        }
        return expression + " → " + dice + (modifier > 0 ? " + " : " − ") + Math.abs(modifier) + " = " + total;
    }
}
