package com.dndmasters.domain;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CombatState {

    private Long id;
    private Long campaignId;
    private Long sceneId;
    private boolean active;
    private int round;
    private int currentTurnIndex;
    private String status = CombatStatus.NOT_STARTED.name();
    private List<CombatParticipant> participants = new ArrayList<>();

    public CombatParticipant currentParticipant() {
        if (participants.isEmpty() || currentTurnIndex < 0 || currentTurnIndex >= participants.size()) {
            return null;
        }
        return participants.get(currentTurnIndex);
    }

    public long aliveEnemies() {
        return participants.stream().filter(p -> p.isEnemy() && p.isAlive()).count();
    }

    public long aliveHeroes() {
        return participants.stream().filter(p -> !p.isEnemy() && p.isAlive()).count();
    }

    public boolean isCombatantTurn(long entityId) {
        CombatParticipant current = currentParticipant();
        return current != null && current.getEntityId() == entityId;
    }
}
