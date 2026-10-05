package com.dndmasters.web;

import com.dndmasters.domain.PlayerCharacter;
import com.dndmasters.engine.character.CharacterBuildRequest;
import com.dndmasters.persistence.entity.CharacterEntity;
import com.dndmasters.persistence.entity.UserEntity;
import com.dndmasters.persistence.mapper.CharacterMapper;
import com.dndmasters.persistence.repository.CharacterRepository;
import com.dndmasters.persistence.repository.UserRepository;
import com.dndmasters.rules.Edition;
import com.dndmasters.security.SecurityUtils;
import com.dndmasters.service.CharacterService;
import com.dndmasters.service.FileStorageService;
import com.dndmasters.web.dto.CharacterDtos.CreateCharacterRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/characters")
public class CharacterController {

    private final CharacterRepository characters;
    private final CharacterMapper mapper;
    private final CharacterService characterService;
    private final FileStorageService storage;
    private final UserRepository users;

    public CharacterController(CharacterRepository characters, CharacterMapper mapper,
                               CharacterService characterService, FileStorageService storage, UserRepository users) {
        this.characters = characters;
        this.mapper = mapper;
        this.characterService = characterService;
        this.storage = storage;
        this.users = users;
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
        return characterService.create(userId, build, request.subclass());
    }

    /** Uploads a portrait image for a character the caller owns (or an admin). */
    @PostMapping("/{id}/portrait")
    public PlayerCharacter uploadPortrait(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        Long userId = SecurityUtils.requireUserId();
        CharacterEntity entity = requireOwned(id, userId);
        entity.setPortrait(storage.storePortrait(file, "char" + id));
        characters.save(entity);
        return mapper.toDomain(entity);
    }

    /** Renames a character the caller owns (or an admin). */
    @PatchMapping("/{id}")
    public PlayerCharacter rename(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Long userId = SecurityUtils.requireUserId();
        CharacterEntity entity = requireOwned(id, userId);
        String name = body.get("name");
        if (name != null && !name.isBlank()) {
            entity.setName(name.trim());
        }
        characters.save(entity);
        return mapper.toDomain(entity);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        Long userId = SecurityUtils.requireUserId();
        CharacterEntity entity = requireOwned(id, userId);
        if (entity.isPredefined()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Los personajes predefinidos no se pueden borrar");
        }
        characters.deleteById(id);
        return Map.of("deleted", id);
    }

    private CharacterEntity requireOwned(Long id, Long userId) {
        CharacterEntity entity = characters.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Personaje no encontrado"));
        boolean admin = users.findById(userId).map(UserEntity::getRole).map("ADMIN"::equals).orElse(false);
        boolean owner = entity.getOwnerId() != null && entity.getOwnerId().equals(userId);
        if (!admin && !owner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes gestionar ese personaje");
        }
        return entity;
    }
}
