package com.dndmasters.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.dndmasters.domain.Ability;
import com.dndmasters.domain.Attack;
import com.dndmasters.domain.Campaign;
import com.dndmasters.domain.Cell;
import com.dndmasters.domain.EntityKind;
import com.dndmasters.domain.GameEntity;
import com.dndmasters.domain.GameState;
import com.dndmasters.domain.InventoryItem;
import com.dndmasters.domain.PlayerCharacter;
import com.dndmasters.domain.Stats;
import com.dndmasters.engine.character.CharacterBuildRequest;
import com.dndmasters.persistence.entity.CharacterEntity;
import com.dndmasters.persistence.mapper.CharacterMapper;
import com.dndmasters.persistence.repository.CharacterRepository;
import com.dndmasters.rules.Edition;
import com.dndmasters.service.action.ActionResult;
import com.dndmasters.service.action.PlayerAction;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/** R-18: ranged player weapon attacks consume ammunition from the persisted character sheet. */
@SpringBootTest
@TestPropertySource(properties = "spring.datasource.url=jdbc:sqlite:target/dnd-test.db")
class AmmunitionIntegrationTest {

    private static final String ARROWS = "Flechas";

    @Autowired
    GameService game;

    @Autowired
    CampaignService campaigns;

    @Autowired
    CombatService combat;

    @Autowired
    CharacterService characterService;

    @Autowired
    CharacterRepository characters;

    @Autowired
    CharacterMapper characterMapper;

    private PlayerCharacter createArcher(long userId, String name) {
        Map<Ability, Integer> scores = new EnumMap<>(Ability.class);
        scores.put(Ability.STRENGTH, 12);
        scores.put(Ability.DEXTERITY, 15);
        scores.put(Ability.CONSTITUTION, 14);
        scores.put(Ability.INTELLIGENCE, 10);
        scores.put(Ability.WISDOM, 10);
        scores.put(Ability.CHARISMA, 8);
        CharacterBuildRequest request = new CharacterBuildRequest(
                name, Edition.DND_5E_2014, "human", "fighter", "soldier",
                scores, List.of("athletics", "perception"), "leather", "shortbow");
        return characterService.create(userId, request);
    }

    private Campaign campaignWithArcher(long userId, String name) {
        PlayerCharacter archer = createArcher(userId, name);
        Campaign campaign = game.createCampaign(userId, name);
        game.joinCampaign(campaign.getId(), userId, archer.getId());
        return campaign;
    }

    private GameEntity player(Campaign campaign) {
        return campaigns.state(campaign.getId()).getEntities().stream()
                .filter(entity -> entity.getKind() == EntityKind.PLAYER)
                .findFirst().orElseThrow();
    }

    private GameEntity createEnemy(Campaign campaign, GameEntity actor, String name, int dx, int dy) {
        GameState state = campaigns.state(campaign.getId());
        GameEntity enemy = new GameEntity();
        enemy.setSceneId(state.getScene().id());
        enemy.setMapId(state.getMap().getId());
        enemy.setKind(EntityKind.ENEMY);
        enemy.setName(name);
        enemy.setHp(999);
        enemy.setMaxHp(999);
        enemy.setAc(10);
        enemy.setSpeed(6);
        enemy.setAlive(true);
        enemy.moveTo(new Cell(actor.getX() + dx, actor.getY() + dy));
        campaigns.saveEntity(enemy);
        return campaigns.state(campaign.getId()).getEntities().stream()
                .filter(entity -> entity.getKind() == EntityKind.ENEMY && name.equals(entity.getName()))
                .findFirst().orElseThrow();
    }

    private PlayerAction attack(Long actorId, Long targetId, String attackName) {
        PlayerAction action = new PlayerAction();
        action.setType(PlayerAction.ATTACK);
        action.setActorEntityId(actorId);
        action.setTargetEntityId(targetId);
        action.setAttack(attackName);
        return action;
    }

    private PlayerAction cast(String spell, Long actorId, Long targetId) {
        PlayerAction action = new PlayerAction();
        action.setType(PlayerAction.CAST_SPELL);
        action.setActorEntityId(actorId);
        action.setTargetEntityId(targetId);
        action.setSpell(spell);
        return action;
    }

    private void setArrows(GameEntity actor, int quantity) {
        CharacterEntity stored = characters.findById(actor.getRefId()).orElseThrow();
        PlayerCharacter character = characterMapper.toDomain(stored);
        List<InventoryItem> inventory = new ArrayList<>(character.getInventory().stream()
                .filter(item -> !ARROWS.equalsIgnoreCase(item.name()))
                .toList());
        if (quantity > 0) {
            inventory.add(new InventoryItem(ARROWS, "Munición para arcos.", "crosshair", quantity, 0.05));
        }
        character.setInventory(inventory);
        CharacterEntity updated = characterMapper.toEntity(character);
        updated.setOwnerId(stored.getOwnerId());
        updated.setCreatedAt(stored.getCreatedAt());
        characters.save(updated);
    }

    private int arrows(Campaign campaign, GameEntity actor) {
        return campaigns.partyCharacters(campaign.getId()).stream()
                .filter(character -> character.getId().equals(actor.getRefId()))
                .flatMap(character -> character.getInventory().stream())
                .filter(item -> ARROWS.equalsIgnoreCase(item.name()))
                .mapToInt(InventoryItem::quantity)
                .findFirst()
                .orElse(0);
    }

    @Test
    void rangedWeaponAttackConsumesOneArrow() {
        long userId = 9801L;
        Campaign campaign = campaignWithArcher(userId, "Ammo Basic");
        GameEntity player = player(campaign);
        setArrows(player, 20);
        GameEntity target = createEnemy(campaign, player, "Goblin", 2, 0);

        ActionResult result = game.act(campaign.getId(), userId,
                attack(player.getId(), target.getId(), "Arco corto"));

        assertThat(result.ok()).as(result.message()).isTrue();
        assertThat(result.events()).anyMatch(event ->
                "ATTACK".equals(event.type()) || "ATTACK_MISS".equals(event.type()));
        assertThat(arrows(campaign, player)).isEqualTo(19);
    }

    @Test
    void lastArrowIsConsumedAndTheFollowingAttackIsRejected() {
        long userId = 9802L;
        Campaign campaign = campaignWithArcher(userId, "Ammo Last");
        GameEntity player = player(campaign);
        setArrows(player, 1);
        GameEntity target = createEnemy(campaign, player, "Goblin", 2, 0);

        ActionResult first = game.act(campaign.getId(), userId,
                attack(player.getId(), target.getId(), "Arco corto"));
        assertThat(first.ok()).as(first.message()).isTrue();
        assertThat(arrows(campaign, player)).isZero();

        campaigns.resetTurnUsage(player.getId());
        int hpBefore = campaigns.entity(target.getId()).orElseThrow().getHp();
        ActionResult second = game.act(campaign.getId(), userId,
                attack(player.getId(), target.getId(), "Arco corto"));

        assertThat(second.ok()).isFalse();
        assertThat(second.message()).containsIgnoringCase("flechas");
        assertThat(campaigns.entity(target.getId()).orElseThrow().getHp()).isEqualTo(hpBefore);
        assertThat(campaigns.entity(player.getId()).orElseThrow().getData().get("actionUsed"))
                .isNotEqualTo(true);
    }

    @Test
    void attackWithoutAmmunitionIsRejectedBeforeSpendingTheAction() {
        long userId = 9803L;
        Campaign campaign = campaignWithArcher(userId, "Ammo None");
        GameEntity player = player(campaign);
        setArrows(player, 0);
        GameEntity target = createEnemy(campaign, player, "Goblin", 2, 0);
        int hpBefore = target.getHp();

        ActionResult result = game.act(campaign.getId(), userId,
                attack(player.getId(), target.getId(), "Arco corto"));

        assertThat(result.ok()).isFalse();
        assertThat(result.message()).containsIgnoringCase("flechas");
        assertThat(campaigns.entity(player.getId()).orElseThrow().getData().get("actionUsed"))
                .isNotEqualTo(true);
        assertThat(campaigns.entity(target.getId()).orElseThrow().getHp()).isEqualTo(hpBefore);
    }

    @Test
    void meleeWeaponAttackDoesNotConsumeArrows() {
        long userId = 9804L;
        Campaign campaign = campaignWithArcher(userId, "Ammo Melee");
        GameEntity player = player(campaign);
        setArrows(player, 5);
        player.getData().put("attacks", List.of(Attack.melee("Espada corta", 100, "1d6+3")));
        campaigns.saveEntity(player);
        GameEntity target = createEnemy(campaign, player, "Goblin", 1, 0);

        ActionResult result = game.act(campaign.getId(), userId,
                attack(player.getId(), target.getId(), "Espada corta"));

        assertThat(result.ok()).as(result.message()).isTrue();
        assertThat(arrows(campaign, player)).isEqualTo(5);
    }

    @Test
    void spellAttackDoesNotConsumeArrows() {
        long userId = 9805L;
        Campaign campaign = campaignWithArcher(userId, "Ammo Spell");
        GameEntity player = player(campaign);
        setArrows(player, 5);
        player.getData().put("classId", "wizard");
        player.getData().put("class", "wizard");
        player.getData().put("level", 1);
        player.getData().put("spellSlotsUsed", 0);
        player.getData().put("spellFocus", true);
        player.setStats(new Stats(10, 10, 10, 16, 10, 10));
        campaigns.saveEntity(player);
        GameEntity target = createEnemy(campaign, player, "Goblin", 2, 0);

        ActionResult result = game.act(campaign.getId(), userId,
                cast("fire_bolt", player.getId(), target.getId()));

        assertThat(result.ok()).as(result.message()).isTrue();
        assertThat(result.events()).anyMatch(event ->
                "ATTACK".equals(event.type()) || "ATTACK_MISS".equals(event.type()));
        assertThat(arrows(campaign, player)).isEqualTo(5);
    }

    @Test
    void enemyRangedAttackDoesNotConsumePlayerAmmunition() {
        long userId = 9806L;
        Campaign campaign = campaignWithArcher(userId, "Ammo Enemy");
        GameEntity player = player(campaign);
        setArrows(player, 5);
        GameEntity enemy = createEnemy(campaign, player, "Arquero goblin", 2, 0);

        GameState state = campaigns.state(campaign.getId());
        com.dndmasters.domain.CombatState transientState = new com.dndmasters.domain.CombatState();
        transientState.setCampaignId(campaign.getId());
        transientState.setSceneId(state.getScene().id());
        transientState.setActive(false);
        transientState.setParticipants(new ArrayList<>());

        combat.attack(campaign.getId(), transientState, enemy, player,
                Attack.ranged("Arco corto", 5, "1d6", 16));

        assertThat(arrows(campaign, player)).isEqualTo(5);
    }
}
