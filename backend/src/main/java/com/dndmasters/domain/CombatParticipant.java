package com.dndmasters.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CombatParticipant {

    private Long id;
    private Long entityId;
    private String name;
    private EntityKind kind;
    private int initiative;
    private int orderIndex;
    private boolean alive;

    public boolean isEnemy() {
        return kind == EntityKind.ENEMY;
    }
}
