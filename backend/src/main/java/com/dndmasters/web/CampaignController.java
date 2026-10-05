package com.dndmasters.web;

import com.dndmasters.domain.Campaign;
import com.dndmasters.domain.GameState;
import com.dndmasters.security.SecurityUtils;
import com.dndmasters.service.GameService;
import com.dndmasters.service.action.ActionResult;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/campaigns")
public class CampaignController {

    private final GameService game;

    public CampaignController(GameService game) {
        this.game = game;
    }

    public record CreateCampaignRequest(String name, String edition) {
    }

    public record JoinRequest(Long characterId) {
    }

    @GetMapping
    public List<Campaign> list() {
        return game.campaigns();
    }

    @PostMapping
    public Campaign create(@RequestBody(required = false) CreateCampaignRequest request) {
        Long userId = SecurityUtils.requireUserId();
        return game.createCampaign(userId, request == null ? null : request.name(),
                request == null ? null : request.edition());
    }

    @GetMapping("/{id}")
    public Campaign get(@PathVariable Long id) {
        return game.state(id).getCampaign();
    }

    @PostMapping("/{id}/join")
    public Campaign join(@PathVariable Long id, @RequestBody(required = false) JoinRequest request) {
        Long userId = SecurityUtils.requireUserId();
        return game.joinCampaign(id, userId, request == null ? null : request.characterId());
    }

    @PostMapping("/{id}/start")
    public ActionResult start(@PathVariable Long id) {
        Long userId = SecurityUtils.requireUserId();
        return game.start(id, userId);
    }

    @GetMapping("/{id}/state")
    public GameState state(@PathVariable Long id) {
        return game.state(id);
    }

    @GetMapping("/{id}/intro")
    public java.util.Map<String, Object> intro(@PathVariable Long id) {
        return game.intro(id);
    }
}
