package com.dndmasters.ai.dm;

import com.dndmasters.engine.combat.Condition;
import com.dndmasters.engine.rules.Skills;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Sanitises DM actions before the engine sees them. Unknown action types are dropped,
 * Dcs are clamped, checks are mapped to known skills, conditions are validated and
 * combat cannot be started twice.
 */
@Component
public class DmActionValidator {

    private static final Set<String> KNOWN_TYPES = Set.of(
            "NARRATE", "REQUEST_CHECK", "START_COMBAT", "SPAWN_ENEMY", "SHIFT_SCENE", "GRANT_REWARD",
            "END_ADVENTURE", "OPEN_SHOP", "APPLY_CONDITION", "REMOVE_CONDITION");

    private static final int MIN_DC = 1;
    private static final int MAX_DC = 30;
    private static final int MAX_CONDITION_ROUNDS = 20;

    public DmResponse sanitize(DmResponse response, DmContext context) {
        if (response == null) {
            return DmResponse.narration("El Dungeon Master permanece en silencio...");
        }
        List<DmAction> clean = new ArrayList<>();
        for (DmAction action : response.actions() == null ? List.<DmAction>of() : response.actions()) {
            DmAction sanitized = sanitize(action, context);
            if (sanitized != null) {
                clean.add(sanitized);
            }
        }
        String narration = response.narration() == null || response.narration().isBlank()
                ? "El Dungeon Master observa en silencio."
                : response.narration();
        return new DmResponse(narration, clean, response.sceneChanged(), response.imagePrompt());
    }

    private DmAction sanitize(DmAction action, DmContext context) {
        if (action == null || action.type() == null || !KNOWN_TYPES.contains(action.type())) {
            return null;
        }
        return switch (action.type()) {
            case "REQUEST_CHECK" -> {
                String skill = Skills.isKnown(action.skill()) ? action.skill().toLowerCase() : "perception";
                int dc = action.dc() == null ? 12 : Math.max(MIN_DC, Math.min(MAX_DC, action.dc()));
                yield DmAction.requestCheck(skill, dc);
            }
            case "SHIFT_SCENE" -> {
                if (action.sceneOrder() == null || action.sceneOrder() < 0 || action.sceneOrder() > 5) {
                    yield null;
                }
                yield DmAction.shiftScene(action.sceneOrder());
            }
            case "START_COMBAT" -> context.isCombatActive() ? null : DmAction.startCombat();
            case "SPAWN_ENEMY" -> action.enemyName() == null ? null : action;
            case "GRANT_REWARD" -> action.itemName() == null ? null : action;
            case "APPLY_CONDITION" -> {
                Condition condition = Condition.fromCode(action.condition());
                if (action.entityId() == null || condition == null) {
                    yield null;
                }
                Integer rounds = action.rounds() == null
                        ? null
                        : Math.max(1, Math.min(MAX_CONDITION_ROUNDS, action.rounds()));
                yield DmAction.applyCondition(action.entityId(), condition.name(), rounds);
            }
            case "REMOVE_CONDITION" -> {
                Condition condition = Condition.fromCode(action.condition());
                if (action.entityId() == null || condition == null) {
                    yield null;
                }
                yield DmAction.removeCondition(action.entityId(), condition.name());
            }
            default -> action;
        };
    }
}
