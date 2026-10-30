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

/** Class features through the real services and persisted state. */
@SpringBootTest
@TestPropertySource(properties = "spring.datasource.url=jdbc:sqlite:target/dnd-test.db")
class ClassFeatureIntegrationTest {

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

    private PlayerAction action(String type, Long actorId) {
        PlayerAction action = new PlayerAction();
        action.setType(type);
        action.setActorEntityId(actorId);
        return action;
    }

    @Test
    void secondWindHealsOnceAndConsumesTheBonusAction() {
        Campaign campaign = campaignWithPlayer(9301L, "Second Wind");
        GameEntity player = player(campaign);
        player.getData().put("classId", "fighter");
        player.setHp(10);
        campaigns.saveEntity(player);

        ActionResult first = game.act(campaign.getId(), 9301L, action(PlayerAction.SECOND_WIND, player.getId()));

        assertThat(first.ok()).isTrue();
        GameEntity reloaded = campaigns.entity(player.getId()).orElseThrow();
        assertThat(reloaded.getHp()).isGreaterThan(10);
        assertThat(reloaded.getData().get("secondWindUsed")).isEqualTo(true);
        assertThat(reloaded.getData().get("bonusUsed")).isEqualTo(true);

        ActionResult second = game.act(campaign.getId(), 9301L, action(PlayerAction.SECOND_WIND, player.getId()));
        assertThat(second.ok()).isFalse();
        assertThat(second.message()).containsIgnoringCase("acción adicional");

        campaigns.resetTurnUsage(player.getId());
        ActionResult third = game.act(campaign.getId(), 9301L, action(PlayerAction.SECOND_WIND, player.getId()));
        assertThat(third.ok()).isFalse();
        assertThat(third.message()).containsIgnoringCase("Segundo aliento");
    }

    @Test
    void rageUsesTheBonusActionAndIsLimitedToTwoUses() {
        Campaign campaign = campaignWithPlayer(9302L, "Rage");
        GameEntity player = player(campaign);
        player.getData().put("classId", "barbarian");
        player.getData().put("class", "Barbarian");
        campaigns.saveEntity(player);

        ActionResult first = game.act(campaign.getId(), 9302L, action(PlayerAction.RAGE, player.getId()));
        assertThat(first.ok()).isTrue();
        GameEntity reloaded = campaigns.entity(player.getId()).orElseThrow();
        assertThat(reloaded.getData().get("raging")).isEqualTo(true);
        assertThat(reloaded.getData().get("ragesUsed")).isEqualTo(1);
        assertThat(reloaded.getData().get("bonusUsed")).isEqualTo(true);

        ActionResult second = game.act(campaign.getId(), 9302L, action(PlayerAction.RAGE, player.getId()));
        assertThat(second.ok()).isFalse();
        assertThat(second.message()).containsIgnoringCase("acción adicional");

        campaigns.resetTurnUsage(player.getId());
        ActionResult third = game.act(campaign.getId(), 9302L, action(PlayerAction.RAGE, player.getId()));
        assertThat(third.ok()).isFalse();
        assertThat(third.message()).containsIgnoringCase("enfurecido");

        GameEntity current = campaigns.entity(player.getId()).orElseThrow();
        current.getData().put("raging", false);
        campaigns.saveEntity(current);
        campaigns.resetTurnUsage(player.getId());
        ActionResult fourth = game.act(campaign.getId(), 9302L, action(PlayerAction.RAGE, player.getId()));
        assertThat(fourth.ok()).isTrue();

        current = campaigns.entity(player.getId()).orElseThrow();
        current.getData().put("raging", false);
        campaigns.saveEntity(current);
        campaigns.resetTurnUsage(player.getId());
        ActionResult fifth = game.act(campaign.getId(), 9302L, action(PlayerAction.RAGE, player.getId()));
        assertThat(fifth.ok()).isFalse();
        assertThat(fifth.message()).containsIgnoringCase("No te quedan usos");
    }

    @Test
    void rogueSneakAttackAddsDamageWhenAnAllyIsAdjacent() {
        Campaign campaign = campaignWithPlayer(9303L, "Sneak Attack");
        GameState state = campaigns.state(campaign.getId());
        GameEntity player = player(campaign);
        GameEntity target = state.getEntities().stream()
                .filter(entity -> entity.getId() != player.getId())
                .findFirst().orElseThrow();

        player.getData().put("classId", "rogue");
        player.getData().put("class", "Rogue");
        player.getData().put("sneakAttackUsed", false);
        player.moveTo(new Cell(4, 5));
        campaigns.saveEntity(player);
        target.setHp(999);
        target.setMaxHp(999);
        target.moveTo(new Cell(5, 5));
        campaigns.saveEntity(target);

        GameEntity ally = new GameEntity();
        ally.setSceneId(state.getScene().id());
        ally.setMapId(state.getMap().getId());
        ally.setKind(EntityKind.PLAYER);
        ally.setName("Aliada");
        ally.setHp(10);
        ally.setMaxHp(10);
        ally.setAc(12);
        ally.setSpeed(6);
        ally.setAlive(true);
        ally.moveTo(new Cell(6, 5));
        campaigns.saveEntity(ally);

        CombatState transientState = new CombatState();
        transientState.setCampaignId(campaign.getId());
        transientState.setSceneId(state.getScene().id());
        transientState.setActive(false);
        transientState.setParticipants(new ArrayList<>());

        boolean sawSneakAttack = false;
        for (int attempt = 0; attempt < 20 && !sawSneakAttack; attempt++) {
            List<GameEvent> events = combat.attack(campaign.getId(), transientState, player, target,
                    Attack.melee("Daga", 100, "1d4"));
            GameEvent attackEvent = events.stream()
                    .filter(event -> "ATTACK".equals(event.type()))
                    .findFirst().orElse(null);
            if (attackEvent != null) {
                Map<String, Object> data = Json.readMap(attackEvent.dataJson());
                if (Boolean.TRUE.equals(data.get("sneakAttack"))) {
                    sawSneakAttack = true;
                    assertThat(((Number) data.get("sneakAttackDamage")).intValue()).isPositive();
                }
            }
        }

        assertThat(sawSneakAttack).isTrue();
        GameEntity reloaded = campaigns.entity(player.getId()).orElseThrow();
        assertThat(reloaded.getData().get("sneakAttackUsed")).isEqualTo(true);
    }

    @Test
    void rageExpiresAtTheStartOfTheNextTurn() {
        Campaign campaign = campaignWithPlayer(9304L, "Rage Timer");
        GameEntity player = player(campaign);
        player.getData().put("classId", "barbarian");
        player.getData().put("raging", true);
        player.getData().put("rageRounds", 1);
        campaigns.saveEntity(player);

        campaigns.startTurn(campaign.getId(), player.getId());

        GameEntity reloaded = campaigns.entity(player.getId()).orElseThrow();
        assertThat(reloaded.getData().get("raging")).isEqualTo(false);
    }
}
