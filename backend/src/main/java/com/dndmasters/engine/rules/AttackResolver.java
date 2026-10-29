package com.dndmasters.engine.rules;

import com.dndmasters.domain.Attack;
import com.dndmasters.engine.dice.DiceEngine;
import com.dndmasters.engine.dice.DiceRollResult;

/** Resolves an attack roll against a target's AC. Natural 20 always hits, natural 1 always misses. */
public class AttackResolver {

    private final DiceEngine dice;

    public AttackResolver(DiceEngine dice) {
        this.dice = dice;
    }

    public AttackResult resolve(Combatant attacker, Combatant target, Attack attack) {
        return resolve(attacker, target, attack, Advantage.NONE);
    }

    public AttackResult resolve(Combatant attacker, Combatant target, Attack attack, Advantage advantage) {
        int dieRoll = rollD20(advantage);
        int total = dieRoll + attack.attackBonus();
        boolean critical = dieRoll == 20;
        boolean fumble = dieRoll == 1;
        boolean hit = critical || (!fumble && total >= target.getAc());
        return new AttackResult(attack.name(), dieRoll, attack.attackBonus(), total, target.getAc(), hit, critical, fumble);
    }

    /** Rolls a d20, using the higher of two dice for advantage and the lower for disadvantage. */
    private int rollD20(Advantage advantage) {
        int first = dice.roll("1d20").rolls().get(0);
        if (advantage == Advantage.NONE) {
            return first;
        }
        int second = dice.roll("1d20").rolls().get(0);
        return advantage == Advantage.ADVANTAGE ? Math.max(first, second) : Math.min(first, second);
    }
}
