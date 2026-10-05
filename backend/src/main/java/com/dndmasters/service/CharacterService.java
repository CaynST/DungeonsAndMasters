package com.dndmasters.service;

import com.dndmasters.domain.PlayerCharacter;
import com.dndmasters.engine.character.CharacterBuildRequest;
import com.dndmasters.engine.character.CharacterBuilder;
import com.dndmasters.persistence.entity.CharacterEntity;
import com.dndmasters.persistence.mapper.CharacterMapper;
import com.dndmasters.persistence.repository.CharacterRepository;
import com.dndmasters.rules.RuleSetRegistry;
import java.time.Instant;
import java.util.HashMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Creates and persists custom characters built with the SRD rules engine. */
@Service
public class CharacterService {

    private final CharacterBuilder builder;
    private final CharacterRepository characters;
    private final CharacterMapper mapper;
    private final RuleSetRegistry registry;

    public CharacterService(CharacterBuilder builder, CharacterRepository characters, CharacterMapper mapper,
                            RuleSetRegistry registry) {
        this.builder = builder;
        this.characters = characters;
        this.mapper = mapper;
        this.registry = registry;
    }

    @Transactional
    public PlayerCharacter create(Long userId, CharacterBuildRequest request) {
        return create(userId, request, null);
    }

    @Transactional
    public PlayerCharacter create(Long userId, CharacterBuildRequest request, String subclass) {
        PlayerCharacter character = builder.build(request);
        if (subclass != null && !subclass.isBlank()) {
            java.util.List<String> options = registry.get(request.edition()).subclassesOf(request.classId());
            if (!options.isEmpty() && !options.contains(subclass)) {
                throw new IllegalArgumentException("Subclase no válida para esa clase: " + subclass);
            }
            java.util.Map<String, Object> data = new HashMap<>(character.getCreationData());
            data.put("subclass", subclass);
            character.setCreationData(data);
        }
        character.setOwnerId(userId);
        character.setPredefined(false);
        character.setPortrait(null);
        CharacterEntity entity = mapper.toEntity(character);
        entity.setCreatedAt(Instant.now().toString());
        characters.save(entity);
        return mapper.toDomain(entity);
    }
}
