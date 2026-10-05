package com.dndmasters.engine.rules;

import com.dndmasters.engine.dice.DiceEngine;
import com.dndmasters.engine.dice.DiceRollResult;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Rolls initiative (d20 + DEX) and orders combatants; ties break by DEX then id. */
public class InitiativeEngine {

    private final DiceEngine dice;

    public InitiativeEngine(DiceEngine dice) {
        this.dice = dice;
    }

    public List<InitiativeEntry> order(List<Combatant> combatants) {
        List<InitiativeEntry> entries = new ArrayList<>();
        for (Combatant combatant : combatants) {
            DiceRollResult roll = dice.roll("1d20");
            int dieRoll = roll.rolls().get(0);
            int dexMod = combatant.dexterityModifier();
            entries.add(new InitiativeEntry(
                    combatant.getId(),
                    combatant.getName(),
                    combatant.getKind(),
                    dieRoll,
                    dexMod,
                    dieRoll + dexMod,
                    0));
        }
        entries.sort(Comparator
                .comparingInt(InitiativeEntry::initiative).reversed()
                .thenComparing(Comparator.comparingInt(InitiativeEntry::dexterityModifier).reversed())
                .thenComparingLong(InitiativeEntry::entityId));

        List<InitiativeEntry> ordered = new ArrayList<>(entries.size());
        for (int i = 0; i < entries.size(); i++) {
            InitiativeEntry e = entries.get(i);
            ordered.add(new InitiativeEntry(e.entityId(), e.name(), e.kind(), e.dieRoll(), e.dexterityModifier(), e.initiative(), i));
        }
        return ordered;
    }
}
