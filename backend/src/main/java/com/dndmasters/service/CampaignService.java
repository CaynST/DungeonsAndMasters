package com.dndmasters.service;

import com.dndmasters.content.AdventureDefinition;
import com.dndmasters.content.AdventureDefinition.EnemyDefinition;
import com.dndmasters.content.AdventureDefinition.NpcDefinition;
import com.dndmasters.content.AdventureDefinition.Placement;
import com.dndmasters.content.AdventureDefinition.SceneDefinition;
import com.dndmasters.content.ValdrosAdventure;
import com.dndmasters.domain.Campaign;
import com.dndmasters.domain.CampaignStatus;
import com.dndmasters.domain.Cell;
import com.dndmasters.domain.CombatState;
import com.dndmasters.domain.GameEntity;
import com.dndmasters.domain.GameMap;
import com.dndmasters.domain.GamePhase;
import com.dndmasters.domain.GameState;
import com.dndmasters.domain.PlayerCharacter;
import com.dndmasters.domain.Scene;
import com.dndmasters.persistence.entity.BoardEntity;
import com.dndmasters.persistence.entity.CampaignEntity;
import com.dndmasters.persistence.entity.CampaignMemberEntity;
import com.dndmasters.persistence.entity.CharacterEntity;
import com.dndmasters.persistence.entity.CombatParticipantEntity;
import com.dndmasters.persistence.entity.CombatStateEntity;
import com.dndmasters.persistence.entity.EnemyTemplateEntity;
import com.dndmasters.persistence.entity.ItemEntity;
import com.dndmasters.persistence.entity.MapEntity;
import com.dndmasters.persistence.entity.NpcTemplateEntity;
import com.dndmasters.persistence.entity.SceneEntity;
import com.dndmasters.persistence.entity.UserEntity;
import com.dndmasters.persistence.mapper.CampaignMapper;
import com.dndmasters.persistence.mapper.CharacterMapper;
import com.dndmasters.persistence.mapper.CombatMapper;
import com.dndmasters.persistence.mapper.GameEntityMapper;
import com.dndmasters.persistence.mapper.MapMapper;
import com.dndmasters.persistence.repository.BoardEntityRepository;
import com.dndmasters.persistence.repository.CampaignMemberRepository;
import com.dndmasters.persistence.repository.CampaignRepository;
import com.dndmasters.persistence.repository.CharacterRepository;
import com.dndmasters.persistence.repository.CombatParticipantRepository;
import com.dndmasters.persistence.repository.CombatStateRepository;
import com.dndmasters.persistence.repository.EnemyTemplateRepository;
import com.dndmasters.persistence.repository.ItemRepository;
import com.dndmasters.persistence.repository.MapEntityRepository;
import com.dndmasters.persistence.repository.NpcTemplateRepository;
import com.dndmasters.persistence.repository.SceneRepository;
import com.dndmasters.persistence.repository.UserRepository;
import com.dndmasters.rules.Edition;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owns campaign CRUD, content instantiation, party placement and state assembly. It is the
 * source of truth the combat, DM and controller layers read from.
 */
@Service
public class CampaignService {

    private final CampaignRepository campaigns;
    private final CampaignMemberRepository members;
    private final CharacterRepository characters;
    private final UserRepository users;
    private final SceneRepository scenes;
    private final MapEntityRepository maps;
    private final BoardEntityRepository boardEntities;
    private final NpcTemplateRepository npcs;
    private final EnemyTemplateRepository enemies;
    private final ItemRepository items;
    private final CombatStateRepository combatStates;
    private final CombatParticipantRepository combatParticipants;
    private final CampaignMapper campaignMapper;
    private final CharacterMapper characterMapper;
    private final MapMapper mapMapper;
    private final GameEntityMapper entityMapper;
    private final CombatMapper combatMapper;
    private final GameEventService events;
    private final com.dndmasters.adventure.AdventureGenerator adventureGenerator;
    private final java.util.Random random = new java.util.Random();

    public CampaignService(CampaignRepository campaigns, CampaignMemberRepository members,
                           CharacterRepository characters, UserRepository users, SceneRepository scenes,
                           MapEntityRepository maps, BoardEntityRepository boardEntities, NpcTemplateRepository npcs,
                           EnemyTemplateRepository enemies, ItemRepository items, CombatStateRepository combatStates,
                           CombatParticipantRepository combatParticipants, CampaignMapper campaignMapper,
                           CharacterMapper characterMapper, MapMapper mapMapper, GameEntityMapper entityMapper,
                           CombatMapper combatMapper, GameEventService events,
                           com.dndmasters.adventure.AdventureGenerator adventureGenerator) {
        this.campaigns = campaigns;
        this.members = members;
        this.characters = characters;
        this.users = users;
        this.scenes = scenes;
        this.maps = maps;
        this.boardEntities = boardEntities;
        this.npcs = npcs;
        this.enemies = enemies;
        this.items = items;
        this.combatStates = combatStates;
        this.combatParticipants = combatParticipants;
        this.campaignMapper = campaignMapper;
        this.characterMapper = characterMapper;
        this.mapMapper = mapMapper;
        this.entityMapper = entityMapper;
        this.combatMapper = combatMapper;
        this.events = events;
        this.adventureGenerator = adventureGenerator;
    }

    @Transactional(readOnly = true)
    public List<Campaign> list() {
        return campaigns.findAllByOrderByIdDesc().stream().map(e -> {
            Campaign c = campaignMapper.toDomain(e);
            c.setMembers(memberViews(e.getId()));
            return c;
        }).toList();
    }

    @Transactional(readOnly = true)
    public Campaign get(Long campaignId) {
        CampaignEntity entity = requireCampaign(campaignId);
        Campaign c = campaignMapper.toDomain(entity);
        c.setMembers(memberViews(campaignId));
        return c;
    }

    @Transactional(readOnly = true)
    public CampaignEntity requireCampaign(Long campaignId) {
        return campaigns.findById(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("Campaign not found: " + campaignId));
    }

    /** Creates a new campaign pre-populated with the Valdros one-shot content. */
    @Transactional
    public Campaign create(Long userId, String name, String editionId) {
        AdventureDefinition adventure = adventureGenerator.materialize(adventureGenerator.generate(random), random);
        Edition edition = Edition.fromId(editionId);
        String now = Instant.now().toString();

        CampaignEntity campaign = new CampaignEntity();
        campaign.setName(name == null || name.isBlank() ? adventure.name() : name);
        campaign.setSetting(adventure.setting());
        campaign.setDescription(adventure.description());
        campaign.setDmName(adventure.dmName());
        campaign.setDmAvatar("/images/dm/dungeon-master.png");
        campaign.setImage("/images/campaigns/valdros.png");
        campaign.setStatus(CampaignStatus.LOBBY.name());
        campaign.setPhase(GamePhase.INTRO.name());
        campaign.setEdition(edition.name());
        campaign.setPremise(adventure.premise() == null ? null : String.join("\n", adventure.premise()));
        campaign.setVillain(adventure.villain());
        campaign.setObjective(adventure.objective());
        campaign.setCreatedBy(userId);
        campaign.setCreatedAt(now);
        campaign.setUpdatedAt(now);
        campaigns.save(campaign);

        Map<String, Long> enemyIds = new LinkedHashMap<>();
        for (EnemyDefinition ed : adventure.enemies()) {
            EnemyTemplateEntity e = new EnemyTemplateEntity();
            e.setCampaignId(campaign.getId());
            e.setName(ed.name());
            e.setHp(ed.hp());
            e.setMaxHp(ed.hp());
            e.setAc(ed.ac());
            e.setSpeed(ed.speed());
            e.setAttacksJson(com.dndmasters.util.Json.write(ed.attacks()));
            e.setBehavior(ed.behavior());
            e.setXp(50);
            e.setPortrait(ed.portrait());
            enemies.save(e);
            enemyIds.put(ed.name(), e.getId());
        }

        Map<String, Long> npcIds = new LinkedHashMap<>();
        for (NpcDefinition nd : adventure.npcs()) {
            NpcTemplateEntity n = new NpcTemplateEntity();
            n.setCampaignId(campaign.getId());
            n.setName(nd.name());
            n.setDescription(nd.description());
            n.setPortrait(nd.portrait());
            n.setDisposition("NEUTRAL");
            n.setDialogueJson(com.dndmasters.util.Json.write(nd.dialogue()));
            npcs.save(n);
            npcIds.put(nd.name(), n.getId());
        }

        Map<String, ItemEntity> itemEntities = new LinkedHashMap<>();
        for (AdventureDefinition.ItemDefinition id : adventure.items()) {
            ItemEntity item = new ItemEntity();
            item.setCampaignId(campaign.getId());
            item.setName(id.name());
            item.setDescription(id.description());
            item.setKind(id.kind());
            item.setIcon(id.icon());
            items.save(item);
            itemEntities.put(id.name(), item);
        }

        Long firstSceneId = null;
        for (SceneDefinition def : adventure.scenes()) {
            MapEntity map = mapMapper.toEntity(def.map(), campaign.getId());
            maps.save(map);

            SceneEntity scene = new SceneEntity();
            scene.setCampaignId(campaign.getId());
            scene.setOrderIndex(def.orderIndex());
            scene.setTitle(def.title());
            scene.setDescription(def.description());
            scene.setKind(def.kind());
            scene.setImagePrompt(def.imagePrompt());
            scene.setImageUrl("/images/scenes/valdros-" + def.orderIndex() + ".png");
            scene.setMapId(map.getId());
            scenes.save(scene);
            if (firstSceneId == null) {
                firstSceneId = scene.getId();
            }

            for (Placement placement : def.placements()) {
                createPlacement(campaign.getId(), scene.getId(), map.getId(), placement, adventure, enemyIds, npcIds, itemEntities);
            }
        }
        campaign.setActiveSceneId(firstSceneId);
        campaigns.save(campaign);
        return get(campaign.getId());
    }

    private void createPlacement(Long campaignId, Long sceneId, Long mapId, Placement placement,
                                 AdventureDefinition adventure, Map<String, Long> enemyIds,
                                 Map<String, Long> npcIds, Map<String, ItemEntity> itemsByName) {
        switch (placement.kind()) {
            case "ENEMY" -> {
                EnemyDefinition ed = adventure.enemies().stream()
                        .filter(e -> e.name().equals(placement.ref())).findFirst().orElseThrow();
                Map<String, Object> data = new HashMap<>();
                data.put("stats", ed.stats());
                data.put("attacks", ed.attacks());
                data.put("behavior", ed.behavior());
                saveEntity(sceneId, mapId, "ENEMY", enemyIds.get(ed.name()), ed.name(), placement,
                        ed.hp(), ed.hp(), ed.ac(), ed.speed(), ed.color(), ed.icon(), ed.portrait(), data);
            }
            case "NPC" -> {
                NpcDefinition nd = adventure.npcs().stream()
                        .filter(n -> n.name().equals(placement.ref())).findFirst().orElseThrow();
                Map<String, Object> data = new HashMap<>();
                data.put("dialogue", nd.dialogue());
                saveEntity(sceneId, mapId, "NPC", npcIds.get(nd.name()), nd.name(), placement,
                        10, 10, 10, 0, "#D8C39A", "ghost", nd.portrait(), data);
            }
            case "ITEM" -> {
                ItemEntity item = itemsByName.get(placement.ref());
                Map<String, Object> data = new HashMap<>();
                data.put("itemKind", item != null ? item.getKind() : "MISC");
                saveEntity(sceneId, mapId, "ITEM", item != null ? item.getId() : null, placement.ref(), placement,
                        1, 1, 0, 0, "#D8C39A", item != null ? item.getIcon() : "gem",
                        null, data);
            }
            default -> throw new IllegalArgumentException("Unknown placement kind: " + placement.kind());
        }
    }

    private void saveEntity(Long sceneId, Long mapId, String kind, Long refId, String name, Placement placement,
                            int hp, int maxHp, int ac, int speed, String color, String icon, String portrait,
                            Map<String, Object> data) {
        BoardEntity entity = new BoardEntity();
        entity.setSceneId(sceneId);
        entity.setMapId(mapId);
        entity.setKind(kind);
        entity.setRefId(refId);
        entity.setName(name);
        entity.setX(placement.x());
        entity.setY(placement.y());
        entity.setHp(hp);
        entity.setMaxHp(maxHp);
        entity.setAc(ac);
        entity.setSpeed(speed);
        entity.setColor(color);
        entity.setIcon(icon);
        entity.setPortrait(portrait);
        entity.setAlive(true);
        entity.setDataJson(com.dndmasters.util.Json.write(data));
        boardEntities.save(entity);
    }

    @Transactional
    public Campaign join(Long campaignId, Long userId, Long characterId) {
        CampaignEntity campaign = requireCampaign(campaignId);
        if (members.existsByCampaignIdAndUserId(campaignId, userId)) {
            return get(campaignId);
        }
        Long chosen = characterId;
        if (chosen == null) {
            chosen = firstAvailablePredefinedCharacter(campaignId).map(CharacterEntity::getId).orElse(null);
        }
        if (chosen == null) {
            throw new IllegalStateException("No character available to join with");
        }
        CampaignMemberEntity member = new CampaignMemberEntity();
        member.setCampaignId(campaignId);
        member.setUserId(userId);
        member.setCharacterId(chosen);
        member.setRole("PLAYER");
        member.setReady(true);
        member.setJoinedAt(Instant.now().toString());
        members.save(member);

        if (campaign.getActiveSceneId() != null) {
            ensurePartyInScene(campaignId, campaign.getActiveSceneId());
        }
        events.record(campaignId, campaign.getActiveSceneId(), "PLAYER_JOINED", displayName(userId),
                displayName(userId) + " se une a la partida.", Map.of());
        return get(campaignId);
    }

    private Optional<CharacterEntity> firstAvailablePredefinedCharacter(Long campaignId) {
        List<Long> taken = members.findByCampaignIdOrderByIdAsc(campaignId).stream()
                .map(CampaignMemberEntity::getCharacterId).filter(java.util.Objects::nonNull).toList();
        return characters.findByPredefinedTrueOrderByIdAsc().stream()
                .filter(c -> !taken.contains(c.getId()))
                .findFirst();
    }

    @Transactional
    public GameState start(Long campaignId, Long userId) {
        CampaignEntity campaign = requireCampaign(campaignId);
        if (!members.existsByCampaignIdAndUserId(campaignId, userId)) {
            join(campaignId, userId, null);
            campaign = requireCampaign(campaignId);
        }
        SceneEntity first = scenes.findFirstByCampaignIdOrderByOrderIndexAsc(campaignId).orElseThrow();
        campaign.setStatus(CampaignStatus.ACTIVE.name());
        campaign.setPhase(GamePhase.EXPLORATION.name());
        campaign.setActiveSceneId(first.getId());
        campaign.setUpdatedAt(Instant.now().toString());
        campaigns.save(campaign);

        ensurePartyInScene(campaignId, first.getId());
        events.record(campaignId, first.getId(), "CAMPAIGN_STARTED", "Sistema",
                "La compañía llega a " + campaign.getName() + ".", Map.of());
        return state(campaignId);
    }

    /** Free cells near the left-middle of the scene's map, used to place the party. */
    private List<Cell> freeSpawnCells(SceneEntity scene) {
        GameMap map = scene.getMapId() == null ? null
                : maps.findById(scene.getMapId()).map(mapMapper::toDomain).orElse(null);
        if (map == null) {
            return List.of(new Cell(1, 1));
        }
        int midY = map.getHeight() / 2;
        List<Cell> result = new ArrayList<>();
        for (int dx = 1; dx < 5 && result.size() < 6; dx++) {
            for (int dy = -2; dy <= 2 && result.size() < 6; dy++) {
                Cell c = new Cell(dx, midY + dy);
                if (map.inBounds(c) && !map.blocksMovement(c) && !result.contains(c)) {
                    result.add(c);
                }
            }
        }
        if (result.isEmpty()) {
            result.add(new Cell(1, midY));
        }
        return result;
    }

    /** Creates a player/ally token for every party member that is missing from the scene. */
    @Transactional
    public void ensurePartyInScene(Long campaignId, Long sceneId) {
        SceneEntity scene = scenes.findById(sceneId).orElseThrow();
        List<Cell> spawns = freeSpawnCells(scene);
        Map<Long, String> controllerByCharacter = controllerByCharacter(campaignId);
        List<PlayerCharacter> party = partyCharacters(campaignId);
        List<BoardEntity> existing = boardEntities.findBySceneIdOrderByIdAsc(sceneId);
        int index = 0;
        for (PlayerCharacter character : party) {
            boolean present = existing.stream()
                    .anyMatch(e -> "PLAYER".equals(e.getKind()) && character.getId().equals(e.getRefId()));
            if (present) {
                index++;
                continue;
            }
            Cell spawn = spawns.get(index % spawns.size());
            Map<String, Object> data = new HashMap<>();
            String controller = controllerByCharacter.getOrDefault(character.getId(), "AI");
            data.put("controller", controller);
            data.put("stats", character.getStats());
            data.put("attacks", character.getAttacks());
            data.put("class", character.getCls());
            data.put("race", character.getRace());
            data.put("ownerId", controllerByCharacterOwner(campaignId).get(character.getId()));
            saveEntity(sceneId, scene.getMapId(), "PLAYER", character.getId(), character.getName(),
                    new Placement("PLAYER", character.getName(), spawn.x(), spawn.y()),
                    character.getHp(), character.getMaxHp(), character.getAc(), character.getSpeed(),
                    character.getColor(), character.getIcon(), character.getPortrait(), data);
            index++;
        }
    }

    @Transactional(readOnly = true)
    public GameState state(Long campaignId) {
        CampaignEntity campaignEntity = requireCampaign(campaignId);
        Campaign campaign = campaignMapper.toDomain(campaignEntity);
        campaign.setMembers(memberViews(campaignId));

        SceneEntity sceneEntity = campaignEntity.getActiveSceneId() == null ? null
                : scenes.findById(campaignEntity.getActiveSceneId()).orElse(null);
        Scene scene = sceneEntity == null ? null : new Scene(
                sceneEntity.getId(), campaignId, sceneEntity.getOrderIndex(), sceneEntity.getTitle(),
                sceneEntity.getDescription(), sceneEntity.getKind(), sceneEntity.getImagePrompt(),
                sceneEntity.getImageUrl(), sceneEntity.getMapId());

        GameMap map = scene != null && scene.mapId() != null
                ? maps.findById(scene.mapId()).map(mapMapper::toDomain).orElse(null) : null;

        List<GameEntity> entities = scene == null ? List.of()
                : boardEntities.findBySceneIdOrderByIdAsc(scene.id()).stream().map(entityMapper::toDomain).toList();

        CombatState combat = loadCombat(campaignId, entities);
        List<PlayerCharacter> party = partyCharacters(campaignId);

        GameState state = new GameState();
        state.setCampaign(campaign);
        state.setScene(scene);
        state.setMap(map);
        state.setEntities(new ArrayList<>(entities));
        state.setParty(party);
        state.setCombat(combat);
        state.setRecentEvents(events.recent(campaignId));
        state.setObjective(campaign.getObjective());
        return state;
    }

    private CombatState loadCombat(Long campaignId, List<GameEntity> sceneEntities) {
        Optional<CombatStateEntity> stateEntity = combatStates.findFirstByCampaignIdAndActiveTrue(campaignId);
        if (stateEntity.isEmpty()) {
            return null;
        }
        List<CombatParticipantEntity> participants = combatParticipants.findByCombatIdOrderByOrderIndexAsc(stateEntity.get().getId());
        Map<Long, GameEntity> lookup = new HashMap<>();
        sceneEntities.forEach(e -> lookup.put(e.getId(), e));
        return combatMapper.toDomain(stateEntity.get(), participants, lookup);
    }

    @Transactional(readOnly = true)
    public CombatState activeCombat(Long campaignId) {
        Optional<CombatStateEntity> stateEntity = combatStates.findFirstByCampaignIdAndActiveTrue(campaignId);
        if (stateEntity.isEmpty()) {
            return null;
        }
        Long sceneId = stateEntity.get().getSceneId();
        List<GameEntity> sceneEntities = sceneId == null ? List.of() : entitiesInScene(sceneId);
        return loadCombat(campaignId, sceneEntities);
    }

    @Transactional(readOnly = true)
    public GameMap mapForScene(Long sceneId) {
        SceneEntity scene = scenes.findById(sceneId).orElse(null);
        if (scene == null || scene.getMapId() == null) {
            return null;
        }
        return maps.findById(scene.getMapId()).map(mapMapper::toDomain).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<PlayerCharacter> partyCharacters(Long campaignId) {
        // Only characters actually chosen by joined players. No demo characters are injected.
        List<PlayerCharacter> result = new ArrayList<>();
        for (CampaignMemberEntity member : members.findByCampaignIdOrderByIdAsc(campaignId)) {
            if (member.getCharacterId() != null) {
                characters.findById(member.getCharacterId()).map(characterMapper::toDomain).ifPresent(result::add);
            }
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<GameEntity> entitiesInScene(Long sceneId) {
        return boardEntities.findBySceneIdOrderByIdAsc(sceneId).stream().map(entityMapper::toDomain).toList();
    }

    @Transactional(readOnly = true)
    public Optional<GameEntity> entity(Long entityId) {
        return boardEntities.findById(entityId).map(entityMapper::toDomain);
    }

    @Transactional
    public void saveEntity(GameEntity gameEntity) {
        boardEntities.save(entityMapper.toEntity(gameEntity));
    }

    @Transactional
    public void saveEntities(List<GameEntity> gameEntities) {
        gameEntities.forEach(this::saveEntity);
    }

    @Transactional
    public void updateCampaignPhase(Long campaignId, GamePhase phase, Long sceneId) {
        CampaignEntity campaign = requireCampaign(campaignId);
        campaign.setPhase(phase.name());
        if (sceneId != null) {
            campaign.setActiveSceneId(sceneId);
        }
        campaign.setUpdatedAt(Instant.now().toString());
        campaigns.save(campaign);
    }

    @Transactional
    public void updateCampaignStatus(Long campaignId, CampaignStatus status) {
        CampaignEntity campaign = requireCampaign(campaignId);
        campaign.setStatus(status.name());
        campaign.setUpdatedAt(Instant.now().toString());
        campaigns.save(campaign);
    }

    @Transactional(readOnly = true)
    public Long firstSceneId(Long campaignId) {
        return scenes.findFirstByCampaignIdOrderByOrderIndexAsc(campaignId).map(SceneEntity::getId).orElse(null);
    }

    @Transactional(readOnly = true)
    public Long sceneIdByOrder(Long campaignId, int order) {
        return scenes.findByCampaignIdAndOrderIndex(campaignId, order).map(SceneEntity::getId).orElse(null);
    }

    @Transactional(readOnly = true)
    public Long sceneOrder(Long sceneId) {
        return scenes.findById(sceneId).map(s -> (long) s.getOrderIndex()).orElse(0L);
    }

    @Transactional(readOnly = true)
    public Map<Long, String> controllerByCharacter(Long campaignId) {
        Map<Long, String> map = new HashMap<>();
        for (CampaignMemberEntity member : members.findByCampaignIdOrderByIdAsc(campaignId)) {
            if (member.getCharacterId() != null) {
                map.put(member.getCharacterId(), "PLAYER");
            }
        }
        return map;
    }

    @Transactional(readOnly = true)
    public Map<Long, Long> controllerByCharacterOwner(Long campaignId) {
        Map<Long, Long> map = new HashMap<>();
        members.findByCampaignIdOrderByIdAsc(campaignId).forEach(m -> {
            if (m.getCharacterId() != null) {
                map.put(m.getCharacterId(), m.getUserId());
            }
        });
        return map;
    }

    @Transactional(readOnly = true)
    public Long characterIdForUser(Long campaignId, Long userId) {
        return members.findByCampaignIdAndUserId(campaignId, userId)
                .map(CampaignMemberEntity::getCharacterId).orElse(null);
    }

    /** Advances the narrative spotlight to the next party member and returns its index. */
    @Transactional
    public int rotateSpotlight(Long campaignId) {
        CampaignEntity campaign = requireCampaign(campaignId);
        int size = Math.max(1, partyCharacters(campaignId).size());
        int next = (campaign.getSpotlightIndex() + 1) % size;
        campaign.setSpotlightIndex(next);
        campaigns.save(campaign);
        return next;
    }

    @Transactional(readOnly = true)
    public String spotlightName(Long campaignId) {
        List<PlayerCharacter> party = partyCharacters(campaignId);
        if (party.isEmpty()) {
            return null;
        }
        CampaignEntity campaign = requireCampaign(campaignId);
        int idx = Math.floorMod(campaign.getSpotlightIndex(), party.size());
        return party.get(idx).getName();
    }

    private List<Campaign.CampaignMember> memberViews(Long campaignId) {
        List<Campaign.CampaignMember> views = new ArrayList<>();
        for (CampaignMemberEntity m : members.findByCampaignIdOrderByIdAsc(campaignId)) {
            String display = users.findById(m.getUserId()).map(UserEntity::getDisplayName).orElse("Aventurero");
            String characterName = m.getCharacterId() == null ? null
                    : characters.findById(m.getCharacterId()).map(CharacterEntity::getName).orElse(null);
            views.add(new Campaign.CampaignMember(m.getId(), m.getUserId(), display, m.getCharacterId(),
                    characterName, m.getRole(), m.isReady()));
        }
        return views;
    }

    private String displayName(Long userId) {
        return users.findById(userId).map(UserEntity::getDisplayName).orElse("Aventurero");
    }
}
