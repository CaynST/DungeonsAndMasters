package com.dndmasters.web;

import com.dndmasters.persistence.entity.CampaignEntity;
import com.dndmasters.persistence.entity.CharacterEntity;
import com.dndmasters.persistence.entity.UserEntity;
import com.dndmasters.persistence.repository.CampaignMemberRepository;
import com.dndmasters.persistence.repository.CampaignRepository;
import com.dndmasters.persistence.repository.CharacterRepository;
import com.dndmasters.persistence.repository.UserRepository;
import com.dndmasters.security.SecurityUtils;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Administration API. Every endpoint requires the ADMIN role. */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserRepository users;
    private final CampaignRepository campaigns;
    private final CharacterRepository characters;
    private final CampaignMemberRepository members;

    public AdminController(UserRepository users, CampaignRepository campaigns, CharacterRepository characters,
                           CampaignMemberRepository members) {
        this.users = users;
        this.campaigns = campaigns;
        this.characters = characters;
        this.members = members;
    }

    private UserEntity requireAdmin() {
        Long id = SecurityUtils.requireUserId();
        UserEntity user = users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado"));
        if (!"ADMIN".equals(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo los administradores pueden hacer esto");
        }
        return user;
    }

    @GetMapping("/users")
    public List<Map<String, Object>> users() {
        requireAdmin();
        List<Map<String, Object>> result = new ArrayList<>();
        for (UserEntity user : users.findAll()) {
            Map<String, Object> view = new LinkedHashMap<>();
            view.put("id", user.getId());
            view.put("username", user.getUsername());
            view.put("displayName", user.getDisplayName());
            view.put("role", user.getRole());
            view.put("avatar", user.getAvatar());
            view.put("characterCount", characters.findByOwnerIdOrderByIdAsc(user.getId()).size());
            result.add(view);
        }
        return result;
    }

    @GetMapping("/campaigns")
    public List<Map<String, Object>> campaigns() {
        requireAdmin();
        List<Map<String, Object>> result = new ArrayList<>();
        for (CampaignEntity campaign : campaigns.findAllByOrderByIdDesc()) {
            Map<String, Object> view = new LinkedHashMap<>();
            view.put("id", campaign.getId());
            view.put("name", campaign.getName());
            view.put("edition", campaign.getEdition());
            view.put("status", campaign.getStatus());
            view.put("memberCount", members.findByCampaignIdOrderByIdAsc(campaign.getId()).size());
            view.put("owner", ownerName(campaign.getCreatedBy()));
            result.add(view);
        }
        return result;
    }

    @GetMapping("/characters")
    public List<Map<String, Object>> characters() {
        requireAdmin();
        List<Map<String, Object>> result = new ArrayList<>();
        for (CharacterEntity character : characters.findAll()) {
            Map<String, Object> view = new LinkedHashMap<>();
            view.put("id", character.getId());
            view.put("name", character.getName());
            view.put("race", character.getRace());
            view.put("cls", character.getCls());
            view.put("level", character.getLevel());
            view.put("hp", character.getHp());
            view.put("maxHp", character.getMaxHp());
            view.put("predefined", character.isPredefined());
            view.put("owner", ownerName(character.getOwnerId()));
            result.add(view);
        }
        return result;
    }

    @DeleteMapping("/characters/{id}")
    public Map<String, Object> deleteCharacter(@PathVariable Long id) {
        requireAdmin();
        characters.deleteById(id);
        return Map.of("deleted", id);
    }

    @DeleteMapping("/campaigns/{id}")
    public Map<String, Object> deleteCampaign(@PathVariable Long id) {
        requireAdmin();
        members.findByCampaignIdOrderByIdAsc(id).forEach(member -> members.deleteById(member.getId()));
        campaigns.deleteById(id);
        return Map.of("deleted", id);
    }

    @DeleteMapping("/users/{id}")
    public Map<String, Object> deleteUser(@PathVariable Long id) {
        UserEntity admin = requireAdmin();
        if (admin.getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No puedes eliminarte a ti mismo");
        }
        characters.findByOwnerIdOrderByIdAsc(id).forEach(character -> characters.deleteById(character.getId()));
        users.deleteById(id);
        return Map.of("deleted", id);
    }

    private String ownerName(Long userId) {
        if (userId == null) {
            return "—";
        }
        return users.findById(userId).map(UserEntity::getDisplayName).orElse("—");
    }
}
