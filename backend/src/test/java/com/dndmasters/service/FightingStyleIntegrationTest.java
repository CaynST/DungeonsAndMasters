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
        GameState state = campaigns.state(campaign.getId());
        GameEntity enemy = new GameEntity();
        enemy.setSceneId(state.getScene().id());
        enemy.setMapId(state.getMap().getId());
        enemy.setKind(EntityKind.ENEMY);
        enemy.setName("Dummy de estilo");
        enemy.setHp(999);
        enemy.setMaxHp(999);
        enemy.setAc(10);
        enemy.setSpeed(6);
        enemy.setAlive(true);
        enemy.moveTo(new Cell(player.getX() + 1, player.getY()));
        campaigns.saveEntity(enemy);
        return campaigns.state(campaign.getId()).getEntities().stream()
                .filter(entity -> entity.getKind() == EntityKind.ENEMY
                        && "Dummy de estilo".equals(entity.getName()))
                .findFirst().orElseThrow();
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
}
