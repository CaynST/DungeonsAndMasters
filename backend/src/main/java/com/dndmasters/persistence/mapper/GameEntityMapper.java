package com.dndmasters.persistence.mapper;

import com.dndmasters.domain.Attack;
import com.dndmasters.domain.EntityKind;
import com.dndmasters.domain.GameEntity;
import com.dndmasters.domain.Stats;
import com.dndmasters.persistence.entity.BoardEntity;
import com.dndmasters.util.Json;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class GameEntityMapper {

    public GameEntity toDomain(BoardEntity e) {
        GameEntity g = new GameEntity();
        g.setId(e.getId());
        g.setSceneId(e.getSceneId());
        g.setMapId(e.getMapId());
        g.setKind(EntityKind.valueOf(e.getKind()));
        g.setRefId(e.getRefId());
        g.setName(e.getName());
        g.setX(e.getX());
        g.setY(e.getY());
        g.setHp(e.getHp());
        g.setMaxHp(e.getMaxHp());
        g.setAc(e.getAc());
        g.setSpeed(e.getSpeed());
        g.setColor(e.getColor());
        g.setIcon(e.getIcon());
        g.setPortrait(e.getPortrait());
        g.setAlive(e.isAlive());
        g.setData(Json.readMap(e.getDataJson()));
        Object stats = g.getData().get("stats");
        if (stats != null) {
            g.setStats(Json.convert(stats, Stats.class));
        }
        return g;
    }

    public BoardEntity toEntity(GameEntity g) {
        BoardEntity e = new BoardEntity();
        e.setId(g.getId());
        e.setSceneId(g.getSceneId());
        e.setMapId(g.getMapId());
        e.setKind(g.getKind().name());
        e.setRefId(g.getRefId());
        e.setName(g.getName());
        e.setX(g.getX());
        e.setY(g.getY());
        e.setHp(g.getHp());
        e.setMaxHp(g.getMaxHp());
        e.setAc(g.getAc());
        e.setSpeed(g.getSpeed());
        e.setColor(g.getColor());
        e.setIcon(g.getIcon());
        e.setPortrait(g.getPortrait());
        e.setAlive(g.isAlive());
        e.setDataJson(Json.write(g.getData()));
        return e;
    }

    public Stats statsOrDefault(GameEntity g) {
        return g.getStats() == null ? new Stats(10, 10, 10, 10, 10, 10) : g.getStats();
    }

    @SuppressWarnings("unchecked")
    public List<Attack> attacksOf(GameEntity g) {
        Object raw = g.getData().get("attacks");
        if (raw == null) {
            return List.of();
        }
        List<java.util.Map<String, Object>> maps = (List<java.util.Map<String, Object>>) raw;
        return maps.stream().map(m -> Json.convert(m, Attack.class)).toList();
    }
}
