package com.dndmasters.engine.dice;

import java.util.List;

/**
 * Result of rolling a dice expression. {@code natural20}/{@code natural1} are only
 * meaningful when the expression is a single d20.
 *
 * <p>{@code rerolledFrom} and {@code rerollBonus} describe dice that were rerolled by a
 * rule such as Great Weapon Fighting; both are empty/zero for ordinary rolls.</p>
 */
public record DiceRollResult(
        String expression,
        List<Integer> rolls,
        int modifier,
        int total,
        boolean natural20,
        boolean natural1,
        List<Integer> rerolledFrom,
        int rerollBonus) {

    /** Convenience constructor for ordinary rolls without reroll metadata. */
    public DiceRollResult(String expression, List<Integer> rolls, int modifier, int total,
                          boolean natural20, boolean natural1) {
        this(expression, rolls, modifier, total, natural20, natural1, List.of(), 0);
    }

    public String describe() {
        String dice = rolls.toString();
        if (modifier == 0) {
            return expression + " → " + dice + " = " + total;
        }
        return expression + " → " + dice + (modifier > 0 ? " + " : " − ") + Math.abs(modifier) + " = " + total;
    }
}
