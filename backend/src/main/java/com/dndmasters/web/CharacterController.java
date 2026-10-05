package com.dndmasters.web;

import com.dndmasters.domain.PlayerCharacter;
import com.dndmasters.persistence.mapper.CharacterMapper;
import com.dndmasters.persistence.repository.CharacterRepository;
import com.dndmasters.security.SecurityUtils;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/characters")
public class CharacterController {

    private final CharacterRepository characters;
    private final CharacterMapper mapper;

    public CharacterController(CharacterRepository characters, CharacterMapper mapper) {
        this.characters = characters;
        this.mapper = mapper;
    }

    @GetMapping
    public List<PlayerCharacter> list() {
        Long userId = SecurityUtils.currentUserId();
        List<PlayerCharacter> result = new ArrayList<>();
        characters.findByPredefinedTrueOrderByIdAsc().forEach(c -> result.add(mapper.toDomain(c)));
        if (userId != null) {
            characters.findByOwnerIdOrderByIdAsc(userId).forEach(c -> result.add(mapper.toDomain(c)));
        }
        return result;
    }

    @PostMapping
    public PlayerCharacter create(@RequestBody PlayerCharacter request) {
        Long userId = SecurityUtils.requireUserId();
        request.setId(null);
        request.setOwnerId(userId);
        request.setPredefined(false);
        var entity = mapper.toEntity(request);
        entity.setCreatedAt(Instant.now().toString());
        characters.save(entity);
        return mapper.toDomain(entity);
    }
}
