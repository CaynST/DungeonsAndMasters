package com.dndmasters.engine.combat;

import com.dndmasters.domain.CombatParticipant;
import com.dndmasters.domain.CombatState;
import com.dndmasters.domain.CombatStatus;
import com.dndmasters.domain.EntityKind;
import java.util.List;

/** Advances turns within a combat, skipping dead participants and rolling the round over. */
public class TurnManager {

    /** Advances to the next living participant and returns it, or null if combat is over. */
    public CombatParticipant advance(CombatState state) {
        List<CombatParticipant> participants = state.getParticipants();
        int size = participants.size();
        if (size == 0) {
            finish(state);
            return null;
        }
        long alive = participants.stream().filter(CombatParticipant::isAlive).count();
        if (alive == 0) {
            finish(state);
            return null;
        }

        int index = state.getCurrentTurnIndex();
        for (int guard = 0; guard < size * 2; guard++) {
            index++;
            if (index >= size) {
                index = 0;
                state.setRound(state.getRound() + 1);
            }
            if (participants.get(index).isAlive()) {
                state.setCurrentTurnIndex(index);
                return participants.get(index);
            }
        }
        finish(state);
        return null;
    }

    public boolean isFinished(CombatState state) {
        return state.aliveEnemies() == 0 || state.aliveHeroes() == 0;
    }

    public void finish(CombatState state) {
        state.setActive(false);
        state.setStatus(CombatStatus.FINISHED.name());
    }

    /** Marks a participant dead. The caller decides whether to advance the turn. */
    public CombatParticipant markDead(CombatState state, long entityId) {
        for (CombatParticipant participant : state.getParticipants()) {
            if (participant.getEntityId() == entityId) {
                participant.setAlive(false);
                return participant;
            }
        }
        return null;
    }

    public boolean hasLivingEnemies(CombatState state) {
        return state.getParticipants().stream()
                .anyMatch(p -> p.getKind() == EntityKind.ENEMY && p.isAlive());
    }
}
