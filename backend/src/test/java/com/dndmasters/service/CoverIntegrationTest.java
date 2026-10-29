package com.dndmasters.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.dndmasters.domain.Attack;
import com.dndmasters.domain.Campaign;
import com.dndmasters.domain.Cell;
import com.dndmasters.domain.CombatState;
import com.dndmasters.domain.EntityKind;
import com.dndmasters.domain.GameEntity;
import com.dndmasters.domain.GameEvent;
import com.dndmasters.domain.GameMap;
import com.dndmasters.domain.GameState;
import com.dndmasters.persistence.mapper.MapMapper;
import com.dndmasters.persistence.repository.MapEntityRepository;
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

/** Cover is computed by the engine from the persisted map, not by the client. */
@SpringBootTest
@TestPropertySource(properties = "spring.datasource.url=jdbc:sqlite:target/dnd-test.db")
class CoverIntegrationTest {

    @Autowired
    GameService game;

    @Autowired
    CampaignService campaigns;

    @Autowired
    CombatService combat;

    @Autowired
    MapEntityRepository maps;

    @Autowired
    MapMapper mapMapper;

    private Campaign campaignWithOpponent(Long userId, String name) {
        Campaign campaign = game.createCampaign(userId, name);
        game.joinCampaign(campaign.getId(), userId, null);
        return campaign;
    }

    private GameEntity entityOfKind(GameState state, EntityKind kind) {
        return state.getEntities().stream()
                .filter(entity -> entity.getKind() == kind)
                .findFirst().orElseThrow();
    }

    /** Finds a clear vertical run of three cells and returns its top-left coordinate. */
    private Cell clearVerticalLine(GameMap map) {
        for (int y = 1; y < map.getHeight() - 3; y++) {
            for (int x = 1; x < map.getWidth() - 1; x++) {
                Cell top = new Cell(x, y);
                Cell middle = new Cell(x, y + 1);
                Cell bottom = new Cell(x, y + 2);
                if (!map.blocksMovement(top) && !map.blocksMovement(middle) && !map.blocksMovement(bottom)) {
                    return top;
                }
            }
        }
        throw new AssertionError("No clear vertical line on the map");
    }

    private void saveMap(GameMap map, Long campaignId) {
        var entity = mapMapper.toEntity(map, campaignId);
        maps.save(entity);
    }

    @Test
    void totalCoverRejectsAPlayerRangedAttack() {
        Long userId = 9201L;
        Campaign campaign = campaignWithOpponent(userId, "Cover Total");
        GameState state = campaigns.state(campaign.getId());
        GameEntity player = entityOfKind(state, EntityKind.PLAYER);
        GameEntity target = state.getEntities().stream()
                .filter(entity -> entity.getId() != player.getId())
                .findFirst().orElseThrow();

        GameMap map = campaigns.mapForScene(state.getScene().id());
        Cell top = clearVerticalLine(map);
        map.getWalls().add(new Cell(top.x(), top.y() + 1));
        saveMap(map, campaign.getId());

        player.moveTo(top);
        player.getData().put("attacks", List.of(Attack.ranged("Arco de prueba", 5, "1d8", 10)));
        campaigns.saveEntity(player);
        target.moveTo(new Cell(top.x(), top.y() + 2));
        campaigns.saveEntity(target);

        PlayerAction attack = new PlayerAction();
        attack.setType(PlayerAction.ATTACK);
        attack.setActorEntityId(player.getId());
        attack.setTargetEntityId(target.getId());
        attack.setAttack("Arco de prueba");

        ActionResult result = game.act(campaign.getId(), userId, attack);

        assertThat(result.ok()).isFalse();
        assertThat(result.message()).containsIgnoringCase("cobertura total");
    }

    @Test
    void halfCoverAddsTwoToTheTargetAc() {
        Long userId = 9202L;
        Campaign campaign = campaignWithOpponent(userId, "Cover Half");
        GameState state = campaigns.state(campaign.getId());
        GameEntity player = entityOfKind(state, EntityKind.PLAYER);
        GameEntity target = state.getEntities().stream()
                .filter(entity -> entity.getId() != player.getId())
                .findFirst().orElseThrow();

        GameMap map = campaigns.mapForScene(state.getScene().id());
        Cell top = clearVerticalLine(map);
        map.getObstacles().add(new Cell(top.x(), top.y() + 1));
        saveMap(map, campaign.getId());

        player.moveTo(top);
        campaigns.saveEntity(player);
        target.moveTo(new Cell(top.x(), top.y() + 2));
        campaigns.saveEntity(target);

        CombatState transientState = new CombatState();
        transientState.setCampaignId(campaign.getId());
        transientState.setSceneId(state.getScene().id());
        transientState.setActive(false);
        transientState.setParticipants(new ArrayList<>());

        List<GameEvent> events = combat.attack(campaign.getId(), transientState, player, target,
                Attack.ranged("Arco de prueba", 5, "1d8", 10));

        GameEvent attackEvent = events.stream()
                .filter(event -> "ATTACK".equals(event.type()) || "ATTACK_MISS".equals(event.type()))
                .findFirst().orElseThrow();
        Map<String, Object> data = Json.readMap(attackEvent.dataJson());
        assertThat(data.get("cover")).isEqualTo("HALF");
        assertThat(data.get("coverBonus")).isEqualTo(2);
        assertThat(data.get("ac")).isEqualTo(target.getAc() + 2);
    }
}
