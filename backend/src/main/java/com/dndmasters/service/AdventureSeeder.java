package com.dndmasters.service;

import com.dndmasters.content.PredefinedCharacters;
import com.dndmasters.domain.PlayerCharacter;
import com.dndmasters.persistence.entity.CharacterEntity;
import com.dndmasters.persistence.mapper.CharacterMapper;
import com.dndmasters.persistence.repository.CharacterRepository;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Seeds the four predefined heroes on first boot. Idempotent. */
@Component
public class AdventureSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdventureSeeder.class);

    private final CharacterRepository characters;
    private final CharacterMapper mapper;

    public AdventureSeeder(CharacterRepository characters, CharacterMapper mapper) {
        this.characters = characters;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!characters.findByPredefinedTrueOrderByIdAsc().isEmpty()) {
            return;
        }
        String now = Instant.now().toString();
        for (PlayerCharacter character : PredefinedCharacters.all()) {
            CharacterEntity entity = mapper.toEntity(character);
            entity.setPredefined(true);
            entity.setCreatedAt(now);
            characters.save(entity);
        }
        log.info("Seeded {} predefined heroes", PredefinedCharacters.all().size());
    }
}
