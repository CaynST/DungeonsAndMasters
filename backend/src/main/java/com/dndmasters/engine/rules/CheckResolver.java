package com.dndmasters.engine.rules;

import com.dndmasters.engine.dice.DiceEngine;
import com.dndmasters.engine.dice.DiceRollResult;

/** Resolves ability checks against a DC. */
public class CheckResolver {

    private final DiceEngine dice;

    public CheckResolver(DiceEngine dice) {
        this.dice = dice;
    }

    public CheckResult resolve(Combatant combatant, String skill, int dc) {
        return resolve(combatant, skill, dc, Advantage.NONE);
    }

    public CheckResult resolve(Combatant combatant, String skill, int dc, Advantage advantage) {
        return resolve(combatant, skill, dc, advantage, 0);
    }

    /**
     * Resolves a check with an extra flat modifier (e.g. the -2 per exhaustion level).
     * Natural 20 always succeeds and natural 1 always fails, as before.
     */
    public CheckResult resolve(Combatant combatant, String skill, int dc, Advantage advantage, int extraModifier) {
        DiceRollResult first = dice.roll("1d20");
        int firstRoll = first.rolls().get(0);

        int chosen = firstRoll;
        int secondRoll = firstRoll;
        if (advantage != Advantage.NONE) {
            DiceRollResult second = dice.roll("1d20");
            secondRoll = second.rolls().get(0);
            chosen = advantage == Advantage.ADVANTAGE
                    ? Math.max(firstRoll, secondRoll)
                    : Math.min(firstRoll, secondRoll);
        }

        int modifier = combatant.modifier(Skills.abilityFor(skill));
        int total = chosen + modifier + extraModifier;
        boolean success = chosen == 20 || (chosen != 1 && total >= dc);

        return new CheckResult(
                skill,
                dc,
                firstRoll,
                secondRoll,
                modifier,
                total,
                success,
                chosen == 20,
                chosen == 1);
    }
}
