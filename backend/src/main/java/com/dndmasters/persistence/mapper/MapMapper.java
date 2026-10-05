package com.dndmasters.persistence.mapper;

import com.dndmasters.domain.Cell;
import com.dndmasters.domain.Decor;
import com.dndmasters.domain.Door;
import com.dndmasters.domain.GameMap;
import com.dndmasters.persistence.entity.MapEntity;
import com.dndmasters.util.Json;
import org.springframework.stereotype.Component;

@Component
public class MapMapper {

    public GameMap toDomain(MapEntity e) {
        GameMap m = new GameMap();
        m.setId(e.getId());
        m.setName(e.getName());
        m.setWidth(e.getWidth());
        m.setHeight(e.getHeight());
        m.setTileSize(e.getTileSize());
        m.setWalls(Json.readList(e.getWallsJson(), Cell.class));
        m.setDoors(Json.readList(e.getDoorsJson(), Door.class));
        m.setObstacles(Json.readList(e.getObstaclesJson(), Cell.class));
        m.setDecor(Json.readList(e.getDecorJson(), Decor.class));
        return m;
    }

    public MapEntity toEntity(GameMap m, Long campaignId) {
        MapEntity e = new MapEntity();
        e.setId(m.getId());
        e.setCampaignId(campaignId);
        e.setName(m.getName());
        e.setWidth(m.getWidth());
        e.setHeight(m.getHeight());
        e.setTileSize(m.getTileSize());
        e.setWallsJson(Json.write(m.getWalls()));
        e.setDoorsJson(Json.write(m.getDoors()));
        e.setObstaclesJson(Json.write(m.getObstacles()));
        e.setDecorJson(Json.write(m.getDecor()));
        return e;
    }
}
