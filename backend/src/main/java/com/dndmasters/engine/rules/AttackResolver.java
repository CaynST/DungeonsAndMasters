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
        DiceRollResult roll = dice.roll("1d20");
        int dieRoll = roll.rolls().get(0);
        int total = dieRoll + attack.attackBonus();
        boolean critical = dieRoll == 20;
        boolean fumble = dieRoll == 1;
        boolean hit = critical || (!fumble && total >= target.getAc());
        return new AttackResult(attack.name(), dieRoll, attack.attackBonus(), total, target.getAc(), hit, critical, fumble);
    }
}
