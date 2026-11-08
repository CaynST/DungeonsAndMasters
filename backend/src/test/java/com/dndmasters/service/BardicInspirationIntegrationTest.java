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
import com.dndmasters.domain.Stats;
import com.dndmasters.service.action.ActionResult;
import com.dndmasters.service.action.PlayerAction;
import com.dndmasters.util.Json;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/** Bardic Inspiration through the real actions and combat pipeline. */
@SpringBootTest
@TestPropertySource(properties = "spring.datasource.url=jdbc:sqlite:target/dnd-test.db")
class BardicInspirationIntegrationTest {

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

    private GameEntity createAlly(Campaign campaign, GameEntity bard, Long userId, String name, int dx, int dy) {
        GameState state = campaigns.state(campaign.getId());
        GameEntity ally = new GameEntity();
        ally.setSceneId(state.getScene().id());
        ally.setMapId(state.getMap().getId());
        ally.setKind(EntityKind.PLAYER);
        ally.setName(name);
        ally.setHp(30);
        ally.setMaxHp(30);
        ally.setAc(12);
        ally.setSpeed(6);
        ally.setAlive(true);
        ally.getData().put("ownerId", userId);
        ally.getData().put("classId", "fighter");
        ally.moveTo(new Cell(bard.getX() + dx, bard.getY() + dy));
        campaigns.saveEntity(ally);
        return campaigns.state(campaign.getId()).getEntities().stream()
                .filter(entity -> entity.getKind() == EntityKind.PLAYER && name.equals(entity.getName()))
                .findFirst().orElseThrow();
    }

    private GameEntity createEnemy(Campaign campaign, GameEntity player) {
        GameState state = campaigns.state(campaign.getId());
        GameEntity enemy = new GameEntity();
        enemy.setSceneId(state.getScene().id());
        enemy.setMapId(state.getMap().getId());
        enemy.setKind(EntityKind.ENEMY);
        enemy.setName("Dummy de inspiración");
        enemy.setHp(999);
        enemy.setMaxHp(999);
        enemy.setAc(10);
        enemy.setSpeed(6);
        enemy.setAlive(true);
        enemy.moveTo(new Cell(player.getX() + 1, player.getY()));
        campaigns.saveEntity(enemy);
        return campaigns.state(campaign.getId()).getEntities().stream()
                .filter(entity -> entity.getKind() == EntityKind.ENEMY
                        && "Dummy de inspiración".equals(entity.getName()))
                .findFirst().orElseThrow();
    }

    private PlayerAction action(String type, Long actorId, Long targetId) {
        PlayerAction action = new PlayerAction();
        action.setType(type);
        action.setActorEntityId(actorId);
        action.setTargetEntityId(targetId);
        return action;
    }

    private GameEntity configureBard(Campaign campaign) {
        GameEntity player = player(campaign);
        player.getData().put("classId", "bard");
        player.getData().put("class", "Bard");
        player.getData().put("level", 1);
        player.setStats(new Stats(10, 10, 10, 10, 10, 16));
        campaigns.saveEntity(player);
        return player;
    }

    @Test
    void bardInspiresAnotherAllyAndTheDieIsConsumedOnTheNextAttack() {
        Campaign campaign = campaignWithPlayer(9601L, "Inspiration Attack");
        GameEntity bard = configureBard(campaign);
        GameEntity ally = createAlly(campaign, bard, 9601L, "Aliado", 1, 0);

        ActionResult inspire = game.act(campaign.getId(), 9601L,
                action(PlayerAction.BARDIC_INSPIRATION, bard.getId(), ally.getId()));

        assertThat(inspire.ok()).as(inspire.message()).isTrue();
        GameEntity inspired = campaigns.entity(ally.getId()).orElseThrow();
        assertThat(inspired.getData().get("inspirationDie")).isEqualTo(6);
        GameEntity reloadedBard = campaigns.entity(bard.getId()).orElseThrow();
        assertThat(reloadedBard.getData().get("bardicInspirationUsed")).isEqualTo(1);
        assertThat(reloadedBard.getData().get("bonusUsed")).isEqualTo(true);

        ActionResult ready = game.act(campaign.getId(), 9601L,
                action(PlayerAction.USE_INSPIRATION, ally.getId(), null));
        assertThat(ready.ok()).as(ready.message()).isTrue();
        GameEntity pending = campaigns.entity(ally.getId()).orElseThrow();
        assertThat(pending.getData().get("inspirationPending")).isEqualTo(true);

        GameEntity enemy = createEnemy(campaign, bard);
        GameState state = campaigns.state(campaign.getId());
        CombatState transientState = new CombatState();
        transientState.setCampaignId(campaign.getId());
        transientState.setSceneId(state.getScene().id());
        transientState.setActive(false);
        transientState.setParticipants(new ArrayList<>());

        GameEntity attacker = campaigns.entity(ally.getId()).orElseThrow();
        List<GameEvent> events = combat.attack(campaign.getId(), transientState, attacker, enemy,
                Attack.melee("Prueba", 100, "1d6"));
        GameEvent attackEvent = events.stream()
                .filter(event -> "ATTACK".equals(event.type()) || "ATTACK_MISS".equals(event.type()))
                .findFirst().orElseThrow();
        Map<String, Object> data = Json.readMap(attackEvent.dataJson());
        assertThat(((Number) data.get("inspirationBonus")).intValue()).isPositive();
        GameEntity after = campaigns.entity(ally.getId()).orElseThrow();
        assertThat(after.getData().get("inspirationDie")).isEqualTo(0);
        assertThat(after.getData().get("inspirationPending")).isEqualTo(false);
    }

    @Test
    void inspirationRequiresAnotherLivingAllyAndIsLimitedPerCombat() {
        Campaign campaign = campaignWithPlayer(9602L, "Inspiration Rules");
        GameEntity bard = configureBard(campaign);
        GameEntity ally = createAlly(campaign, bard, 9602L, "Aliado", 1, 0);

        ActionResult self = game.act(campaign.getId(), 9602L,
                action(PlayerAction.BARDIC_INSPIRATION, bard.getId(), bard.getId()));
        assertThat(self.ok()).isFalse();

        ActionResult first = game.act(campaign.getId(), 9602L,
                action(PlayerAction.BARDIC_INSPIRATION, bard.getId(), ally.getId()));
        assertThat(first.ok()).isTrue();

        campaigns.resetTurnUsage(bard.getId());
        ActionResult second = game.act(campaign.getId(), 9602L,
                action(PlayerAction.BARDIC_INSPIRATION, bard.getId(), ally.getId()));
        assertThat(second.ok()).isFalse();
        assertThat(second.message()).containsIgnoringCase("inspiración");
    }

    @Test
    void inspirationExpiresAtTheStartOfTheTargetsTurn() {
        Campaign campaign = campaignWithPlayer(9603L, "Inspiration Timer");
        GameEntity bard = configureBard(campaign);
        GameEntity ally = createAlly(campaign, bard, 9603L, "Aliado", 1, 0);
        game.act(campaign.getId(), 9603L,
                action(PlayerAction.BARDIC_INSPIRATION, bard.getId(), ally.getId()));

        GameEntity current = campaigns.entity(ally.getId()).orElseThrow();
        current.getData().put("inspirationRounds", 1);
        campaigns.saveEntity(current);

        campaigns.startTurn(campaign.getId(), ally.getId());

        GameEntity after = campaigns.entity(ally.getId()).orElseThrow();
        assertThat(after.getData().get("inspirationDie")).isEqualTo(0);
        assertThat(after.getData().get("inspirationPending")).isEqualTo(false);
    }

    @Test
    void nonBardsCannotInspire() {
        Campaign campaign = campaignWithPlayer(9604L, "Inspiration Non Bard");
        GameEntity player = player(campaign);
        player.getData().put("classId", "fighter");
        campaigns.saveEntity(player);
        GameEntity ally = createAlly(campaign, player, 9604L, "Aliado", 1, 0);

        ActionResult result = game.act(campaign.getId(), 9604L,
                action(PlayerAction.BARDIC_INSPIRATION, player.getId(), ally.getId()));

        assertThat(result.ok()).isFalse();
        assertThat(result.message()).containsIgnoringCase("bardos");
    }
}
