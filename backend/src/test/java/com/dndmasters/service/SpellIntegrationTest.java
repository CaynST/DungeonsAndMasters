package com.dndmasters.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.dndmasters.domain.Campaign;
import com.dndmasters.domain.Cell;
import com.dndmasters.domain.EntityKind;
import com.dndmasters.domain.GameEntity;
import com.dndmasters.domain.GameEvent;
import com.dndmasters.domain.GameState;
import com.dndmasters.domain.Stats;
import com.dndmasters.service.action.ActionResult;
import com.dndmasters.service.action.PlayerAction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/** Spell slots, targeting, attacks, saves and healing through the real services. */
@SpringBootTest
@TestPropertySource(properties = "spring.datasource.url=jdbc:sqlite:target/dnd-test.db")
class SpellIntegrationTest {

    @Autowired
    GameService game;

    @Autowired
    CampaignService campaigns;

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

    private PlayerAction cast(String spell, Long actorId, Long targetId) {
        PlayerAction action = new PlayerAction();
        action.setType(PlayerAction.CAST_SPELL);
        action.setActorEntityId(actorId);
        action.setTargetEntityId(targetId);
        action.setSpell(spell);
        return action;
    }

    private void configureCaster(GameEntity player, String classId, int intelligence, int wisdom) {
        player.getData().put("classId", classId);
        player.getData().put("class", classId);
        player.getData().put("level", 1);
        player.getData().put("spellSlotsUsed", 0);
        player.getData().put("actionUsed", false);
        player.getData().put("bonusUsed", false);
        player.setStats(new Stats(10, 10, 10, intelligence, wisdom, 10));
        campaigns.saveEntity(player);
    }

    private GameEntity createEnemy(Campaign campaign, GameEntity player) {
        GameState state = campaigns.state(campaign.getId());
        GameEntity enemy = new GameEntity();
        enemy.setSceneId(state.getScene().id());
        enemy.setMapId(state.getMap().getId());
        enemy.setKind(EntityKind.ENEMY);
        enemy.setName("Objetivo de conjuro");
        enemy.setHp(999);
        enemy.setMaxHp(999);
        enemy.setAc(12);
        enemy.setSpeed(6);
        enemy.setAlive(true);
        enemy.moveTo(new Cell(player.getX() + 1, player.getY()));
        campaigns.saveEntity(enemy);
        return campaigns.state(campaign.getId()).getEntities().stream()
                .filter(entity -> entity.getKind() == EntityKind.ENEMY
                        && "Objetivo de conjuro".equals(entity.getName()))
                .findFirst().orElseThrow();
    }

    @Test
    void nonCastersCannotCastSpells() {
        Campaign campaign = campaignWithPlayer(9401L, "Spells No Caster");
        GameEntity player = player(campaign);
        player.getData().put("classId", "fighter");
        campaigns.saveEntity(player);

        ActionResult result = game.act(campaign.getId(), 9401L,
                cast("fire_bolt", player.getId(), null));

        assertThat(result.ok()).isFalse();
        assertThat(result.message()).containsIgnoringCase("no lanza conjuros");
    }

    @Test
    void wizardCantripConsumesTheActionButNoSlot() {
        Campaign campaign = campaignWithPlayer(9402L, "Spells Cantrip");
        GameEntity player = player(campaign);
        configureCaster(player, "wizard", 16, 10);
        GameEntity target = createEnemy(campaign, player);

        ActionResult result = game.act(campaign.getId(), 9402L,
                cast("fire_bolt", player.getId(), target.getId()));

        assertThat(result.ok()).isTrue();
        assertThat(result.events()).anyMatch(event -> "SPELL_CAST".equals(event.type()));
        assertThat(result.events()).anyMatch(event ->
                "ATTACK".equals(event.type()) || "ATTACK_MISS".equals(event.type()));
        GameEntity reloaded = campaigns.entity(player.getId()).orElseThrow();
        assertThat(reloaded.getData().get("spellSlotsUsed")).isEqualTo(0);
        assertThat(reloaded.getData().get("actionUsed")).isEqualTo(true);
    }

    @Test
    void wizardSpellSlotsAreConsumedAndLimited() {
        Campaign campaign = campaignWithPlayer(9403L, "Spells Slots");
        GameEntity player = player(campaign);
        configureCaster(player, "wizard", 16, 10);
        GameEntity target = createEnemy(campaign, player);

        ActionResult first = game.act(campaign.getId(), 9403L,
                cast("magic_missile", player.getId(), target.getId()));
        assertThat(first.ok()).isTrue();
        assertThat(first.events()).anyMatch(event -> "SPELL_DAMAGE".equals(event.type()));
        GameEntity reloaded = campaigns.entity(player.getId()).orElseThrow();
        assertThat(reloaded.getData().get("spellSlotsUsed")).isEqualTo(1);

        campaigns.resetTurnUsage(player.getId());
        ActionResult second = game.act(campaign.getId(), 9403L,
                cast("magic_missile", player.getId(), target.getId()));
        assertThat(second.ok()).isTrue();
        reloaded = campaigns.entity(player.getId()).orElseThrow();
        assertThat(reloaded.getData().get("spellSlotsUsed")).isEqualTo(2);

        campaigns.resetTurnUsage(player.getId());
        ActionResult third = game.act(campaign.getId(), 9403L,
                cast("magic_missile", player.getId(), target.getId()));
        assertThat(third.ok()).isFalse();
        assertThat(third.message()).containsIgnoringCase("espacios de conjuro");
    }

    @Test
    void clericHealsWithAnActionAndWithHealingWordAsBonusAction() {
        Campaign campaign = campaignWithPlayer(9404L, "Spells Heal");
        GameEntity player = player(campaign);
        configureCaster(player, "cleric", 10, 16);
        player.setHp(10);
        player.setMaxHp(30);
        campaigns.saveEntity(player);

        ActionResult cure = game.act(campaign.getId(), 9404L,
                cast("cure_wounds", player.getId(), null));
        assertThat(cure.ok()).isTrue();
        assertThat(cure.events()).anyMatch(event -> "SPELL_HEAL".equals(event.type()));
        GameEntity reloaded = campaigns.entity(player.getId()).orElseThrow();
        assertThat(reloaded.getHp()).isGreaterThan(10);
        assertThat(reloaded.getData().get("spellSlotsUsed")).isEqualTo(1);
        assertThat(reloaded.getData().get("actionUsed")).isEqualTo(true);

        campaigns.resetTurnUsage(player.getId());
        ActionResult word = game.act(campaign.getId(), 9404L,
                cast("healing_word", player.getId(), null));
        assertThat(word.ok()).isTrue();
        reloaded = campaigns.entity(player.getId()).orElseThrow();
        assertThat(reloaded.getData().get("spellSlotsUsed")).isEqualTo(2);
        assertThat(reloaded.getData().get("bonusUsed")).isEqualTo(true);
    }

    @Test
    void sacredFlameResolvesASavingThrowAndDoesNotUseASlot() {
        Campaign campaign = campaignWithPlayer(9405L, "Spells Save");
        GameEntity player = player(campaign);
        configureCaster(player, "cleric", 10, 16);
        GameEntity target = createEnemy(campaign, player);

        ActionResult result = game.act(campaign.getId(), 9405L,
                cast("sacred_flame", player.getId(), target.getId()));

        assertThat(result.ok()).isTrue();
        assertThat(result.events()).anyMatch(event -> "SPELL_CAST".equals(event.type()));
        assertThat(result.events()).anyMatch(event ->
                "SPELL_SAVED".equals(event.type()) || "SPELL_DAMAGE".equals(event.type()));
        GameEntity reloaded = campaigns.entity(player.getId()).orElseThrow();
        assertThat(reloaded.getData().get("spellSlotsUsed")).isEqualTo(0);
    }

    @Test
    void gameStateExposesTheStaticSpellbooks() {
        Campaign campaign = campaignWithPlayer(9406L, "Spells Catalog");

        GameState state = campaigns.state(campaign.getId());

        assertThat(state.getSpellbook()).containsKeys("wizard", "cleric");
        assertThat(state.getSpellbook().get("wizard").spells()).isNotEmpty();
    }
}
