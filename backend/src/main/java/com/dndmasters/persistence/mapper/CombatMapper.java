package com.dndmasters.persistence.mapper;

import com.dndmasters.domain.CombatParticipant;
import com.dndmasters.domain.CombatState;
import com.dndmasters.domain.EntityKind;
import com.dndmasters.domain.GameEntity;
import com.dndmasters.persistence.entity.CombatParticipantEntity;
import com.dndmasters.persistence.entity.CombatStateEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class CombatMapper {

    public CombatState toDomain(CombatStateEntity state, List<CombatParticipantEntity> participants,
                                Map<Long, GameEntity> lookup) {
        CombatState domain = new CombatState();
        domain.setId(state.getId());
        domain.setCampaignId(state.getCampaignId());
        domain.setSceneId(state.getSceneId());
        domain.setActive(state.isActive());
        domain.setRound(state.getRound());
        domain.setCurrentTurnIndex(state.getCurrentTurnIndex());
        domain.setStatus(state.getStatus());

        List<CombatParticipant> list = new ArrayList<>();
        for (CombatParticipantEntity p : participants) {
            GameEntity entity = lookup.get(p.getEntityId());
            String name = entity != null ? entity.getName() : "Participante " + p.getEntityId();
            EntityKind kind = entity != null ? entity.getKind() : EntityKind.ENEMY;
            list.add(new CombatParticipant(p.getId(), p.getEntityId(), name, kind, p.getInitiative(), p.getOrderIndex(), p.isAlive()));
        }
        domain.setParticipants(list);
        return domain;
    }
}
