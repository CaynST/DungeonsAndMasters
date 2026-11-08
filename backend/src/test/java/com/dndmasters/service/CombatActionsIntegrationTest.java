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
import com.dndmasters.engine.combat.TurnEconomy;
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

/** R-02/R-03 combat actions through the real services and persisted state. */
@SpringBootTest
@TestPropertySource(properties = "spring.datasource.url=jdbc:sqlite:target/dnd-test.db")
class CombatActionsIntegrationTest {

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

    private GameEntity createAlly(Campaign campaign, GameEntity actor, Long userId, String name, int dx, int dy) {
        return createEntity(campaign, actor, EntityKind.PLAYER, name, dx, dy, 30);
    }

    private GameEntity createEnemy(Campaign campaign, GameEntity actor, String name, int dx, int dy) {
        return createEntity(campaign, actor, EntityKind.ENEMY, name, dx, dy, 999);
    }

    private GameEntity createEntity(Campaign campaign, GameEntity actor, EntityKind kind, String name,
                                    int dx, int dy, int hp) {
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
        entity.getData().put("ownerId", actor.getData().get("ownerId"));
        entity.getData().put("attacks", List.of(Map.of(
                "name", "Prueba", "attackBonus", 5, "damageExpression", "1d4",
                "range", 1, "ranged", false)));
        entity.moveTo(new Cell(actor.getX() + dx, actor.getY() + dy));
        campaigns.saveEntity(entity);
        return campaigns.state(campaign.getId()).getEntities().stream()
                .filter(candidate -> candidate.getKind() == kind && name.equals(candidate.getName()))
                .findFirst().orElseThrow();
    }

    private PlayerAction action(String type, Long actorId, Long targetId) {
        PlayerAction action = new PlayerAction();
        action.setType(type);
        action.setActorEntityId(actorId);
        action.setTargetEntityId(targetId);
        return action;
    }

    private CombatState transientState(Campaign campaign) {
        GameState state = campaigns.state(campaign.getId());
        CombatState combatState = new CombatState();
        combatState.setCampaignId(campaign.getId());
        combatState.setSceneId(state.getScene().id());
        combatState.setActive(false);
        combatState.setParticipants(new ArrayList<>());
        return combatState;
    }

    private GameEvent eventOfType(ActionResult result, String type) {
        return result.events().stream().filter(event -> type.equals(event.type())).findFirst().orElseThrow();
    }

    private GameEvent attackEventOf(List<GameEvent> events) {
        return events.stream()
                .filter(event -> "ATTACK".equals(event.type()) || "ATTACK_MISS".equals(event.type()))
                .findFirst().orElseThrow();
    }

    @Test
    void helpGrantsAdvantageToAnAdjacentAllyAgainstATargetNearTheHelper() {
        Campaign campaign = campaignWithPlayer(9701L, "Actions Help");
        GameEntity helper = player(campaign);
        GameEntity ally = createAlly(campaign, helper, 9701L, "Aliado", 1, 0);
        GameEntity enemy = createEnemy(campaign, helper, "Goblin", 0, 1);

        ActionResult result = game.act(campaign.getId(), 9701L,
                action(PlayerAction.HELP, helper.getId(), ally.getId()));

        assertThat(result.ok()).as(result.message()).isTrue();
        assertThat(eventOfType(result, "HELP")).isNotNull();
        GameEntity helpedAlly = campaigns.entity(ally.getId()).orElseThrow();
        assertThat(helpedAlly.getData().get("helpAdvantage")).isEqualTo(true);
        assertThat(((Number) helpedAlly.getData().get("helpedBy")).longValue()).isEqualTo(helper.getId());
        assertThat(campaigns.entity(helper.getId()).orElseThrow().getData().get("actionUsed")).isEqualTo(true);

        List<GameEvent> attacks = combat.attack(campaign.getId(), transientState(campaign), helpedAlly, enemy,
                Attack.melee("Prueba", 100, "1d1"));
        Map<String, Object> data = Json.readMap(attackEventOf(attacks).dataJson());

        assertThat(data.get("advantage")).isEqualTo("ADVANTAGE");
        assertThat(data.get("helped")).isEqualTo(true);
        GameEntity after = campaigns.entity(ally.getId()).orElseThrow();
        assertThat(after.getData().get("helpAdvantage")).isEqualTo(false);
    }

    @Test
    void helpOnlyGrantsAdvantageWhenTheTargetIsNearTheHelper() {
        Campaign campaign = campaignWithPlayer(9712L, "Actions Help Target");
        GameEntity helper = player(campaign);
        GameEntity ally = createAlly(campaign, helper, 9712L, "Aliado", 1, 0);
        GameEntity farEnemy = createEnemy(campaign, helper, "Goblin lejano", 6, 0);

        ActionResult result = game.act(campaign.getId(), 9712L,
                action(PlayerAction.HELP, helper.getId(), ally.getId()));
        assertThat(result.ok()).as(result.message()).isTrue();

        GameEntity helpedAlly = campaigns.entity(ally.getId()).orElseThrow();
        List<GameEvent> attacks = combat.attack(campaign.getId(), transientState(campaign), helpedAlly, farEnemy,
                Attack.melee("Prueba", 100, "1d1"));
        Map<String, Object> data = Json.readMap(attackEventOf(attacks).dataJson());

        assertThat(data.get("advantage")).isEqualTo("NONE");
        assertThat(data.get("helped")).isEqualTo(false);
        assertThat(campaigns.entity(ally.getId()).orElseThrow().getData().get("helpAdvantage")).isEqualTo(false);
    }

    @Test
    void helpRequiresAnotherAllyWithinFiveFeet() {
        Campaign campaign = campaignWithPlayer(9702L, "Actions Help Range");
        GameEntity helper = player(campaign);
        GameEntity ally = createAlly(campaign, helper, 9702L, "Aliado", 2, 0);

        ActionResult outOfRange = game.act(campaign.getId(), 9702L,
                action(PlayerAction.HELP, helper.getId(), ally.getId()));
        assertThat(outOfRange.ok()).isFalse();
        assertThat(outOfRange.message()).containsIgnoringCase("5 pies");

        ActionResult self = game.act(campaign.getId(), 9702L,
                action(PlayerAction.HELP, helper.getId(), helper.getId()));
        assertThat(self.ok()).isFalse();
    }

    @Test
    void hideFailsWithAnAdjacentEnemyAndGrantsAdvantageOtherwise() {
        Campaign campaign = campaignWithPlayer(9703L, "Actions Hide");
        GameEntity rogue = player(campaign);
        GameEntity enemy = createEnemy(campaign, rogue, "Goblin", 1, 0);

        ActionResult blocked = game.act(campaign.getId(), 9703L,
                action(PlayerAction.HIDE, rogue.getId(), null));
        assertThat(blocked.ok()).isFalse();
        assertThat(blocked.message()).containsIgnoringCase("enemigo");

        enemy.moveTo(new Cell(rogue.getX() + 4, rogue.getY()));
        campaigns.saveEntity(enemy);
        ActionResult hidden = game.act(campaign.getId(), 9703L,
                action(PlayerAction.HIDE, rogue.getId(), null));

        assertThat(hidden.ok()).as(hidden.message()).isTrue();
        GameEntity afterHide = campaigns.entity(rogue.getId()).orElseThrow();
        assertThat(afterHide.getData().get("hidden")).isEqualTo(true);
        assertThat(afterHide.getData().get("actionUsed")).isEqualTo(true);

        List<GameEvent> attacks = combat.attack(campaign.getId(), transientState(campaign), afterHide, enemy,
                Attack.melee("Prueba", 100, "1d1"));
        assertThat(Json.readMap(attackEventOf(attacks).dataJson()).get("advantage")).isEqualTo("ADVANTAGE");
        assertThat(campaigns.entity(rogue.getId()).orElseThrow().getData().get("hidden")).isEqualTo(false);
    }

    @Test
    void searchResolvesPerceptionOrInvestigationAndConsumesTheAction() {
        Campaign campaign = campaignWithPlayer(9704L, "Actions Search");
        GameEntity player = player(campaign);

        PlayerAction search = action(PlayerAction.SEARCH, player.getId(), null);
        search.setSkill("investigation");
        search.setDc(10);
        ActionResult result = game.act(campaign.getId(), 9704L, search);

        assertThat(result.ok()).as(result.message()).isTrue();
        Map<String, Object> data = Json.readMap(eventOfType(result, "SEARCH").dataJson());
        assertThat(data.get("skill")).isEqualTo("investigation");
        assertThat(data.get("dc")).isEqualTo(10);
        assertThat(data.get("total")).isInstanceOf(Number.class);
        assertThat(campaigns.entity(player.getId()).orElseThrow().getData().get("actionUsed")).isEqualTo(true);

        ActionResult again = game.act(campaign.getId(), 9704L, search);
        assertThat(again.ok()).isFalse();
        assertThat(again.message()).containsIgnoringCase("acción");

        campaigns.resetTurnUsage(player.getId());
        assertThat(game.act(campaign.getId(), 9704L, search).ok()).isTrue();
    }

    @Test
    void searchRejectsSkillsOtherThanPerceptionAndInvestigation() {
        Campaign campaign = campaignWithPlayer(9705L, "Actions Search Skill");
        GameEntity player = player(campaign);

        PlayerAction search = action(PlayerAction.SEARCH, player.getId(), null);
        search.setSkill("stealth");
        ActionResult result = game.act(campaign.getId(), 9705L, search);

        assertThat(result.ok()).isFalse();
        assertThat(result.message()).containsIgnoringCase("Percepción");
    }

    @Test
    void useObjectAppliesANonPotionItemAndRejectsPotionsAndUnknownItems() {
        Campaign campaign = campaignWithPlayer(9706L, "Actions Object");
        GameEntity player = player(campaign);

        PlayerAction torch = action(PlayerAction.USE_OBJECT, player.getId(), null);
        torch.setItem("Antorcha");
        ActionResult used = game.act(campaign.getId(), 9706L, torch);

        assertThat(used.ok()).as(used.message()).isTrue();
        Map<String, Object> data = Json.readMap(eventOfType(used, "USE_OBJECT").dataJson());
        assertThat(data.get("item")).isEqualTo("Antorcha");
        GameEntity after = campaigns.entity(player.getId()).orElseThrow();
        assertThat(after.getData().get("lightSource")).isEqualTo("torch");
        assertThat(after.getData().get("actionUsed")).isEqualTo(true);

        campaigns.resetTurnUsage(player.getId());
        PlayerAction potion = action(PlayerAction.USE_OBJECT, player.getId(), null);
        potion.setItem("Poción de Curación");
        ActionResult potionResult = game.act(campaign.getId(), 9706L, potion);
        assertThat(potionResult.ok()).isFalse();
        assertThat(potionResult.message()).containsIgnoringCase("poción");

        PlayerAction unknown = action(PlayerAction.USE_OBJECT, player.getId(), null);
        unknown.setItem("Reliquia inexistente");
        ActionResult unknownResult = game.act(campaign.getId(), 9706L, unknown);
        assertThat(unknownResult.ok()).isFalse();
        assertThat(unknownResult.message()).containsIgnoringCase("inventario");
    }

    @Test
    void readyConsumesTheActionAndTriggersAReactionAttackWhenAnEnemyEntersReach() {
        Campaign campaign = campaignWithPlayer(9707L, "Actions Ready");
        GameEntity player = player(campaign);

        ActionResult ready = game.act(campaign.getId(), 9707L,
                action(PlayerAction.READY, player.getId(), null));
        assertThat(ready.ok()).as(ready.message()).isTrue();
        GameEntity readied = campaigns.entity(player.getId()).orElseThrow();
        assertThat(readied.getData().get("readied")).isEqualTo(true);
        assertThat(readied.getData().get("actionUsed")).isEqualTo(true);

        // The enemy starts two cells away and steps into the player's reach.
        GameEntity enemy = createEnemy(campaign, player, "Goblin", 2, 0);
        Cell previous = enemy.cell();
        enemy.moveTo(new Cell(player.getX() + 1, player.getY()));
        campaigns.saveEntity(enemy);

        List<GameEvent> events = combat.readiedAttacks(campaign.getId(), transientState(campaign), enemy, previous);

        assertThat(events).anyMatch(event -> "READIED_ATTACK".equals(event.type()));
        assertThat(events).anyMatch(event -> "ATTACK".equals(event.type()) || "ATTACK_MISS".equals(event.type()));
        GameEntity after = campaigns.entity(player.getId()).orElseThrow();
        assertThat(after.getData().get("readied")).isEqualTo(false);
        assertThat(after.getData().get("reactionUsed")).isEqualTo(true);

        // One reaction only: another trigger in the same turn does nothing.
        enemy.moveTo(previous);
        campaigns.saveEntity(enemy);
        enemy.moveTo(new Cell(player.getX() + 1, player.getY()));
        campaigns.saveEntity(enemy);
        List<GameEvent> second = combat.readiedAttacks(campaign.getId(), transientState(campaign), enemy, previous);
        assertThat(second).noneMatch(event -> "READIED_ATTACK".equals(event.type()));
    }

    @Test
    void readiedExpiresAtTheStartOfThePlayersNextTurn() {
        Campaign campaign = campaignWithPlayer(9708L, "Actions Ready Expire");
        GameEntity player = player(campaign);
        game.act(campaign.getId(), 9708L, action(PlayerAction.READY, player.getId(), null));

        campaigns.startTurn(campaign.getId(), player.getId());

        assertThat(campaigns.entity(player.getId()).orElseThrow().getData().get("readied")).isEqualTo(false);
    }

    @Test
    void freeObjectInteractionIsOncePerTurnAndDoesNotConsumeTheAction() {
        Campaign campaign = campaignWithPlayer(9709L, "Actions Interact");
        GameEntity player = player(campaign);

        PlayerAction interact = action(PlayerAction.INTERACT, player.getId(), null);
        interact.setText("Abre la puerta.");
        ActionResult first = game.act(campaign.getId(), 9709L, interact);

        assertThat(first.ok()).as(first.message()).isTrue();
        assertThat(eventOfType(first, "INTERACT")).isNotNull();
        GameEntity after = campaigns.entity(player.getId()).orElseThrow();
        assertThat(after.getData().get("objectInteractionUsed")).isEqualTo(true);
        assertThat(TurnEconomy.flag(after.getData(), "actionUsed")).isFalse();

        ActionResult second = game.act(campaign.getId(), 9709L, interact);
        assertThat(second.ok()).isFalse();
        assertThat(second.message()).containsIgnoringCase("interacción");

        campaigns.resetTurnUsage(player.getId());
        assertThat(game.act(campaign.getId(), 9709L, interact).ok()).isTrue();
    }

    @Test
    void playersGetAnOpportunityAttackWhenAnEnemyLeavesTheirReach() {
        Campaign campaign = campaignWithPlayer(9710L, "Actions Opportunity");
        GameEntity player = player(campaign);
        GameEntity enemy = createEnemy(campaign, player, "Goblin", 1, 0);
        Cell previous = enemy.cell();
        enemy.moveTo(new Cell(player.getX() + 3, player.getY()));
        campaigns.saveEntity(enemy);

        List<GameEvent> events = combat.opportunityAttacks(campaign.getId(), transientState(campaign), enemy, previous);

        assertThat(events).anyMatch(event -> "OPPORTUNITY_ATTACK".equals(event.type()));
        assertThat(events).anyMatch(event -> "ATTACK".equals(event.type()) || "ATTACK_MISS".equals(event.type()));
        GameEntity after = campaigns.entity(player.getId()).orElseThrow();
        assertThat(after.getData().get("reactionUsed")).isEqualTo(true);

        // The player has already spent its reaction.
        enemy.moveTo(previous);
        campaigns.saveEntity(enemy);
        enemy.moveTo(new Cell(player.getX() + 3, player.getY()));
        campaigns.saveEntity(enemy);
        List<GameEvent> second = combat.opportunityAttacks(campaign.getId(), transientState(campaign), enemy, previous);
        assertThat(second).noneMatch(event -> "OPPORTUNITY_ATTACK".equals(event.type()));

        campaigns.resetTurnUsage(player.getId());
        List<GameEvent> third = combat.opportunityAttacks(campaign.getId(), transientState(campaign), enemy, previous);
        assertThat(third).anyMatch(event -> "OPPORTUNITY_ATTACK".equals(event.type()));
    }

    @Test
    void enemyLeavingWhileDisengagedDoesNotProvokePlayerOpportunityAttacks() {
        Campaign campaign = campaignWithPlayer(9711L, "Actions Opportunity Disengage");
        GameEntity player = player(campaign);
        GameEntity enemy = createEnemy(campaign, player, "Goblin", 1, 0);
        enemy.getData().put("disengaged", true);
        campaigns.saveEntity(enemy);
        Cell previous = enemy.cell();
        enemy.moveTo(new Cell(player.getX() + 3, player.getY()));
        campaigns.saveEntity(enemy);

        List<GameEvent> events = combat.opportunityAttacks(campaign.getId(), transientState(campaign), enemy, previous);

        assertThat(events).noneMatch(event -> "OPPORTUNITY_ATTACK".equals(event.type()));
        assertThat(TurnEconomy.flag(campaigns.entity(player.getId()).orElseThrow().getData(), "reactionUsed"))
                .isFalse();
    }
}
