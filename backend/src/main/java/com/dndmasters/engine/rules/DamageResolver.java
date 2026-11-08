package com.dndmasters.engine.rules;

import com.dndmasters.domain.Attack;
import com.dndmasters.engine.dice.DiceEngine;
import com.dndmasters.engine.dice.DiceRollResult;

/**
 * Applies damage from a successful attack. On a critical hit the damage dice are
 * doubled (the flat modifier is not). HP never drops below zero.
 */
public class DamageResolver {

    /** Faces at or below this value are rerolled by Great Weapon Fighting. */
    private static final int GREAT_WEAPON_REROLL_FACE = 2;

    private final DiceEngine dice;

    public DamageResolver(DiceEngine dice) {
        this.dice = dice;
    }

    public DamageResult apply(Attack attack, AttackResult attackResult, Combatant target) {
        return apply(attack, attackResult, target, 0, false, false);
    }

    public DamageResult apply(Attack attack, AttackResult attackResult, Combatant target,
                              int bonusDamage, boolean resistance) {
        return apply(attack, attackResult, target, bonusDamage, resistance, false);
    }

    /**
     * Applies damage with optional flat bonus (Rage, Sneak Attack, Dueling) and resistance.
     * Critical hits double the dice only; bonuses are added afterwards and resistance halves
     * the final total, as in 5e.
     *
     * <p>With Great Weapon Fighting, each 1 or 2 on the damage dice is rerolled once and the
     * replacement is final. On a critical hit the prototype rolls one set of dice, rerolls it
     * and doubles the result, matching its existing critical-damage approximation.</p>
     */
    public DamageResult apply(Attack attack, AttackResult attackResult, Combatant target,
                              int bonusDamage, boolean resistance, boolean greatWeaponFighting) {
        if (!attackResult.hit()) {
            return new DamageResult(attack.damageExpression(), java.util.List.of(), 0, 0, 0,
                    target.getHp(), !target.isAlive());
        }

        DiceRollResult roll = greatWeaponFighting
                ? dice.rollRerollingLowFaces(attack.damageExpression(), GREAT_WEAPON_REROLL_FACE)
                : dice.roll(attack.damageExpression());
        int diceTotal = roll.rolls().stream().mapToInt(Integer::intValue).sum();
        int rerollBonus = roll.rerollBonus();
        if (attackResult.critical()) {
            diceTotal *= 2;
            rerollBonus *= 2;
        }
        int total = Math.max(0, diceTotal + roll.modifier() + Math.max(0, bonusDamage));
        if (resistance) {
            total /= 2;
            rerollBonus /= 2;
        }

        target.applyDamage(total);
        return new DamageResult(roll.expression(), roll.rolls(), diceTotal, roll.modifier(), total,
                target.getHp(), !target.isAlive(), roll.rerolledFrom().size(), rerollBonus);
    }
}
