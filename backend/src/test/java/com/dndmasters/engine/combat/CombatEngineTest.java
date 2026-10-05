package com.dndmasters.engine.combat;

import static org.assertj.core.api.Assertions.assertThat;

import com.dndmasters.domain.CombatParticipant;
import com.dndmasters.domain.CombatState;
import com.dndmasters.domain.CombatStatus;
import com.dndmasters.domain.EntityKind;
import com.dndmasters.domain.Stats;
import com.dndmasters.engine.dice.DiceEngine;
import com.dndmasters.testsupport.StubRandom;
import com.dndmasters.engine.rules.Combatant;
import com.dndmasters.engine.rules.InitiativeEngine;
import java.util.List;
import org.junit.jupiter.api.Test;

class CombatEngineTest {

    private CombatEngine engine(int... diceValues) {
        DiceEngine dice = new DiceEngine(StubRandom.of(diceValues));
        return new CombatEngine(new InitiativeEngine(dice), new TurnManager());
    }

    private static Combatant combatant(long id, String kind, Stats stats) {
        return new Combatant(id, "E" + id, kind, 10, 10, 12, stats, true);
    }

    @Test
    void startsCombatWithOrderedInitiative() {
        CombatEngine engine = engine(10, 5);
        CombatState state = engine.start(1L, 1L, List.of(
                combatant(1, "PLAYER", new Stats(10, 12, 10, 10, 10, 10)),
                combatant(2, "ENEMY", new Stats(10, 14, 10, 10, 10, 10))));

        assertThat(state.isActive()).isTrue();
        assertThat(state.getRound()).isEqualTo(1);
        assertThat(state.getCurrentTurnIndex()).isZero();
        assertThat(state.getParticipants()).extracting(CombatParticipant::getEntityId).containsExactly(1L, 2L);
        assertThat(state.currentParticipant().getName()).isEqualTo("E1");
    }

    @Test
    void skipsDeadParticipantsWhenAdvancing() {
        CombatEngine engine = new CombatEngine(new InitiativeEngine(new DiceEngine()), new TurnManager());
        CombatState state = new CombatState();
        state.setParticipants(List.of(
                participant(1, EntityKind.PLAYER, true),
                participant(2, EntityKind.ENEMY, false),
                participant(3, EntityKind.ENEMY, true)));
        state.setCurrentTurnIndex(0);
        state.setRound(1);
        state.setActive(true);

        CombatParticipant next = engine.turns().advance(state);

        assertThat(next.getEntityId()).isEqualTo(3L);
        assertThat(state.getRound()).isEqualTo(1);
    }

    @Test
    void incrementsRoundWhenWrappingPastLastParticipant() {
        CombatEngine engine = new CombatEngine(new InitiativeEngine(new DiceEngine()), new TurnManager());
        CombatState state = new CombatState();
        state.setParticipants(List.of(
                participant(1, EntityKind.PLAYER, true),
                participant(2, EntityKind.ENEMY, true)));
        state.setCurrentTurnIndex(1);
        state.setRound(1);
        state.setActive(true);

        CombatParticipant next = engine.turns().advance(state);

        assertThat(next.getEntityId()).isEqualTo(1L);
        assertThat(state.getRound()).isEqualTo(2);
    }

    @Test
    void combatFinishesWhenAllEnemiesAreDead() {
        TurnManager turns = new TurnManager();
        CombatState state = new CombatState();
        state.setParticipants(List.of(
                participant(1, EntityKind.PLAYER, true),
                participant(2, EntityKind.ENEMY, false)));
        state.setActive(true);
        state.setRound(1);

        assertThat(turns.isFinished(state)).isTrue();
        turns.finish(state);
        assertThat(state.getStatus()).isEqualTo(CombatStatus.FINISHED.name());
        assertThat(state.isActive()).isFalse();
    }

    private static CombatParticipant participant(long id, EntityKind kind, boolean alive) {
        CombatParticipant p = new CombatParticipant(id, id, "E" + id, kind, 10, (int) id, alive);
        return p;
    }
}
