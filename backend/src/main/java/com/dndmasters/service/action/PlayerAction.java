package com.dndmasters.service.action;

import com.dndmasters.domain.Cell;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A player intent coming from the client. Mechanical validation happens in the engine. */
@Getter
@Setter
@NoArgsConstructor
public class PlayerAction {

    public static final String MOVE = "MOVE";
    public static final String ATTACK = "ATTACK";
    public static final String CHECK = "CHECK";
    public static final String END_TURN = "END_TURN";
    public static final String DIALOGUE = "DIALOGUE";
    public static final String NARRATE = "NARRATE";
    public static final String DICE = "DICE";
    public static final String USE_ITEM = "USE_ITEM";
    public static final String PASS = "PASS";
    public static final String DEATH_SAVE = "DEATH_SAVE";
    public static final String DASH = "DASH";
    public static final String DODGE = "DODGE";
    public static final String DISENGAGE = "DISENGAGE";
    public static final String STAND_UP = "STAND_UP";

    private String type;
    private Long actorEntityId;
    private Long targetEntityId;
    private List<Cell> path = new ArrayList<>();
    private String attack;
    private String skill;
    private Integer dc;
    private String expression;
    private String text;
    private String item;
}
