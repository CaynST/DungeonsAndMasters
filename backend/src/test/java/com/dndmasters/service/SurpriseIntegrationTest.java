package com.dndmasters.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.dndmasters.domain.Campaign;
import com.dndmasters.ai.dm.DmAction;
import com.dndmasters.domain.CombatParticipant;
import com.dndmasters.domain.CombatState;
import com.dndmasters.domain.EntityKind;
import com.dndmasters.domain.GameEntity;
import com.dndmasters.domain.GameState;
import com.dndmasters.engine.combat.SurpriseRules;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/** Surprise is a flag on the entity that costs the creature its first combat turn. */
@SpringBootTest
@TestPropertySource(properties = "spring.datasource.url=jdbc:sqlite:target/dnd-test.db")
class SurpriseIntegrationTest {

    @Autowired
    GameService game;

    @Autowired
    CampaignService campaigns;

    @Autowired
    CombatService combat;

    @Test
    void aSurprisedParticipantSkipsItsFirstTurnAndTheFlagIsCleared() {
        Long userId = 9401L;
        Campaign campaign = game.createCampaign(userId, "Surprise Skip");
        game.joinCampaign(campaign.getId(), userId, null);
        GameState state = campaigns.state(campaign.getId());
        addEnemy(state);

        CombatState combatState = combat.startCombat(campaign.getId(), state.getScene().id());
        CombatParticipant current = combatState.currentParticipant();
        CombatParticipant next = combatState.getParticipants()
                .get((combatState.getCurrentTurnIndex() + 1) % combatState.getParticipants().size());
        GameEntity nextEntity = campaigns.entity(next.getEntityId()).orElseThrow();
        SurpriseRules.markSurprised(nextEntity);
        campaigns.saveEntity(nextEntity);

        CombatParticipant after = combat.advanceTurn(campaign.getId(), combatState);

        assertThat(after.getEntityId()).isEqualTo(current.getEntityId());
        assertThat(SurpriseRules.isSurprised(campaigns.entity(next.getEntityId()).orElseThrow())).isFalse();
        assertThat(campaigns.state(campaign.getId()).getRecentEvents())
                .anyMatch(event -> "SURPRISED".equals(event.type()));
    }

    @Test
    void startingCombatSkipsSurprisedCreaturesBeforeTheFirstRoundResolves() {
        Long userId = 9402L;
        Campaign campaign = game.createCampaign(userId, "Surprise Start");
        game.joinCampaign(campaign.getId(), userId, null);
        GameState state = campaigns.state(campaign.getId());
        addEnemy(state);

        campaigns.state(campaign.getId()).getEntities().stream()
                .filter(GameEntity::isCombatant)
                .forEach(entity -> {
                    entity.getData().put("surprised", true);
                    campaigns.saveEntity(entity);
                });

        CombatState combatState = combat.startCombat(campaign.getId(), state.getScene().id());

        // Every combatant lost the surprised round, so play resumes in round 2.
        assertThat(combatState.getRound()).isEqualTo(2);
        combatState.getParticipants().forEach(participant ->
                assertThat(SurpriseRules.isSurprised(
                        campaigns.entity(participant.getEntityId()).orElseThrow())).isFalse());
    }

    @Test
    void dmCanMarkATokenAsSurprised() {
        Long userId = 9403L;
        Campaign campaign = game.createCampaign(userId, "Surprise DM");
        game.joinCampaign(campaign.getId(), userId, null);
        GameState state = campaigns.state(campaign.getId());
        GameEntity player = state.getEntities().stream()
                .filter(entity -> entity.getKind() == EntityKind.PLAYER)
                .findFirst().orElseThrow();

        game.applyDmActions(campaign.getId(), userId,
                java.util.List.of(DmAction.setSurprised(player.getId())));

        assertThat(SurpriseRules.isSurprised(campaigns.entity(player.getId()).orElseThrow())).isTrue();
        assertThat(campaigns.state(campaign.getId()).getRecentEvents())
                .anyMatch(event -> "SURPRISE_APPLIED".equals(event.type()));
    }

    private void addEnemy(GameState state) {
        GameEntity enemy = new GameEntity();
        enemy.setSceneId(state.getScene().id());
        enemy.setMapId(state.getMap().getId());
        enemy.setKind(EntityKind.ENEMY);
        enemy.setName("Goblin sorprendido");
        enemy.setHp(7);
        enemy.setMaxHp(7);
        enemy.setAc(13);
        enemy.setSpeed(6);
        enemy.setAlive(true);
        enemy.moveTo(new com.dndmasters.domain.Cell(8, 5));
        campaigns.saveEntity(enemy);
    }
}
