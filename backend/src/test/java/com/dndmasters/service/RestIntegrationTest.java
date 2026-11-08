package com.dndmasters.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.dndmasters.domain.Campaign;
import com.dndmasters.domain.Cell;
import com.dndmasters.domain.EntityKind;
import com.dndmasters.domain.GameEntity;
import com.dndmasters.domain.GameState;
import com.dndmasters.engine.rules.Modifier;
import com.dndmasters.service.action.ActionResult;
import com.dndmasters.service.action.PlayerAction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/** Short and long rests through the real services and persisted state. */
@SpringBootTest
@TestPropertySource(properties = "spring.datasource.url=jdbc:sqlite:target/dnd-test.db")
class RestIntegrationTest {

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

    private void addEnemy(GameState state) {
        GameEntity enemy = new GameEntity();
        enemy.setSceneId(state.getScene().id());
        enemy.setMapId(state.getMap().getId());
        enemy.setKind(EntityKind.ENEMY);
        enemy.setName("Goblin de guardia");
        enemy.setHp(7);
        enemy.setMaxHp(7);
        enemy.setAc(13);
        enemy.setSpeed(6);
        enemy.setAlive(true);
        enemy.moveTo(new Cell(8, 5));
        campaigns.saveEntity(enemy);
    }

    @Test
    void newPartyTokensExposeHitDiceDefaults() {
        Campaign campaign = campaignWithPlayer(9600L, "Hit Dice Defaults");
        GameEntity player = player(campaign);

        assertThat(player.getData().get("hitDie")).isEqualTo("d10");
        assertThat(player.getData().get("hitDiceMax")).isEqualTo(1);
        assertThat(player.getData().get("hitDiceRemaining")).isEqualTo(1);
    }

    @Test
    void shortRestSpendsOneHitDieHealsAndRechargesSecondWind() {
        Long userId = 9601L;
        Campaign campaign = campaignWithPlayer(userId, "Short Rest");
        GameEntity player = player(campaign);
        player.getData().put("classId", "fighter");
        player.getData().put("hitDie", "d10");
        player.getData().put("hitDiceMax", 1);
        player.getData().put("hitDiceRemaining", 1);
        player.getData().put("secondWindUsed", true);
        player.setHp(5);
        player.setMaxHp(30);
        player.setAlive(true);
        campaigns.saveEntity(player);
        int conModifier = Modifier.of(player.getStats().constitution());

        ActionResult result = game.act(campaign.getId(), userId,
                action(PlayerAction.SHORT_REST, player.getId()));

        assertThat(result.ok()).isTrue();
        assertThat(result.events()).anyMatch(event -> PlayerAction.SHORT_REST.equals(event.type()));
        GameEntity reloaded = campaigns.entity(player.getId()).orElseThrow();
        assertThat(reloaded.getHp()).isBetween(6 + conModifier, 15 + conModifier);
        assertThat(reloaded.getData().get("hitDiceRemaining")).isEqualTo(0);
        assertThat(reloaded.getData().get("secondWindUsed")).isEqualTo(false);
        assertThat(reloaded.getData().get("hitDiceMax")).isEqualTo(1);
    }

    @Test
    void shortRestDoesNotRestoreSpellSlots() {
        Long userId = 9602L;
        Campaign campaign = campaignWithPlayer(userId, "Short Rest Slots");
        GameEntity player = player(campaign);
        player.getData().put("classId", "wizard");
        player.getData().put("hitDie", "d6");
        player.getData().put("hitDiceMax", 1);
        player.getData().put("hitDiceRemaining", 1);
        player.getData().put("spellSlotsUsed", 1);
        player.setHp(5);
        player.setMaxHp(20);
        campaigns.saveEntity(player);

        ActionResult result = game.act(campaign.getId(), userId,
                action(PlayerAction.SHORT_REST, player.getId()));

        assertThat(result.ok()).isTrue();
        assertThat(campaigns.entity(player.getId()).orElseThrow().getData().get("spellSlotsUsed"))
                .isEqualTo(1);
    }

    @Test
    void shortRestRequiresAnAvailableHitDie() {
        Long userId = 9603L;
        Campaign campaign = campaignWithPlayer(userId, "No Hit Dice");
        GameEntity player = player(campaign);
        player.getData().put("hitDiceMax", 1);
        player.getData().put("hitDiceRemaining", 0);
        campaigns.saveEntity(player);

        ActionResult result = game.act(campaign.getId(), userId,
                action(PlayerAction.SHORT_REST, player.getId()));

        assertThat(result.ok()).isFalse();
        assertThat(result.message()).containsIgnoringCase("dados de golpe");
    }

    @Test
    void longRestRestoresHealthResourcesAndAtLeastHalfTheHitDice() {
        Long userId = 9604L;
        Campaign campaign = campaignWithPlayer(userId, "Long Rest");
        GameEntity player = player(campaign);
        player.getData().put("classId", "barbarian");
        player.getData().put("hitDie", "d12");
        player.getData().put("hitDiceMax", 2);
        player.getData().put("hitDiceRemaining", 0);
        player.getData().put("secondWindUsed", true);
        player.getData().put("ragesUsed", 2);
        player.getData().put("raging", true);
        player.getData().put("rageRounds", 4);
        player.getData().put("bardicInspirationUsed", 2);
        player.getData().put("spellSlotsUsed", 2);
        player.getData().put("concentrationSpell", "bless");
        player.getData().put("concentrationName", "Bendición");
        player.getData().put("concentrationRounds", 5);
        player.getData().put("inspirationDie", 6);
        player.getData().put("inspirationPending", true);
        player.getData().put("inspirationRounds", 3);
        player.getData().put("inspirationSourceId", 42L);
        player.getData().put("actionUsed", true);
        player.getData().put("bonusUsed", true);
        player.getData().put("dodging", true);
        player.getData().put("disengaged", true);
        player.getData().put("readied", true);
        player.getData().put("hidden", true);
        player.getData().put("helpAdvantage", true);
        player.getData().put("helpedBy", 42L);
        player.getData().put("deathSuccesses", 1);
        player.getData().put("deathFailures", 2);
        player.getData().put("dying", false);
        player.getData().put("stable", false);
        player.setHp(1);
        player.setMaxHp(30);
        campaigns.saveEntity(player);

        ActionResult result = game.act(campaign.getId(), userId,
                action(PlayerAction.LONG_REST, player.getId()));

        assertThat(result.ok()).isTrue();
        assertThat(result.events()).anyMatch(event -> PlayerAction.LONG_REST.equals(event.type()));
        GameEntity reloaded = campaigns.entity(player.getId()).orElseThrow();
        assertThat(reloaded.getHp()).isEqualTo(reloaded.getMaxHp());
        assertThat(reloaded.getData().get("hitDiceRemaining")).isEqualTo(1);
        assertThat(reloaded.getData().get("hitDiceMax")).isEqualTo(2);
        assertThat(reloaded.getData().get("secondWindUsed")).isEqualTo(false);
        assertThat(reloaded.getData().get("ragesUsed")).isEqualTo(0);
        assertThat(reloaded.getData().get("raging")).isEqualTo(false);
        assertThat(reloaded.getData().get("rageRounds")).isEqualTo(0);
        assertThat(reloaded.getData().get("bardicInspirationUsed")).isEqualTo(0);
        assertThat(reloaded.getData().get("spellSlotsUsed")).isEqualTo(0);
        assertThat(reloaded.getData().get("concentrationSpell")).isNull();
        assertThat(reloaded.getData().get("concentrationName")).isNull();
        assertThat(reloaded.getData().get("inspirationDie")).isEqualTo(0);
        assertThat(reloaded.getData().get("inspirationPending")).isEqualTo(false);
        assertThat(reloaded.getData().get("actionUsed")).isEqualTo(false);
        assertThat(reloaded.getData().get("bonusUsed")).isEqualTo(false);
        assertThat(reloaded.getData().get("dodging")).isEqualTo(false);
        assertThat(reloaded.getData().get("disengaged")).isEqualTo(false);
        assertThat(reloaded.getData().get("readied")).isEqualTo(false);
        assertThat(reloaded.getData().get("hidden")).isEqualTo(false);
        assertThat(reloaded.getData().get("helpAdvantage")).isEqualTo(false);
        assertThat(reloaded.getData().get("deathSuccesses")).isEqualTo(0);
        assertThat(reloaded.getData().get("deathFailures")).isEqualTo(0);
    }

    @Test
    void shortRestHealingIsCappedAtMaximumHitPoints() {
        Long userId = 9608L;
        Campaign campaign = campaignWithPlayer(userId, "Short Rest Cap");
        GameEntity player = player(campaign);
        player.getData().put("classId", "fighter");
        player.getData().put("hitDie", "d10");
        player.getData().put("hitDiceMax", 1);
        player.getData().put("hitDiceRemaining", 1);
        player.setStats(new com.dndmasters.domain.Stats(10, 10, 10, 10, 10, 10));
        player.setHp(9);
        player.setMaxHp(10);
        campaigns.saveEntity(player);

        ActionResult result = game.act(campaign.getId(), userId,
                action(PlayerAction.SHORT_REST, player.getId()));

        assertThat(result.ok()).isTrue();
        assertThat(campaigns.entity(player.getId()).orElseThrow().getHp()).isEqualTo(10);
    }

    @Test
    void restsAreRejectedDuringCombat() {
        Long userId = 9605L;
        Campaign campaign = campaignWithPlayer(userId, "Rest In Combat");
        GameState state = campaigns.state(campaign.getId());
        addEnemy(state);
        combat.startCombat(campaign.getId(), state.getScene().id());
        GameEntity player = player(campaign);

        ActionResult shortRest = game.act(campaign.getId(), userId,
                action(PlayerAction.SHORT_REST, player.getId()));
        ActionResult longRest = game.act(campaign.getId(), userId,
                action(PlayerAction.LONG_REST, player.getId()));

        assertThat(shortRest.ok()).isFalse();
        assertThat(shortRest.message()).containsIgnoringCase("combate");
        assertThat(longRest.ok()).isFalse();
        assertThat(longRest.message()).containsIgnoringCase("combate");
    }

    @Test
    void restsAreRejectedWhileDying() {
        Long userId = 9606L;
        Campaign campaign = campaignWithPlayer(userId, "Rest While Dying");
        GameEntity player = player(campaign);
        player.getData().put("dying", true);
        campaigns.saveEntity(player);

        ActionResult shortRest = game.act(campaign.getId(), userId,
                action(PlayerAction.SHORT_REST, player.getId()));
        ActionResult longRest = game.act(campaign.getId(), userId,
                action(PlayerAction.LONG_REST, player.getId()));

        assertThat(shortRest.ok()).isFalse();
        assertThat(shortRest.message()).containsIgnoringCase("moribundo");
        assertThat(longRest.ok()).isFalse();
        assertThat(longRest.message()).containsIgnoringCase("moribundo");
    }

    @Test
    void restsAreRejectedWhenTheCharacterIsDead() {
        Long userId = 9607L;
        Campaign campaign = campaignWithPlayer(userId, "Rest While Dead");
        GameEntity player = player(campaign);
        player.setAlive(false);
        player.setHp(0);
        player.getData().put("dying", false);
        player.getData().put("stable", false);
        campaigns.saveEntity(player);

        ActionResult result = game.act(campaign.getId(), userId,
                action(PlayerAction.LONG_REST, player.getId()));

        assertThat(result.ok()).isFalse();
        assertThat(result.message()).containsIgnoringCase("muerto");
    }
}
