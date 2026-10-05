package com.dndmasters.domain;

import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GameEntity {

    private Long id;
    private Long sceneId;
    private Long mapId;
    private EntityKind kind;
    private Long refId;
    private String name;
    private int x;
    private int y;
    private int hp;
    private int maxHp;
    private int ac;
    private int speed;
    private String color;
    private String icon;
    private String portrait;
    private boolean alive = true;
    private Stats stats = new Stats(10, 10, 10, 10, 10, 10);
    private Map<String, Object> data = new HashMap<>();

    public Cell cell() {
        return new Cell(x, y);
    }

    public void moveTo(Cell c) {
        this.x = c.x();
        this.y = c.y();
    }

    public boolean isCombatant() {
        return kind == EntityKind.PLAYER || kind == EntityKind.ENEMY;
    }
}
