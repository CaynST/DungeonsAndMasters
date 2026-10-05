package com.dndmasters.ai.dm;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Structured context handed to the Dungeon Master. The AI never sees raw database rows. */
@Getter
@Setter
@NoArgsConstructor
public class DmContext {

    private String trigger;
    private Long campaignId;
    private String campaignName;
    private String phase;
    private String objective;
    private int sceneOrder;
    private String sceneTitle;
    private String sceneKind;
    private String sceneDescription;
    private boolean combatActive;
    private int round;
    private String currentTurn;
    private String spotlight;
    private String lastAction;
    private String skill;
    private Boolean checkSuccess;
    private List<Map<String, Object>> characters = new ArrayList<>();
    private List<Map<String, Object>> enemies = new ArrayList<>();
    private List<Map<String, Object>> npcs = new ArrayList<>();
    private List<String> recentEvents = new ArrayList<>();

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("trigger", trigger);
        map.put("campaignId", campaignId);
        map.put("campaignName", campaignName);
        map.put("phase", phase);
        map.put("objective", objective);
        map.put("sceneOrder", sceneOrder);
        map.put("sceneTitle", sceneTitle);
        map.put("sceneKind", sceneKind);
        map.put("sceneDescription", sceneDescription);
        map.put("combatActive", combatActive);
        map.put("round", round);
        map.put("currentTurn", currentTurn);
        map.put("spotlight", spotlight);
        map.put("lastAction", lastAction);
        map.put("skill", skill);
        map.put("checkSuccess", checkSuccess);
        map.put("characters", characters);
        map.put("enemies", enemies);
        map.put("npcs", npcs);
        map.put("recentEvents", recentEvents);
        return map;
    }
}
