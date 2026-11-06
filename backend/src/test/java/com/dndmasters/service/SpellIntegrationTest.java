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

/** Spell slots, targeting, attacks, saves and healing through the real services. */
@SpringBootTest
@TestPropertySource(properties = "spring.datasource.url=jdbc:sqlite:target/dnd-test.db")
class SpellIntegrationTest {

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
        player.getData().put("spellFocus", true);
        player.getData().put("actionUsed", false);
        player.getData().put("bonusUsed", false);
        player.setStats(new Stats(10, 10, 10, intelligence, wisdom, 10));
        campaigns.saveEntity(player);
    }

    private GameEntity createEnemy(Campaign campaign, GameEntity player) {
        return createEnemyAt(campaign, player, "Objetivo de conjuro", 1, 0);
    }

    private GameEntity createEnemyAt(Campaign campaign, GameEntity player, String name, int dx, int dy) {
        GameState state = campaigns.state(campaign.getId());
        GameEntity enemy = new GameEntity();
        enemy.setSceneId(state.getScene().id());
        enemy.setMapId(state.getMap().getId());
        enemy.setKind(EntityKind.ENEMY);
        enemy.setName(name);
        enemy.setHp(999);
        enemy.setMaxHp(999);
        enemy.setAc(12);
        enemy.setSpeed(6);
        enemy.setAlive(true);
        enemy.moveTo(new Cell(player.getX() + dx, player.getY() + dy));
        campaigns.saveEntity(enemy);
        return campaigns.state(campaign.getId()).getEntities().stream()
                .filter(entity -> entity.getKind() == EntityKind.ENEMY
                        && name.equals(entity.getName()))
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

    @Test
    void blessGrantsConcentrationAndBreaksWhenTheCasterTakesDamage() {
        Campaign campaign = campaignWithPlayer(9407L, "Spells Bless");
        GameEntity player = player(campaign);
        configureCaster(player, "cleric", 10, 16);
        player.setHp(999);
        player.setMaxHp(999);
        campaigns.saveEntity(player);

        ActionResult bless = game.act(campaign.getId(), 9407L,
                cast("bless", player.getId(), null));

        assertThat(bless.ok()).as(bless.message()).isTrue();
        assertThat(bless.events()).anyMatch(event -> "SPELL_BUFF".equals(event.type()));
        GameEntity reloaded = campaigns.entity(player.getId()).orElseThrow();
        assertThat(reloaded.getData().get("blessed")).isEqualTo(true);
        assertThat(reloaded.getData().get("concentrationSpell")).isEqualTo("bless");
        assertThat(reloaded.getData().get("spellSlotsUsed")).isEqualTo(1);

        GameState state = campaigns.state(campaign.getId());
        GameEntity blessedPlayer = campaigns.entity(player.getId()).orElseThrow();
        GameEntity enemy = createEnemy(campaign, player);
        CombatState transientState = new CombatState();
        transientState.setCampaignId(campaign.getId());
        transientState.setSceneId(state.getScene().id());
        transientState.setActive(false);
        transientState.setParticipants(new ArrayList<>());

        List<GameEvent> attackEvents = combat.attack(campaign.getId(), transientState, blessedPlayer, enemy,
                Attack.melee("Prueba bendecida", 100, "1d4"));
        GameEvent attackEvent = attackEvents.stream()
                .filter(event -> "ATTACK".equals(event.type()) || "ATTACK_MISS".equals(event.type()))
                .findFirst().orElseThrow();
        Map<String, Object> attackData = Json.readMap(attackEvent.dataJson());
        assertThat(((Number) attackData.get("blessBonus")).intValue()).isPositive();

        boolean broken = false;
        for (int attempt = 0; attempt < 5 && !broken; attempt++) {
            List<GameEvent> events = combat.attack(campaign.getId(), transientState, enemy, blessedPlayer,
                    Attack.melee("Golpe brutal", 100, "10d10"));
            broken = events.stream().anyMatch(event -> "CONCENTRATION_BROKEN".equals(event.type()));
        }

        assertThat(broken).isTrue();
        GameEntity after = campaigns.entity(player.getId()).orElseThrow();
        assertThat(after.getData().get("concentrationSpell")).isNull();
        assertThat(after.getData().get("blessed")).isEqualTo(false);
    }

    @Test
    void thunderwaveHitsNearbyCreaturesAndSkipsFarOnes() {
        Campaign campaign = campaignWithPlayer(9409L, "Spells Area");
        GameEntity player = player(campaign);
        configureCaster(player, "wizard", 16, 10);
        GameEntity near = createEnemyAt(campaign, player, "Cerca 1", 1, 0);
        GameEntity nearDiagonal = createEnemyAt(campaign, player, "Cerca 2", 0, 2);
        GameEntity far = createEnemyAt(campaign, player, "Lejos", 5, 0);

        ActionResult result = game.act(campaign.getId(), 9409L,
                cast("thunderwave", player.getId(), null));

        assertThat(result.ok()).isTrue();
        assertThat(result.events()).anyMatch(event -> "SPELL_CAST".equals(event.type()));
        assertAffected(result.events(), near, true);
        assertAffected(result.events(), nearDiagonal, true);
        assertAffected(result.events(), far, false);
        GameEntity reloaded = campaigns.entity(player.getId()).orElseThrow();
        assertThat(reloaded.getData().get("spellSlotsUsed")).isEqualTo(1);
    }

    @Test
    void materialComponentsRequireAFocus() {
        Campaign campaign = campaignWithPlayer(9411L, "Spells Components");
        GameEntity player = player(campaign);
        configureCaster(player, "cleric", 10, 16);
        player.getData().put("spellFocus", false);
        campaigns.saveEntity(player);

        ActionResult missing = game.act(campaign.getId(), 9411L,
                cast("bless", player.getId(), null));

        assertThat(missing.ok()).isFalse();
        assertThat(missing.message()).containsIgnoringCase("foco");

        GameEntity reloaded = campaigns.entity(player.getId()).orElseThrow();
        reloaded.getData().put("spellFocus", true);
        campaigns.saveEntity(reloaded);

        ActionResult withFocus = game.act(campaign.getId(), 9411L,
                cast("bless", player.getId(), null));
        assertThat(withFocus.ok()).isTrue();
        assertThat(withFocus.events()).anyMatch(event -> "SPELL_BUFF".equals(event.type()));
    }

    @Test
    void blessExpiresAtTheStartOfTheCastersTurn() {
        Campaign campaign = campaignWithPlayer(9408L, "Spells Bless Timer");
        GameEntity player = player(campaign);
        configureCaster(player, "cleric", 10, 16);

        ActionResult bless = game.act(campaign.getId(), 9408L,
                cast("bless", player.getId(), null));
        assertThat(bless.ok()).as(bless.message()).isTrue();

        GameEntity current = campaigns.entity(player.getId()).orElseThrow();
        current.getData().put("concentrationRounds", 1);
        campaigns.saveEntity(current);

        campaigns.startTurn(campaign.getId(), player.getId());

        GameEntity after = campaigns.entity(player.getId()).orElseThrow();
        assertThat(after.getData().get("concentrationSpell")).isNull();
        assertThat(after.getData().get("blessed")).isEqualTo(false);
    }

    private void assertAffected(List<GameEvent> events, GameEntity target, boolean expected) {
        boolean affected = events.stream()
                .filter(event -> "SPELL_SAVED".equals(event.type()) || "SPELL_DAMAGE".equals(event.type()))
                .map(event -> Json.readMap(event.dataJson()))
                .anyMatch(data -> data.get("targetId") instanceof Number number
                        && target.getId() != null
                        && number.longValue() == target.getId());
        assertThat(affected).as("target " + target.getName()).isEqualTo(expected);
    }
}
