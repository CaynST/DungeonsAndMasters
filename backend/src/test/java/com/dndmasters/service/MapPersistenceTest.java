package com.dndmasters.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.dndmasters.domain.Campaign;
import com.dndmasters.domain.Cell;
import com.dndmasters.domain.GameMap;
import com.dndmasters.persistence.mapper.MapMapper;
import com.dndmasters.persistence.repository.MapEntityRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/** Verifies the V11 column/migration against the SQLite test database. */
@SpringBootTest
@TestPropertySource(properties = "spring.datasource.url=jdbc:sqlite:target/dnd-test.db")
class MapPersistenceTest {

    @Autowired
    GameService game;

    @Autowired
    CampaignService campaigns;

    @Autowired
    MapEntityRepository maps;

    @Autowired
    MapMapper mapMapper;

    @Test
    void persistsDifficultTerrainInTheMapsTable() {
        Long userId = 9501L;
        Campaign campaign = game.createCampaign(userId, "Difficult Terrain Persistence");
        game.joinCampaign(campaign.getId(), userId, null);

        GameMap map = campaigns.state(campaign.getId()).getMap();
        List<Cell> terrain = List.of(new Cell(2, 2), new Cell(3, 2));
        map.setDifficultTerrain(terrain);
        maps.save(mapMapper.toEntity(map, campaign.getId()));

        GameMap reloaded = mapMapper.toDomain(maps.findById(map.getId()).orElseThrow());

        assertThat(reloaded.getDifficultTerrain()).containsExactlyElementsOf(terrain);
        assertThat(reloaded.isDifficultTerrain(new Cell(2, 2))).isTrue();
        assertThat(reloaded.isDifficultTerrain(new Cell(1, 2))).isFalse();
    }
}
