package com.dndmasters.service.action;

import com.dndmasters.domain.GameEvent;
import java.util.List;
import java.util.Map;

/** Outcome of processing a {@link PlayerAction}. */
public record ActionResult(
        boolean ok,
        String message,
        String narration,
        List<GameEvent> events,
        CheckRequest checkRequest,
        Map<String, Object> roll,
        Map<String, Object> attack,
        List<String> dmActions,
        boolean adventureEnded) {

    public static ActionResult error(String message) {
        return new ActionResult(false, message, null, List.of(), null, null, null, List.of(), false);
    }
}
