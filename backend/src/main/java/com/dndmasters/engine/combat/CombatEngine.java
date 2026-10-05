package com.dndmasters.engine.combat;

import com.dndmasters.domain.CombatParticipant;
import com.dndmasters.domain.CombatState;
import com.dndmasters.domain.CombatStatus;
import com.dndmasters.domain.EntityKind;
import com.dndmasters.engine.rules.Combatant;
import com.dndmasters.engine.rules.InitiativeEngine;
import com.dndmasters.engine.rules.InitiativeEntry;
import java.util.ArrayList;
import java.util.List;

/** Builds and drives a combat encounter from a set of combatants. */
public class CombatEngine {

    private final InitiativeEngine initiativeEngine;
    private final TurnManager turnManager;

    public CombatEngine(InitiativeEngine initiativeEngine, TurnManager turnManager) {
        this.initiativeEngine = initiativeEngine;
        this.turnManager = turnManager;
    }

    public CombatState start(Long campaignId, Long sceneId, List<Combatant> combatants) {
        List<InitiativeEntry> order = initiativeEngine.order(combatants);
        List<CombatParticipant> participants = new ArrayList<>(order.size());
        for (InitiativeEntry entry : order) {
            participants.add(new CombatParticipant(
                    null,
                    entry.entityId(),
                    entry.name(),
                    EntityKind.valueOf(entry.kind()),
                    entry.initiative(),
                    entry.orderIndex(),
                    true));
        }
        CombatState state = new CombatState();
        state.setCampaignId(campaignId);
        state.setSceneId(sceneId);
        state.setActive(true);
        state.setRound(1);
        state.setCurrentTurnIndex(0);
        state.setStatus(CombatStatus.ACTIVE.name());
        state.setParticipants(participants);
        return state;
    }

    public TurnManager turns() {
        return turnManager;
    }
}
