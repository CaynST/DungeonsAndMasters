package com.dndmasters.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.dndmasters.domain.Attack;
import com.dndmasters.domain.Campaign;
import com.dndmasters.domain.Cell;
import com.dndmasters.domain.CombatState;
import com.dndmasters.domain.EntityKind;
import com.dndmasters.domain.GameEntity;
import com.dndmasters.domain.GameEvent;
import com.dndmasters.domain.GameState;
import com.dndmasters.engine.combat.Condition;
import com.dndmasters.engine.combat.Conditions;
import com.dndmasters.engine.combat.TurnEconomy;
import com.dndmasters.util.Json;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/** Fighting Style bonuses through the real combat pipeline. */
@SpringBootTest
@TestPropertySource(properties = "spring.datasource.url=jdbc:sqlite:target/dnd-test.db")
class FightingStyleIntegrationTest {

    @Autowired
    GameService game;

    @Autowired
    CampaignService campaigns;

    @Autowired
    CombatService combat;

    private Campaign campaignWithPlayer(Long userId, String name) {
        Campaign campaign = game.createCampaign(userId, name);
        game.joinCampaign(campaign.getId(), userId, null);
        return campaign;
    }

    private GameEntity player(Campaign campaign) {
        return campaigns.state(campaign.getId()).getEntities().stream()
                .filter(entity -> entity.getKind() == EntityKind.PLAYER)
                .findFirst().orElseThrow();
    }

    private GameEntity createEnemy(Campaign campaign, GameEntity player) {
        return createEntity(campaign, player, EntityKind.ENEMY, "Dummy de estilo", 1, 0, 999);
    }

    /** Creates a scene entity next to the anchor player so combat helpers can find it. */
    private GameEntity createEntity(Campaign campaign, GameEntity anchor, EntityKind kind,
                                    String name, int dx, int dy, int hp) {
        GameState state = campaigns.state(campaign.getId());
        GameEntity entity = new GameEntity();
        entity.setSceneId(state.getScene().id());
        entity.setMapId(state.getMap().getId());
        entity.setKind(kind);
        entity.setName(name);
        entity.setHp(hp);
        entity.setMaxHp(hp);
        entity.setAc(10);
        entity.setSpeed(6);
        entity.setAlive(true);
        entity.getData().put("ownerId", anchor.getData().get("ownerId"));
        entity.getData().put("attacks", List.of(Map.of(
                "name", "Prueba", "attackBonus", 5, "damageExpression", "1d4",
                "range", 1, "ranged", false)));
        entity.moveTo(new Cell(anchor.getX() + dx, anchor.getY() + dy));
        campaigns.saveEntity(entity);
        return campaigns.state(campaign.getId()).getEntities().stream()
                .filter(candidate -> candidate.getKind() == kind && name.equals(candidate.getName()))
                .findFirst().orElseThrow();
    }

    private GameEntity createProtector(Campaign campaign, GameEntity anchor, String name, int dx, int dy) {
        GameEntity protector = createEntity(campaign, anchor, EntityKind.PLAYER, name, dx, dy, 30);
        protector.getData().put("fightingStyle", "protection");
        campaigns.saveEntity(protector);
        return campaigns.entity(protector.getId()).orElseThrow();
    }

    private CombatState transientState(Campaign campaign, GameState state) {
        CombatState combatState = new CombatState();
        combatState.setCampaignId(campaign.getId());
        combatState.setSceneId(state.getScene().id());
        combatState.setActive(false);
        combatState.setParticipants(new ArrayList<>());
        return combatState;
    }

    private Map<String, Object> lastAttackData(List<GameEvent> events) {
        return events.stream()
                .filter(event -> "ATTACK".equals(event.type()))
                .findFirst()
                .map(event -> Json.readMap(event.dataJson()))
                .orElse(Map.of());
    }

    @Test
    void predefinedKaelCarriesTheDuelingStyle() {
        Campaign campaign = campaignWithPlayer(9501L, "Style Predefined");
        GameEntity player = player(campaign);

        assertThat(player.getData().get("fightingStyle")).isEqualTo("dueling");
    }

    @Test
    void archeryAddsTwoToRangedWeaponAttacks() {
        Campaign campaign = campaignWithPlayer(9502L, "Style Archery");
        GameEntity player = player(campaign);
        player.getData().put("fightingStyle", "archery");
        campaigns.saveEntity(player);
        GameEntity enemy = createEnemy(campaign, player);
        CombatState state = transientState(campaign, campaigns.state(campaign.getId()));

        List<GameEvent> events = combat.attack(campaign.getId(), state, player, enemy,
                Attack.ranged("Arco", 100, "1d6", 16));

        GameEvent attackEvent = events.stream()
                .filter(event -> "ATTACK".equals(event.type()) || "ATTACK_MISS".equals(event.type()))
                .findFirst().orElseThrow();
        Map<String, Object> data = Json.readMap(attackEvent.dataJson());
        assertThat(data.get("fightingStyleAttackBonus")).isEqualTo(2);
    }

    @Test
    void duelingAddsTwoToOneHandedMeleeDamageButNotToTwoHandedWeapons() {
        Campaign campaign = campaignWithPlayer(9503L, "Style Dueling");
        GameEntity player = player(campaign);
        player.getData().put("fightingStyle", "dueling");
        campaigns.saveEntity(player);
        GameEntity enemy = createEnemy(campaign, player);
        CombatState state = transientState(campaign, campaigns.state(campaign.getId()));

        boolean sawOneHanded = false;
        for (int attempt = 0; attempt < 10 && !sawOneHanded; attempt++) {
            Map<String, Object> data = lastAttackData(combat.attack(campaign.getId(), state, player, enemy,
                    Attack.melee("Espada", 100, "1d6")));
            if (!data.isEmpty()) {
                sawOneHanded = true;
                assertThat(data.get("duelingBonus")).isEqualTo(2);
            }
        }
        assertThat(sawOneHanded).isTrue();

        boolean sawTwoHanded = false;
        for (int attempt = 0; attempt < 10 && !sawTwoHanded; attempt++) {
            Map<String, Object> data = lastAttackData(combat.attack(campaign.getId(), state, player, enemy,
                    Attack.meleeTwoHanded("Hacha", 100, "1d12")));
            if (!data.isEmpty()) {
                sawTwoHanded = true;
                assertThat(data.get("duelingBonus")).isEqualTo(0);
            }
        }
        assertThat(sawTwoHanded).isTrue();
    }

    private int intValue(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    @Test
    void greatWeaponFightingRerollsLowDiceOnTwoHandedWeaponsAndRecordsTheBonus() {
        Campaign campaign = campaignWithPlayer(9504L, "Style Great Weapon");
        GameEntity player = player(campaign);
        player.getData().put("fightingStyle", "great-weapon");
        campaigns.saveEntity(player);
        GameEntity enemy = createEnemy(campaign, player);
        CombatState state = transientState(campaign, campaigns.state(campaign.getId()));

        Map<String, Object> data = Map.of();
        for (int attempt = 0; attempt < 40 && intValue(data.get("greatWeaponRerolls")) == 0; attempt++) {
            data = lastAttackData(combat.attack(campaign.getId(), state, player, enemy,
                    Attack.meleeTwoHanded("Espadón", 100, "1d12")));
        }

        assertThat(data.get("greatWeaponFighting")).isEqualTo(true);
        assertThat(intValue(data.get("greatWeaponRerolls"))).isPositive();
        assertThat(intValue(data.get("greatWeaponBonus"))).isPositive();
    }

    @Test
    void greatWeaponFightingDoesNotApplyToOneHandedWeapons() {
        Campaign campaign = campaignWithPlayer(9505L, "Style Great Weapon One Handed");
        GameEntity player = player(campaign);
        player.getData().put("fightingStyle", "great-weapon");
        campaigns.saveEntity(player);
        GameEntity enemy = createEnemy(campaign, player);
        CombatState state = transientState(campaign, campaigns.state(campaign.getId()));

        Map<String, Object> data = Map.of();
        for (int attempt = 0; attempt < 40 && data.isEmpty(); attempt++) {
            data = lastAttackData(combat.attack(campaign.getId(), state, player, enemy,
                    Attack.melee("Espada larga", 100, "1d8")));
        }

        assertThat(data.get("greatWeaponFighting")).isEqualTo(false);
        assertThat(intValue(data.get("greatWeaponRerolls"))).isZero();
        assertThat(intValue(data.get("greatWeaponBonus"))).isZero();
    }

    @Test
    void protectionImposesDisadvantageAndSpendsTheProtectorsReaction() {
        Campaign campaign = campaignWithPlayer(9511L, "Style Protection");
        GameEntity target = player(campaign);
        GameEntity protector = createProtector(campaign, target, "Guardiana", 0, 1);
        GameEntity enemy = createEnemy(campaign, target);
        CombatState state = transientState(campaign, campaigns.state(campaign.getId()));

        Map<String, Object> data = Map.of();
        List<GameEvent> events = List.of();
        for (int attempt = 0; attempt < 20 && data.isEmpty(); attempt++) {
            // Reset the reaction so a natural 1 on the attack roll does not waste the setup.
            protector.getData().put("reactionUsed", false);
            campaigns.saveEntity(protector);
            events = combat.attack(campaign.getId(), state, enemy, target,
                    Attack.melee("Garra", 100, "0"));
            data = lastAttackData(events);
        }

        assertThat(data.get("advantage")).isEqualTo("DISADVANTAGE");
        assertThat(data.get("protection")).isEqualTo(true);
        assertThat(((Number) data.get("protectorId")).longValue()).isEqualTo(protector.getId());
        assertThat(events).anyMatch(event -> "PROTECTION".equals(event.type()));
        assertThat(campaigns.entity(protector.getId()).orElseThrow().getData().get("reactionUsed"))
                .isEqualTo(true);
    }

    @Test
    void protectionCannotBeUsedAgainWithoutANewReaction() {
        Campaign campaign = campaignWithPlayer(9512L, "Style Protection Spent");
        GameEntity target = player(campaign);
        GameEntity protector = createProtector(campaign, target, "Guardiana", 0, 1);
        GameEntity enemy = createEnemy(campaign, target);
        CombatState state = transientState(campaign, campaigns.state(campaign.getId()));

        combat.attack(campaign.getId(), state, enemy, target, Attack.melee("Garra", 100, "0"));

        Map<String, Object> data = Map.of();
        List<GameEvent> events = List.of();
        for (int attempt = 0; attempt < 20 && data.isEmpty(); attempt++) {
            events = combat.attack(campaign.getId(), state, enemy, target,
                    Attack.melee("Garra", 100, "0"));
            data = lastAttackData(events);
        }

        assertThat(data.get("advantage")).isEqualTo("NONE");
        assertThat(data.get("protection")).isEqualTo(false);
        assertThat(events).noneMatch(event -> "PROTECTION".equals(event.type()));
    }

    @Test
    void protectionDoesNotReachBeyondFiveFeet() {
        Campaign campaign = campaignWithPlayer(9513L, "Style Protection Range");
        GameEntity target = player(campaign);
        GameEntity protector = createProtector(campaign, target, "Lejana", 2, 0);
        GameEntity enemy = createEnemy(campaign, target);
        CombatState state = transientState(campaign, campaigns.state(campaign.getId()));

        List<GameEvent> events = combat.attack(campaign.getId(), state, enemy, target,
                Attack.melee("Garra", 100, "0"));

        assertThat(events).noneMatch(event -> "PROTECTION".equals(event.type()));
        assertThat(TurnEconomy.flag(
                campaigns.entity(protector.getId()).orElseThrow().getData(), "reactionUsed")).isFalse();
    }

    @Test
    void protectionRequiresTheStyleAndAConsciousLivingProtector() {
        Campaign campaign = campaignWithPlayer(9514L, "Style Protection Requirements");
        GameEntity target = player(campaign);
        GameEntity ally = createEntity(campaign, target, EntityKind.PLAYER, "Sin estilo", 0, 1, 30);
        GameEntity enemy = createEnemy(campaign, target);
        CombatState state = transientState(campaign, campaigns.state(campaign.getId()));

        List<GameEvent> withoutStyle = combat.attack(campaign.getId(), state, enemy, target,
                Attack.melee("Garra", 100, "0"));
        assertThat(withoutStyle).noneMatch(event -> "PROTECTION".equals(event.type()));
        assertThat(TurnEconomy.flag(
                campaigns.entity(ally.getId()).orElseThrow().getData(), "reactionUsed")).isFalse();

        ally.getData().put("fightingStyle", "protection");
        Conditions.add(ally, Condition.UNCONSCIOUS, null, null, null, null);
        campaigns.saveEntity(ally);

        List<GameEvent> unconscious = combat.attack(campaign.getId(), state, enemy, target,
                Attack.melee("Garra", 100, "0"));
        assertThat(unconscious).noneMatch(event -> "PROTECTION".equals(event.type()));
        assertThat(TurnEconomy.flag(
                campaigns.entity(ally.getId()).orElseThrow().getData(), "reactionUsed")).isFalse();
    }

    @Test
    void aDeadProtectorCannotUseProtection() {
        Campaign campaign = campaignWithPlayer(9515L, "Style Protection Dead");
        GameEntity target = player(campaign);
        GameEntity protector = createProtector(campaign, target, "Caída", 0, 1);
        protector.setHp(0);
        protector.setAlive(false);
        campaigns.saveEntity(protector);
        GameEntity enemy = createEnemy(campaign, target);
        CombatState state = transientState(campaign, campaigns.state(campaign.getId()));

        List<GameEvent> events = combat.attack(campaign.getId(), state, enemy, target,
                Attack.melee("Garra", 100, "0"));

        assertThat(events).noneMatch(event -> "PROTECTION".equals(event.type()));
    }
}
