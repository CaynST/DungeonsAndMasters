package com.dndmasters.service;

import com.dndmasters.domain.GameEvent;
import com.dndmasters.persistence.entity.GameEventEntity;
import com.dndmasters.persistence.mapper.EventMapper;
import com.dndmasters.persistence.repository.GameEventRepository;
import com.dndmasters.util.Json;
import java.time.Instant;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Records immutable game events and broadcasts them to the campaign topic. */
@Service
public class GameEventService {

    private final GameEventRepository repository;
    private final EventMapper mapper;
    private final ApplicationEventPublisher publisher;

    public GameEventService(GameEventRepository repository, EventMapper mapper, ApplicationEventPublisher publisher) {
        this.repository = repository;
        this.mapper = mapper;
        this.publisher = publisher;
    }

    @Transactional
    public GameEvent record(Long campaignId, Long sceneId, String type, String actor, String message, Object data) {
        GameEventEntity entity = new GameEventEntity();
        entity.setCampaignId(campaignId);
        entity.setSceneId(sceneId);
        entity.setSequence(repository.maxSequence(campaignId) + 1);
        entity.setType(type);
        entity.setActor(actor);
        entity.setMessage(message);
        entity.setDataJson(data == null ? "{}" : Json.write(data));
        entity.setCreatedAt(Instant.now().toString());
        repository.save(entity);

        GameEvent event = mapper.toDomain(entity);
        publisher.publishEvent(new CampaignEvent(campaignId, event));
        return event;
    }

    @Transactional(readOnly = true)
    public List<GameEvent> recent(Long campaignId) {
        List<GameEventEntity> events = repository.findTop40ByCampaignIdOrderBySequenceDesc(campaignId);
        // Query returns newest first; expose oldest-first for narration flow.
        return events.stream().map(mapper::toDomain).sorted(java.util.Comparator.comparingLong(GameEvent::sequence)).toList();
    }
}
