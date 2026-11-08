package com.dndmasters.engine.rules;

import java.util.List;

/**
 * Result of applying damage. {@code rerolls}/{@code rerollBonus} report Great Weapon
 * Fighting rerolls; {@code rerollBonus} is the extra damage those rerolls contributed to
 * the final total (doubled on a critical hit, halved by resistance).
 */
public record DamageResult(
        String expression,
        List<Integer> rolls,
        int diceTotal,
        int modifier,
        int total,
        int targetHpAfter,
        boolean dead,
        int rerolls,
        int rerollBonus) {

    /** Convenience constructor for damage without reroll metadata. */
    public DamageResult(String expression, List<Integer> rolls, int diceTotal, int modifier,
                        int total, int targetHpAfter, boolean dead) {
        this(expression, rolls, diceTotal, modifier, total, targetHpAfter, dead, 0, 0);
    }
}
