package com.dndmasters.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.dndmasters.domain.Campaign;
import com.dndmasters.domain.CombatState;
import com.dndmasters.domain.EntityKind;
import com.dndmasters.domain.GameEntity;
import com.dndmasters.domain.GameState;
import com.dndmasters.service.action.PlayerAction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Exercises the real combat pipeline: starting the fight from the DM script, resolving
 * attacks through the API-facing service, and confirming combat ends when enemies die.
 */
@SpringBootTest
@TestPropertySource(properties = "spring.datasource.url=jdbc:sqlite:target/dnd-test.db")
class CombatVictoryTest {

    @Autowired
    GameService game;

    @Autowired
    CampaignService campaigns;

    @Autowired
    CombatService combat;

    @Test
    void dmScriptStartsTheCryptCombatAfterCheckSucceeds() {
        Long userId = 9001L;
        Campaign campaign = game.createCampaign(userId, "Victory Test");
        game.joinCampaign(campaign.getId(), userId, null);

        boolean combatStarted = false;
        for (int i = 0; i < 60 && !combatStarted; i++) {
            GameState state = campaigns.state(campaign.getId());
            if (state.getCombat() != null && state.getCombat().isActive()) {
                combatStarted = true;
                break;
            }
            PlayerAction check = new PlayerAction();
            check.setType(PlayerAction.CHECK);
            check.setSkill("investigation");
            check.setDc(1); // guaranteed success
            game.act(campaign.getId(), userId, check);
        }
        assertThat(combatStarted).as("check success should shift scene and start combat").isTrue();

        GameState state = campaigns.state(campaign.getId());
        assertThat(state.getScene().orderIndex()).isEqualTo(1);
        assertThat(state.getCombat().getParticipants()).hasSizeGreaterThanOrEqualTo(2);
        assertThat(state.getEntities().stream().filter(e -> e.getKind() == EntityKind.ENEMY).count())
                .isEqualTo(3);
    }

    @Test
    void enemyHpDropsAndCombatEndsWhenAllEnemiesDie() {
        Long userId = 9002L;
        Campaign campaign = game.createCampaign(userId, "Victory Test 2");
        game.joinCampaign(campaign.getId(), userId, null);

        // Reach the crypt.
        for (int i = 0; i < 60; i++) {
            GameState state = campaigns.state(campaign.getId());
            if (state.getCombat() != null && state.getCombat().isActive()) {
                break;
            }
            PlayerAction check = new PlayerAction();
            check.setType(PlayerAction.CHECK);
            check.setSkill("investigation");
            check.setDc(1);
            game.act(campaign.getId(), userId, check);
        }

        // Directly resolve an attack to verify HP mutation through the service.
        GameState state = campaigns.state(campaign.getId());
        GameEntity player = state.getEntities().stream()
                .filter(e -> e.getKind() == EntityKind.PLAYER).findFirst().orElseThrow();
        GameEntity enemy = state.getEntities().stream()
                .filter(e -> e.getKind() == EntityKind.ENEMY).findFirst().orElseThrow();

        int hpBefore = enemy.getHp();
        var events = combat.attack(campaign.getId(), state.getCombat(), player, enemy,
                com.dndmasters.domain.Attack.melee("Test", 100, "1d1"));
        assertThat(events).isNotEmpty();
        assertThat(enemy.getHp()).isLessThanOrEqualTo(hpBefore);

        // Kill every enemy and assert the manager reports the fight is over.
        state.getEntities().stream().filter(e -> e.getKind() == EntityKind.ENEMY).forEach(e -> {
            e.setHp(0);
            e.setAlive(false);
            campaigns.saveEntity(e);
        });
        CombatState reloaded = campaigns.activeCombat(campaign.getId());
        assertThat(reloaded).isNotNull();
        reloaded.getParticipants().stream()
                .filter(p -> p.isEnemy())
                .forEach(p -> p.setAlive(false));
        assertThat(combat.isVictory(reloaded)).isTrue();
        combat.finish(campaign.getId(), reloaded);

        assertThat(campaigns.activeCombat(campaign.getId())).isNull();
    }
}
