package com.dndmasters.rules;

import java.util.EnumMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Provides the SRD data for each supported edition. */
@Component
public class RuleSetRegistry {

    private final Map<Edition, RuleSet> ruleSets = new EnumMap<>(Edition.class);

    public RuleSetRegistry() {
        ruleSets.put(Edition.DND_5E_2014, RuleSet2014.build());
        ruleSets.put(Edition.DND_5E_2024, RuleSet2024.build());
    }

    public RuleSet get(Edition edition) {
        RuleSet set = ruleSets.get(edition);
        if (set == null) {
            throw new IllegalArgumentException("Unsupported edition: " + edition);
        }
        return set;
    }

    public RuleSet get(String editionId) {
        return get(Edition.fromId(editionId));
    }

    public java.util.List<Edition> all() {
        return java.util.List.copyOf(ruleSets.keySet());
    }
}
