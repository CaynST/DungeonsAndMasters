package com.dndmasters.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.dndmasters.ai.dm.DmAction;
import com.dndmasters.content.Bestiary;
import com.dndmasters.domain.Attack;
import com.dndmasters.domain.Campaign;
import com.dndmasters.domain.Cell;
import com.dndmasters.domain.CombatParticipant;
import com.dndmasters.domain.CombatState;
import com.dndmasters.domain.EntityKind;
import com.dndmasters.domain.GameEntity;
import com.dndmasters.domain.GameEvent;
import com.dndmasters.domain.GameMap;
import com.dndmasters.domain.GameState;
import com.dndmasters.util.Json;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/** DM-spawned bestiary enemies must exist on the board and join an active combat. */
@SpringBootTest
@TestPropertySource(properties = "spring.datasource.url=jdbc:sqlite:target/dnd-test.db")
class EnemySpawnIntegrationTest {

    @Autowired
    GameService game;

    @Autowired
    CampaignService campaigns;

    @Autowired
    CombatService combat;

    @Test
    void spawnsABestiaryEnemyDuringExploration() {
        Long userId = 9601L;
        Campaign campaign = campaignWithPlayer(userId, "Spawn Exploration");
        GameState before = campaigns.state(campaign.getId());

        List<GameEvent> events = game.applyDmActions(campaign.getId(), userId,
                List.of(DmAction.spawnEnemy("  gObLiN ")));

        GameEntity spawned = spawnedEntity(campaign, events);
        GameMap map = before.getMap();
        assertThat(spawned.getKind()).isEqualTo(EntityKind.ENEMY);
        assertThat(spawned.getName()).isEqualTo("Goblin");
        assertThat(spawned.isAlive()).isTrue();
        assertThat(spawned.getHp()).isEqualTo(7);
        assertThat(spawned.getMaxHp()).isEqualTo(7);
        assertThat(spawned.getAc()).isEqualTo(13);
        assertThat(spawned.getSpeed()).isEqualTo(6);
        assertThat(spawned.getPortrait()).isEqualTo("/images/enemies/goblin.png");
        assertThat(map.inBounds(spawned.cell())).isTrue();
        assertThat(map.blocksMovement(spawned.cell())).isFalse();
        assertThat(before.getEntities()).noneMatch(e -> e.cell().equals(spawned.cell()));
        assertThat(spawned.getData().get("behavior")).isEqualTo("MELEE");
        assertThat(spawned.getData().get("monsterId")).isEqualTo("goblin");
        assertThat(spawned.getData().get("stats")).isNotNull();
        assertThat((List<?>) spawned.getData().get("attacks")).hasSize(1);

        GameEvent event = event(events, "ENEMY_SPAWNED");
        Map<String, Object> data = Json.readMap(event.dataJson());
        assertThat(((Number) data.get("entityId")).longValue()).isEqualTo(spawned.getId());
        assertThat(((Number) data.get("sceneId")).longValue()).isEqualTo(before.getScene().id());
        assertThat(data.get("monsterId")).isEqualTo("goblin");
        assertThat(data.get("x")).isEqualTo(spawned.getX());
        assertThat(data.get("y")).isEqualTo(spawned.getY());

        assertThat(game.applyDmActionNames(campaign.getId(), userId,
                List.of(DmAction.spawnEnemy("goblin")))).containsExactly("SPAWN_ENEMY:Goblin");
    }

    @Test
    void spawnedEnemyJoinsAnActiveCombatWithItsOwnTurn() {
        Long userId = 9602L;
        Campaign campaign = campaignWithPlayer(userId, "Spawn Combat");
        GameState initial = campaigns.state(campaign.getId());
        addEnemy(initial, "Guardia de prueba", new Cell(8, 5));
        CombatState running = combat.startCombat(campaign.getId(), initial.getScene().id());
        CombatParticipant currentBefore = running.currentParticipant();

        List<GameEvent> events = game.applyDmActions(campaign.getId(), userId,
                List.of(DmAction.spawnEnemy("Lobo")));

        GameEntity spawned = spawnedEntity(campaign, events);
        CombatState after = campaigns.activeCombat(campaign.getId());
        CombatParticipant participant = after.getParticipants().stream()
                .filter(p -> p.getEntityId().equals(spawned.getId()))
                .findFirst().orElseThrow();
        assertThat(participant.getName()).isEqualTo("Lobo");
        assertThat(participant.isEnemy()).isTrue();
        assertThat(participant.isAlive()).isTrue();
        assertThat(participant.getInitiative()).isBetween(1, 25);
        assertThat(participant.getOrderIndex()).isBetween(0, after.getParticipants().size() - 1);
        // Inserting the newcomer must not steal the turn from the active creature.
        assertThat(after.currentParticipant().getEntityId()).isEqualTo(currentBefore.getEntityId());

        boolean reachedTurn = false;
        for (int i = 0; i < after.getParticipants().size() * 2 && !reachedTurn; i++) {
            CombatParticipant next = combat.advanceTurn(campaign.getId(), after);
            reachedTurn = next != null && next.getEntityId().equals(spawned.getId());
        }
        assertThat(reachedTurn).isTrue();

        GameEvent event = event(events, "ENEMY_SPAWNED");
        Map<String, Object> data = Json.readMap(event.dataJson());
        assertThat(((Number) data.get("initiative")).intValue()).isEqualTo(participant.getInitiative());
        assertThat(((Number) data.get("combatId")).longValue()).isEqualTo(after.getId());
    }

    @Test
    void rejectsUnknownMonstersWithoutThrowing() {
        Long userId = 9603L;
        Campaign campaign = campaignWithPlayer(userId, "Spawn Unknown");
        int entitiesBefore = campaigns.state(campaign.getId()).getEntities().size();

        List<GameEvent> events = game.applyDmActions(campaign.getId(), userId,
                List.of(DmAction.spawnEnemy("Dragón de humo")));

        assertThat(campaigns.state(campaign.getId()).getEntities()).hasSize(entitiesBefore);
        GameEvent failed = event(events, "ENEMY_SPAWN_FAILED");
        assertThat(Json.readMap(failed.dataJson()).get("reason")).isEqualTo("UNKNOWN_MONSTER");
        assertThat(game.applyDmActionNames(campaign.getId(), userId,
                List.of(DmAction.spawnEnemy("Dragón de humo"))))
                .containsExactly("SPAWN_ENEMY_FAILED:UNKNOWN_MONSTER");
    }

    @Test
    void rejectsTheSpawnWhenTheMapHasNoFreeCell() {
        Long userId = 9604L;
        Campaign campaign = campaignWithPlayer(userId, "Spawn Full Map");
        GameState state = campaigns.state(campaign.getId());
        fillEveryFreeCell(state);
        int entitiesBefore = campaigns.state(campaign.getId()).getEntities().size();

        List<GameEvent> events = game.applyDmActions(campaign.getId(), userId,
                List.of(DmAction.spawnEnemy("Lobo")));

        assertThat(campaigns.state(campaign.getId()).getEntities()).hasSize(entitiesBefore);
        GameEvent failed = event(events, "ENEMY_SPAWN_FAILED");
        assertThat(Json.readMap(failed.dataJson()).get("reason")).isEqualTo("NO_FREE_CELL");
        assertThat(game.applyDmActionNames(campaign.getId(), userId,
                List.of(DmAction.spawnEnemy("Lobo"))))
                .containsExactly("SPAWN_ENEMY_FAILED:NO_FREE_CELL");
    }

    private Campaign campaignWithPlayer(Long userId, String name) {
        Campaign campaign = game.createCampaign(userId, name);
        game.joinCampaign(campaign.getId(), userId, null);
        return campaign;
    }

    private void addEnemy(GameState state, String name, Cell cell) {
        GameEntity enemy = new GameEntity();
        enemy.setSceneId(state.getScene().id());
        enemy.setMapId(state.getMap().getId());
        enemy.setKind(EntityKind.ENEMY);
        enemy.setName(name);
        enemy.setHp(10);
        enemy.setMaxHp(10);
        enemy.setAc(13);
        enemy.setSpeed(6);
        enemy.setAlive(true);
        enemy.moveTo(cell);
        campaigns.saveEntity(enemy);
    }

    private GameEntity spawnedEntity(Campaign campaign, List<GameEvent> events) {
        Map<String, Object> data = Json.readMap(event(events, "ENEMY_SPAWNED").dataJson());
        Long entityId = ((Number) data.get("entityId")).longValue();
        return campaigns.entity(entityId).orElseThrow();
    }

    private GameEvent event(List<GameEvent> events, String type) {
        return events.stream().filter(item -> type.equals(item.type())).findFirst().orElseThrow();
    }

    /**
     * Fills every walkable cell with a dummy item. Placement must then report the map as full
     * instead of overlapping a token or throwing.
     */
    private void fillEveryFreeCell(GameState state) {
        GameMap map = state.getMap();
        Set<Cell> occupied = new HashSet<>();
        state.getEntities().forEach(entity -> occupied.add(entity.cell()));
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                Cell cell = new Cell(x, y);
                if (map.blocksMovement(cell) || occupied.contains(cell)) {
                    continue;
                }
                GameEntity blocker = new GameEntity();
                blocker.setSceneId(state.getScene().id());
                blocker.setMapId(map.getId());
                blocker.setKind(EntityKind.ITEM);
                blocker.setName("Bloqueo " + x + "," + y);
                blocker.setHp(1);
                blocker.setMaxHp(1);
                blocker.setAlive(true);
                blocker.moveTo(cell);
                campaigns.saveEntity(blocker);
            }
        }
    }
}
