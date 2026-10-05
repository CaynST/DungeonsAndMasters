package com.dndmasters.web;

import com.dndmasters.rules.RuleSetRegistry;
import com.dndmasters.web.dto.CharacterDtos;
import com.dndmasters.web.dto.CharacterDtos.RulesetView;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Serves the SRD catalogues (species, classes, backgrounds) per edition. */
@RestController
@RequestMapping("/api/rulesets")
public class RulesetController {

    private final RuleSetRegistry registry;

    public RulesetController(RuleSetRegistry registry) {
        this.registry = registry;
    }

    @GetMapping
    public List<RulesetView> all() {
        return registry.all().stream().map(edition -> CharacterDtos.toView(registry.get(edition))).toList();
    }

    @GetMapping("/{edition}")
    public RulesetView byEdition(@PathVariable String edition) {
        return CharacterDtos.toView(registry.get(edition));
    }
}
