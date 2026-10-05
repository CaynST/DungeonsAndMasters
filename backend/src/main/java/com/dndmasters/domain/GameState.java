package com.dndmasters.domain;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Everything the client (and the AI DM) needs to render and reason about a campaign. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GameState {

    private Campaign campaign;
    private Scene scene;
    private GameMap map;
    private List<GameEntity> entities = new ArrayList<>();
    private List<PlayerCharacter> party = new ArrayList<>();
    private CombatState combat;
    private List<GameEvent> recentEvents = new ArrayList<>();
    private String objective;
}
