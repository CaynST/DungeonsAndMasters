package com.dndmasters.service;

import com.dndmasters.domain.PlayerCharacter;
import com.dndmasters.engine.character.CharacterBuildRequest;
import com.dndmasters.engine.character.CharacterBuilder;
import com.dndmasters.persistence.entity.CharacterEntity;
import com.dndmasters.persistence.mapper.CharacterMapper;
import com.dndmasters.persistence.repository.CharacterRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Creates and persists custom characters built with the SRD rules engine. */
@Service
public class CharacterService {

    private final CharacterBuilder builder;
    private final CharacterRepository characters;
    private final CharacterMapper mapper;

    public CharacterService(CharacterBuilder builder, CharacterRepository characters, CharacterMapper mapper) {
        this.builder = builder;
        this.characters = characters;
        this.mapper = mapper;
    }

    @Transactional
    public PlayerCharacter create(Long userId, CharacterBuildRequest request) {
        PlayerCharacter character = builder.build(request);
        character.setOwnerId(userId);
        character.setPredefined(false);
        character.setPortrait(null);
        CharacterEntity entity = mapper.toEntity(character);
        entity.setCreatedAt(Instant.now().toString());
        characters.save(entity);
        return mapper.toDomain(entity);
    }
}
