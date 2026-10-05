package com.dndmasters.web;

import com.dndmasters.domain.GameEvent;
import com.dndmasters.security.SecurityUtils;
import com.dndmasters.service.GameEventService;
import com.dndmasters.service.GameService;
import com.dndmasters.service.action.ActionResult;
import com.dndmasters.service.action.PlayerAction;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/campaigns/{id}")
public class GameController {

    private final GameService game;
    private final GameEventService events;

    public GameController(GameService game, GameEventService events) {
        this.game = game;
        this.events = events;
    }

    @PostMapping("/actions")
    public ActionResult act(@PathVariable Long id, @RequestBody PlayerAction action) {
        return game.act(id, SecurityUtils.requireUserId(), action);
    }

    @GetMapping("/events")
    public List<GameEvent> events(@PathVariable Long id) {
        return events.history(id);
    }

    @GetMapping("/provider")
    public java.util.Map<String, String> provider(@PathVariable Long id) {
        return java.util.Map.of("aiProvider", game.dmProviderId());
    }
}
