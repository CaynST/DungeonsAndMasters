package com.dndmasters.web;

import com.dndmasters.domain.PlayerCharacter;
import com.dndmasters.engine.character.CharacterBuildRequest;
import com.dndmasters.persistence.mapper.CharacterMapper;
import com.dndmasters.persistence.repository.CharacterRepository;
import com.dndmasters.rules.Edition;
import com.dndmasters.security.SecurityUtils;
import com.dndmasters.service.CharacterService;
import com.dndmasters.web.dto.CharacterDtos.CreateCharacterRequest;
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
    private final CharacterService characterService;

    public CharacterController(CharacterRepository characters, CharacterMapper mapper,
                              CharacterService characterService) {
        this.characters = characters;
        this.mapper = mapper;
        this.characterService = characterService;
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
    public PlayerCharacter create(@RequestBody CreateCharacterRequest request) {
        Long userId = SecurityUtils.requireUserId();
        CharacterBuildRequest build = new CharacterBuildRequest(
                request.name(),
                Edition.fromId(request.edition()),
                request.speciesId(),
                request.classId(),
                request.backgroundId(),
                request.scores() == null ? com.dndmasters.engine.character.PointBuy.standardArray() : request.scores().toMap(),
                request.skills(),
                request.armor(),
                request.weapon());
        return characterService.create(userId, build);
    }
}
