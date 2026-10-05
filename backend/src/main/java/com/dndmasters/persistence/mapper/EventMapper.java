package com.dndmasters.persistence.mapper;

import com.dndmasters.domain.GameEvent;
import com.dndmasters.persistence.entity.GameEventEntity;
import org.springframework.stereotype.Component;

@Component
public class EventMapper {

    public GameEvent toDomain(GameEventEntity e) {
        return new GameEvent(
                e.getId(),
                e.getCampaignId(),
                e.getSceneId(),
                e.getSequence(),
                e.getType(),
                e.getActor(),
                e.getMessage(),
                e.getDataJson(),
                e.getCreatedAt());
    }
}
