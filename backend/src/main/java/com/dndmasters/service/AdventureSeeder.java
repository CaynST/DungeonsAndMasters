package com.dndmasters.service;

import com.dndmasters.content.PredefinedCharacters;
import com.dndmasters.domain.PlayerCharacter;
import com.dndmasters.persistence.entity.CharacterEntity;
import com.dndmasters.persistence.entity.UserEntity;
import com.dndmasters.persistence.mapper.CharacterMapper;
import com.dndmasters.persistence.repository.CharacterRepository;
import com.dndmasters.persistence.repository.UserRepository;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Seeds the four predefined heroes on first boot and guarantees an administrator exists. */
@Component
public class AdventureSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdventureSeeder.class);

    private final CharacterRepository characters;
    private final CharacterMapper mapper;
    private final UserRepository users;

    public AdventureSeeder(CharacterRepository characters, CharacterMapper mapper, UserRepository users) {
        this.characters = characters;
        this.mapper = mapper;
        this.users = users;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (characters.findByPredefinedTrueOrderByIdAsc().isEmpty()) {
            String now = Instant.now().toString();
            for (PlayerCharacter character : PredefinedCharacters.all()) {
                CharacterEntity entity = mapper.toEntity(character);
                entity.setPredefined(true);
                entity.setCreatedAt(now);
                characters.save(entity);
            }
            log.info("Seeded {} predefined heroes", PredefinedCharacters.all().size());
        }
        ensureAdmin();
    }

    /** If no admin exists yet, promote the earliest registered user. */
    private void ensureAdmin() {
        boolean hasAdmin = users.findAll().stream().anyMatch(u -> "ADMIN".equals(u.getRole()));
        if (hasAdmin) {
            return;
        }
        users.findAll().stream()
                .min(java.util.Comparator.comparing(UserEntity::getId))
                .ifPresent(user -> {
                    user.setRole("ADMIN");
                    users.save(user);
                    log.info("Promoted '{}' to administrator", user.getUsername());
                });
    }
}
