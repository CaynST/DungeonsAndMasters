package com.dndmasters.engine.rules;

import com.dndmasters.domain.Ability;
import com.dndmasters.engine.dice.DiceEngine;
import com.dndmasters.engine.dice.DiceRollResult;

/** Resolves saving throws: d20 + ability modifier against a DC. */
public class SaveResolver {

    private final DiceEngine dice;

    public SaveResolver(DiceEngine dice) {
        this.dice = dice;
    }

    public SaveResult resolve(Combatant combatant, Ability ability, int dc) {
        return resolve(combatant, ability, dc, Advantage.NONE);
    }

    public SaveResult resolve(Combatant combatant, Ability ability, int dc, Advantage advantage) {
        DiceRollResult first = dice.roll("1d20");
        int firstRoll = first.rolls().get(0);
        int secondRoll = firstRoll;
        int chosen = firstRoll;
        if (advantage != Advantage.NONE) {
            DiceRollResult second = dice.roll("1d20");
            secondRoll = second.rolls().get(0);
            chosen = advantage == Advantage.ADVANTAGE
                    ? Math.max(firstRoll, secondRoll)
                    : Math.min(firstRoll, secondRoll);
        }

        int modifier = combatant.modifier(ability);
        int total = chosen + modifier;
        return new SaveResult(ability.name(), chosen, secondRoll, modifier, total, dc, total >= dc);
    }
}
