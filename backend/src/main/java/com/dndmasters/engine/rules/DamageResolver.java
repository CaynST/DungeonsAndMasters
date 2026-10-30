package com.dndmasters.engine.rules;

import com.dndmasters.domain.Attack;
import com.dndmasters.engine.dice.DiceEngine;
import com.dndmasters.engine.dice.DiceRollResult;

/**
 * Applies damage from a successful attack. On a critical hit the damage dice are
 * doubled (the flat modifier is not). HP never drops below zero.
 */
public class DamageResolver {

    private final DiceEngine dice;

    public DamageResolver(DiceEngine dice) {
        this.dice = dice;
    }

    public DamageResult apply(Attack attack, AttackResult attackResult, Combatant target) {
        return apply(attack, attackResult, target, 0, false);
    }

    /**
     * Applies damage with optional flat bonus (Rage, Sneak Attack) and resistance.
     * Critical hits double the dice only; bonuses are added afterwards and resistance halves
     * the final total, as in 5e.
     */
    public DamageResult apply(Attack attack, AttackResult attackResult, Combatant target,
                              int bonusDamage, boolean resistance) {
        if (!attackResult.hit()) {
            return new DamageResult(attack.damageExpression(), java.util.List.of(), 0, 0, 0, target.getHp(), !target.isAlive());
        }

        DiceRollResult roll = dice.roll(attack.damageExpression());
        int diceTotal = roll.rolls().stream().mapToInt(Integer::intValue).sum();
        if (attackResult.critical()) {
            diceTotal *= 2;
        }
        int total = Math.max(0, diceTotal + roll.modifier() + Math.max(0, bonusDamage));
        if (resistance) {
            total /= 2;
        }

        target.applyDamage(total);
        return new DamageResult(roll.expression(), roll.rolls(), diceTotal, roll.modifier(), total, target.getHp(), !target.isAlive());
    }
}
