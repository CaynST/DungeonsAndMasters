package com.dndmasters.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.dndmasters.domain.Attack;
import com.dndmasters.ai.dm.DmAction;
import com.dndmasters.domain.Campaign;
import com.dndmasters.domain.Cell;
import com.dndmasters.domain.EntityKind;
import com.dndmasters.domain.GameEntity;
import com.dndmasters.domain.GameEvent;
import com.dndmasters.domain.GameState;
import com.dndmasters.engine.combat.Condition;
import com.dndmasters.engine.combat.Conditions;
import com.dndmasters.service.action.ActionResult;
import com.dndmasters.service.action.PlayerAction;
import com.dndmasters.util.Json;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/** Exercises conditions through the real services and persisted SQLite state. */
@SpringBootTest
@TestPropertySource(properties = "spring.datasource.url=jdbc:sqlite:target/dnd-test.db")
class ConditionIntegrationTest {

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

    @Test
    void startTurnExpiresTimedConditions() {
        Campaign campaign = campaignWithPlayer(9101L, "Conditions Timer");
        GameEntity player = player(campaign);
        Conditions.add(player, Condition.POISONED, null, null, null, 1);
        campaigns.saveEntity(player);
        assertThat(Conditions.of(player).has(Condition.POISONED)).isTrue();

        campaigns.startTurn(campaign.getId(), player.getId());

        GameEntity reloaded = campaigns.entity(player.getId()).orElseThrow();
        assertThat(Conditions.of(reloaded).has(Condition.POISONED)).isFalse();
    }

    @Test
    void incapacitatedPlayerCannotActAndGrappledPlayerCannotMove() {
        Campaign campaign = campaignWithPlayer(9102L, "Conditions Actions");
        GameEntity player = player(campaign);
        Conditions.add(player, Condition.STUNNED, null, null, null, null);
        campaigns.saveEntity(player);

        PlayerAction dash = new PlayerAction();
        dash.setType(PlayerAction.DASH);
        dash.setActorEntityId(player.getId());
        ActionResult blocked = game.act(campaign.getId(), 9102L, dash);

        assertThat(blocked.ok()).isFalse();
        assertThat(blocked.message()).containsIgnoringCase("incapacit");

        Conditions.remove(player, Condition.STUNNED);
        Conditions.add(player, Condition.GRAPPLED, null, null, null, null);
        campaigns.saveEntity(player);

        PlayerAction move = new PlayerAction();
        move.setType(PlayerAction.MOVE);
        move.setActorEntityId(player.getId());
        move.setPath(List.of(new Cell(player.getX() + 1, player.getY())));
        ActionResult grappled = game.act(campaign.getId(), 9102L, move);

        assertThat(grappled.ok()).isFalse();
        assertThat(grappled.message()).containsIgnoringCase("movimiento");
    }

    @Test
    void exhaustionPenaltyIsAppliedToChecks() {
        Campaign campaign = campaignWithPlayer(9103L, "Conditions Checks");
        GameEntity player = player(campaign);
        Conditions.add(player, Condition.EXHAUSTION, null, null, null, null);
        Conditions.add(player, Condition.EXHAUSTION, null, null, null, null);
        campaigns.saveEntity(player);

        PlayerAction check = new PlayerAction();
        check.setType(PlayerAction.CHECK);
        check.setActorEntityId(player.getId());
        check.setSkill("perception");
        check.setDc(1);
        ActionResult result = game.act(campaign.getId(), 9103L, check);

        GameEvent event = result.events().stream()
                .filter(item -> "CHECK".equals(item.type()))
                .findFirst().orElseThrow();
        Map<String, Object> data = Json.readMap(event.dataJson());
        assertThat(data.get("penalty")).isEqualTo(-4);
    }

    @Test
    void dmCanApplyAndRemoveConditions() {
        Campaign campaign = campaignWithPlayer(9105L, "Conditions DM");
        GameEntity player = player(campaign);

        game.applyDmActions(campaign.getId(), 9105L,
                List.of(DmAction.applyCondition(player.getId(), "poisoned", 3)));

        GameEntity reloaded = campaigns.entity(player.getId()).orElseThrow();
        assertThat(Conditions.of(reloaded).has(Condition.POISONED)).isTrue();
        assertThat(Conditions.of(reloaded).get(Condition.POISONED).rounds()).isEqualTo(3);

        game.applyDmActions(campaign.getId(), 9105L,
                List.of(DmAction.removeCondition(player.getId(), "POISONED")));

        reloaded = campaigns.entity(player.getId()).orElseThrow();
        assertThat(Conditions.of(reloaded).has(Condition.POISONED)).isFalse();
    }

    @Test
    void pronePlayerCanStandUpSpendingHalfMovement() {
        Campaign campaign = campaignWithPlayer(9106L, "Conditions Stand Up");
        GameEntity player = player(campaign);
        Conditions.add(player, Condition.PRONE, null, null, null, null);
        campaigns.saveEntity(player);

        PlayerAction standUp = new PlayerAction();
        standUp.setType(PlayerAction.STAND_UP);
        standUp.setActorEntityId(player.getId());
        ActionResult result = game.act(campaign.getId(), 9106L, standUp);

        assertThat(result.ok()).isTrue();
        GameEntity reloaded = campaigns.entity(player.getId()).orElseThrow();
        assertThat(Conditions.of(reloaded).has(Condition.PRONE)).isFalse();
        assertThat(com.dndmasters.engine.combat.TurnEconomy.movementUsed(reloaded.getData()))
                .isEqualTo(reloaded.getSpeed() / 2);
    }

    @Test
    void unconsciousAlsoAppliesProne() {
        Campaign campaign = campaignWithPlayer(9107L, "Conditions Unconscious");
        GameEntity player = player(campaign);

        Conditions.add(player, Condition.UNCONSCIOUS, null, null, null, null);

        assertThat(Conditions.of(player).has(Condition.PRONE)).isTrue();
        assertThat(Conditions.of(player).incapacitated()).isTrue();
    }

    @Test
    void proneTargetGrantsAdvantageToMeleeAttacks() {
        Campaign campaign = campaignWithPlayer(9104L, "Conditions Attacks");
        reachCryptCombat(campaign, 9104L);

        GameState state = campaigns.state(campaign.getId());
        GameEntity player = state.getEntities().stream()
                .filter(entity -> entity.getKind() == EntityKind.PLAYER).findFirst().orElseThrow();
        GameEntity enemy = state.getEntities().stream()
                .filter(entity -> entity.getKind() == EntityKind.ENEMY).findFirst().orElseThrow();
        enemy.moveTo(new Cell(player.getX() + 1, player.getY()));
        Conditions.add(enemy, Condition.PRONE, null, null, null, null);
        campaigns.saveEntity(enemy);

        List<GameEvent> events = combat.attack(campaign.getId(), state.getCombat(), player, enemy,
                Attack.melee("Prueba", 100, "1d1"));

        GameEvent attackEvent = events.stream()
                .filter(item -> "ATTACK".equals(item.type()) || "ATTACK_MISS".equals(item.type()))
                .findFirst().orElseThrow();
        assertThat(Json.readMap(attackEvent.dataJson()).get("advantage")).isEqualTo("ADVANTAGE");
    }

    private void reachCryptCombat(Campaign campaign, Long userId) {
        for (int i = 0; i < 60; i++) {
            GameState state = campaigns.state(campaign.getId());
            if (state.getCombat() != null && state.getCombat().isActive()) {
                return;
            }
            PlayerAction check = new PlayerAction();
            check.setType(PlayerAction.CHECK);
            check.setSkill("investigation");
            check.setDc(1);
            game.act(campaign.getId(), userId, check);
        }
        throw new AssertionError("Combat did not start");
    }
}
