package com.dndmasters.service;

import com.dndmasters.ai.DungeonMasterService;
import com.dndmasters.ai.dm.DmAction;
import com.dndmasters.ai.dm.DmContext;
import com.dndmasters.ai.dm.DmResponse;
import com.dndmasters.domain.Attack;
import com.dndmasters.domain.Campaign;
import com.dndmasters.domain.CampaignStatus;
import com.dndmasters.domain.CombatState;
import com.dndmasters.domain.EntityKind;
import com.dndmasters.domain.GameEntity;
import com.dndmasters.domain.GameEvent;
import com.dndmasters.domain.GameMap;
import com.dndmasters.domain.GamePhase;
import com.dndmasters.domain.GameState;
import com.dndmasters.domain.InventoryItem;
import com.dndmasters.domain.PlayerCharacter;
import com.dndmasters.engine.board.MoveResult;
import com.dndmasters.engine.board.MovementValidator;
import com.dndmasters.engine.combat.Condition;
import com.dndmasters.engine.combat.ConditionRules;
import com.dndmasters.engine.combat.ConditionSet;
import com.dndmasters.engine.combat.Conditions;
import com.dndmasters.engine.combat.DeathSaveRules;
import com.dndmasters.engine.dice.DiceEngine;
import com.dndmasters.engine.dice.DiceRollResult;
import com.dndmasters.engine.rules.CheckResolver;
import com.dndmasters.engine.rules.CheckResult;
import com.dndmasters.engine.rules.Advantage;
import com.dndmasters.engine.rules.RangeRules;
import com.dndmasters.engine.rules.Combatants;
import com.dndmasters.persistence.entity.CharacterEntity;
import com.dndmasters.persistence.entity.DiceRollEntity;
import com.dndmasters.persistence.mapper.CharacterMapper;
import com.dndmasters.persistence.mapper.GameEntityMapper;
import com.dndmasters.persistence.repository.CharacterRepository;
import com.dndmasters.persistence.repository.DiceRollRepository;
import com.dndmasters.service.action.ActionResult;
import com.dndmasters.service.action.CheckRequest;
import com.dndmasters.service.action.PlayerAction;
import com.dndmasters.util.Json;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Facade that turns player actions into validated state changes. It is the only place that
 * reads player intent, consults the engine, applies DM-proposed actions and records events.
 */
@Service
public class GameService {

    private final CampaignService campaigns;
    private final CombatService combat;
    private final DungeonMasterService dungeonMaster;
    private final GameEventService events;
    private final DiceEngine dice;
    private final CheckResolver checkResolver;
    private final MovementValidator movementValidator;
    private final GameEntityMapper entityMapper;
    private final CharacterRepository characters;
    private final CharacterMapper characterMapper;
    private final DiceRollRepository diceRolls;

    public GameService(CampaignService campaigns, CombatService combat, DungeonMasterService dungeonMaster,
                       GameEventService events, DiceEngine dice, CheckResolver checkResolver,
                       MovementValidator movementValidator, GameEntityMapper entityMapper,
                       CharacterRepository characters, CharacterMapper characterMapper, DiceRollRepository diceRolls) {
        this.campaigns = campaigns;
        this.combat = combat;
        this.dungeonMaster = dungeonMaster;
        this.events = events;
        this.dice = dice;
        this.checkResolver = checkResolver;
        this.movementValidator = movementValidator;
        this.entityMapper = entityMapper;
        this.characters = characters;
        this.characterMapper = characterMapper;
        this.diceRolls = diceRolls;
    }

    public List<Campaign> campaigns() {
        return campaigns.list();
    }

    public Campaign createCampaign(Long userId, String name) {
        return campaigns.create(userId, name, null);
    }

    public Campaign createCampaign(Long userId, String name, String edition) {
        return campaigns.create(userId, name, edition);
    }

    public Campaign joinCampaign(Long campaignId, Long userId, Long characterId) {
        return campaigns.join(campaignId, userId, characterId);
    }

    public void deleteCampaign(Long campaignId, Long userId) {
        campaigns.delete(campaignId, userId);
    }

    public GameState state(Long campaignId) {
        return campaigns.state(campaignId);
    }

    public String dmProviderId() {
        return dungeonMaster.providerId();
    }

    @Transactional
    public ActionResult start(Long campaignId, Long userId) {
        GameState state = campaigns.start(campaignId, userId);
        DmResponse dm = respond(campaignId, "START", "La compañía inicia la aventura.", null, null);
        DmApplication application = applyDm(campaignId, userId, dm);
        List<GameEvent> all = new ArrayList<>(application.events());
        return new ActionResult(true, null, dm.narration(), all, application.checkRequest(), null, null,
                application.actions(), application.adventureEnded());
    }

    @Transactional
    public ActionResult act(Long campaignId, Long userId, PlayerAction action) {
        if (action == null || action.getType() == null) {
            return ActionResult.error("Acción vacía");
        }
        return switch (action.getType()) {
            case PlayerAction.DICE -> rollDice(campaignId, userId, action);
            case PlayerAction.CHECK -> check(campaignId, userId, action);
            case PlayerAction.MOVE -> move(campaignId, userId, action);
            case PlayerAction.ATTACK -> attack(campaignId, userId, action);
            case PlayerAction.END_TURN -> endTurn(campaignId, userId, action);
            case PlayerAction.DIALOGUE -> dialogue(campaignId, userId, action);
            case PlayerAction.NARRATE -> narrate(campaignId, userId, action);
            case PlayerAction.USE_ITEM -> useItem(campaignId, userId, action);
            case PlayerAction.PASS -> pass(campaignId, userId, action);
            case PlayerAction.DEATH_SAVE -> deathSave(campaignId, userId, action);
            case PlayerAction.DASH -> combatAction(campaignId, userId, action, PlayerAction.DASH);
            case PlayerAction.DODGE -> combatAction(campaignId, userId, action, PlayerAction.DODGE);
            case PlayerAction.DISENGAGE -> combatAction(campaignId, userId, action, PlayerAction.DISENGAGE);
            case PlayerAction.STAND_UP -> standUp(campaignId, userId, action);
            default -> ActionResult.error("Tipo de acción desconocido: " + action.getType());
        };
    }

    private ActionResult rollDice(Long campaignId, Long userId, PlayerAction action) {
        String expression = action.getExpression() == null ? "1d20" : action.getExpression();
        DiceRollResult result;
        try {
            result = dice.roll(expression);
        } catch (RuntimeException e) {
            return ActionResult.error("Expresión de dados inválida: " + expression);
        }
        DiceRollEntity log = new DiceRollEntity();
        log.setCampaignId(campaignId);
        log.setUserId(userId);
        log.setExpression(result.expression());
        log.setRollsJson(Json.write(result.rolls()));
        log.setModifier(result.modifier());
        log.setTotal(result.total());
        log.setNatural20(result.natural20());
        log.setNatural1(result.natural1());
        log.setPurpose(action.getText());
        log.setCreatedAt(Instant.now().toString());
        diceRolls.save(log);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("expression", result.expression());
        data.put("rolls", result.rolls());
        data.put("modifier", result.modifier());
        data.put("total", result.total());
        data.put("natural20", result.natural20());
        data.put("natural1", result.natural1());
        GameEvent event = events.record(campaignId, null, "DICE_ROLL", null,
                "Tirada " + result.expression() + " = " + result.total(), data);
        return new ActionResult(true, null, null, List.of(event), null, data, null, List.of(), false);
    }

    private ActionResult check(Long campaignId, Long userId, PlayerAction action) {
        GameEntity actor = resolveActor(campaignId, userId, action);
        if (actor == null) {
            return ActionResult.error("No se encontró el personaje activo");
        }
        if (isDowned(actor)) {
            return ActionResult.error("Estás moribundo; solo puedes tirar una salvación de muerte.");
        }
        if (Conditions.of(actor).incapacitated()) {
            return ActionResult.error("Estás incapacitado y no puedes hacer pruebas.");
        }
        String skill = action.getSkill() == null ? "perception" : action.getSkill();
        int dc = action.getDc() == null ? 12 : action.getDc();
        ConditionSet conditions = Conditions.of(actor);
        Advantage advantage = ConditionRules.checkAdvantage(conditions, skill);
        int penalty = ConditionRules.checkPenalty(conditions);
        CheckResult checkResult = checkResolver.resolve(Combatants.of(actor), skill, dc, advantage, penalty);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("skill", skill);
        data.put("dc", dc);
        data.put("advantage", advantage.name());
        data.put("penalty", penalty);
        // Same shape as a dice roll so the frontend can animate it identically.
        data.put("expression", "1d20");
        data.put("rolls", List.of(checkResult.dieRoll()));
        data.put("dieRoll", checkResult.dieRoll());
        data.put("modifier", checkResult.modifier());
        data.put("total", checkResult.total());
        data.put("success", checkResult.success());
        data.put("natural20", checkResult.natural20());
        data.put("natural1", checkResult.natural1());
        GameEvent event = events.record(campaignId, activeSceneId(campaignId), "CHECK", actor.getName(),
                "Prueba de " + skill + ": " + checkResult.total() + " vs CD " + dc
                        + (checkResult.success() ? " → éxito" : " → fallo"), data);

        DmResponse dm = respond(campaignId, "CHECK_RESULT",
                "Prueba de " + skill + " contra CD " + dc + " → " + (checkResult.success() ? "éxito" : "fallo"),
                skill, checkResult.success());
        DmApplication application = applyDm(campaignId, userId, dm);
        List<GameEvent> all = new ArrayList<>();
        all.add(event);
        all.addAll(application.events());
        return new ActionResult(true, null, dm.narration(), all, application.checkRequest(), data, null,
                application.actions(), application.adventureEnded());
    }

    private ActionResult move(Long campaignId, Long userId, PlayerAction action) {
        GameEntity actor = resolveActor(campaignId, userId, action);
        if (actor == null) {
            return ActionResult.error("No se encontró el personaje activo");
        }
        if (!ownedBy(actor, userId)) {
            return ActionResult.error("No controlas ese personaje");
        }
        if (isDowned(actor)) {
            return ActionResult.error("Estás moribundo; solo puedes tirar una salvación de muerte.");
        }
        ConditionSet conditions = Conditions.of(actor);
        if (conditions.incapacitated()) {
            return ActionResult.error("Estás incapacitado y no puedes usar tu movimiento.");
        }
        if (conditions.speedZero()) {
            return ActionResult.error("No puedes usar tu movimiento: tu velocidad es 0 por una condición.");
        }
        if (action.getPath() == null || action.getPath().isEmpty()) {
            return ActionResult.error("Ruta de movimiento vacía");
        }
        CombatState state = campaigns.activeCombat(campaignId);
        if (state != null && state.isActive() && !state.isCombatantTurn(actor.getId())) {
            return ActionResult.error("No es tu turno");
        }
        GameMap map = campaigns.mapForScene(activeSceneId(campaignId));
        if (map == null) {
            return ActionResult.error("La escena no tiene tablero");
        }
        Set<com.dndmasters.domain.Cell> occupied = occupiedCells(campaignId, actor.getId());
        MoveResult result = movementValidator.validate(map, actor, action.getPath(), occupied);
        if (!result.ok()) {
            return ActionResult.error(result.reason());
        }
        // Cumulative movement this turn must respect the allowance (speed, doubled by Dash),
        // reduced by conditions such as exhaustion, grappling or crawling while prone.
        int used = com.dndmasters.engine.combat.TurnEconomy.movementUsed(actor.getData());
        int effectiveSpeed = ConditionRules.effectiveSpeed(actor.getSpeed(), conditions);
        int allowance = com.dndmasters.engine.combat.TurnEconomy
                .movementAllowance(actor.getData(), effectiveSpeed);
        int cap = effectiveSpeed
                * (com.dndmasters.engine.combat.TurnEconomy.flag(actor.getData(), "dashUsed") ? 2 : 1);
        allowance = Math.min(allowance, cap);
        if (used + result.cost() > allowance) {
            return ActionResult.error(
                    "No te queda movimiento: llevas " + used + " de " + allowance + " casillas este turno.");
        }
        com.dndmasters.domain.Cell previousCell = actor.cell();
        actor.moveTo(action.getPath().get(action.getPath().size() - 1));
        actor.getData().put("movementUsed", used + result.cost());
        campaigns.saveEntity(actor);
        List<GameEvent> recorded = new ArrayList<>();
        recorded.add(events.record(campaignId, activeSceneId(campaignId), "MOVE", actor.getName(),
                actor.getName() + " se mueve " + result.cost() + " casilla(s).",
                Map.of("entityId", actor.getId(), "x", actor.getX(), "y", actor.getY(), "cost", result.cost())));
        // Leaving an enemy's reach provokes an opportunity attack unless disengaged.
        CombatState combatState = state != null ? state : transientState(campaignId);
        recorded.addAll(combat.opportunityAttacks(campaignId, combatState, actor, previousCell));
        return new ActionResult(true, null, null, recorded, null, null, null, List.of(), false);
    }

    private ActionResult attack(Long campaignId, Long userId, PlayerAction action) {
        GameEntity actor = resolveActor(campaignId, userId, action);
        if (actor == null) {
            return ActionResult.error("No se encontró el personaje activo");
        }
        if (!ownedBy(actor, userId)) {
            return ActionResult.error("No controlas ese personaje");
        }
        if (isDowned(actor)) {
            return ActionResult.error("Estás moribundo; solo puedes tirar una salvación de muerte.");
        }
        if (Conditions.of(actor).incapacitated()) {
            return ActionResult.error("Estás incapacitado y no puedes atacar.");
        }
        if (action.getTargetEntityId() == null) {
            return ActionResult.error("Selecciona un objetivo");
        }
        GameEntity target = campaigns.entity(action.getTargetEntityId()).orElse(null);
        if (target == null || !target.isAlive()) {
            return ActionResult.error("Objetivo inválido");
        }
        CombatState state = campaigns.activeCombat(campaignId);
        if (state != null && state.isActive() && !state.isCombatantTurn(actor.getId())) {
            return ActionResult.error("No es tu turno");
        }
        if (Boolean.TRUE.equals(actor.getData().get("actionUsed"))) {
            return ActionResult.error("Ya has usado tu acción este turno. Termina el turno para volver a actuar.");
        }
        Attack chosen = chooseAttack(actor, action.getAttack());
        int distance = actor.cell().chebyshevTo(target.cell());
        RangeRules.Result range = RangeRules.check(chosen, distance);
        if (!range.allowed()) {
            return ActionResult.error(range.reason());
        }
        Advantage advantage = range.disadvantage() ? Advantage.DISADVANTAGE : Advantage.NONE;
        CombatState eventState = state != null ? state : transientState(campaignId);
        List<GameEvent> recorded = new ArrayList<>(combat.attack(campaignId, eventState, actor, target, chosen, advantage));
        actor.getData().put("actionUsed", true);
        campaigns.saveEntity(actor);

        Map<String, Object> attackData = recorded.stream()
                .filter(e -> "ATTACK".equals(e.type()) || "ATTACK_MISS".equals(e.type()))
                .findFirst().map(e -> Json.readMap(e.dataJson())).orElse(Map.of());

        String narration = null;
        if (state != null && state.isActive()) {
            CombatState reloaded = campaigns.activeCombat(campaignId);
            if (reloaded != null && combat.isFinished(reloaded)) {
                combat.finish(campaignId, reloaded);
                DmApplication aftermath = aftermath(campaignId, reloaded);
                recorded.addAll(aftermath.events());
                narration = aftermath.narration();
            }
        }
        return new ActionResult(true, null, narration, recorded, null, null, attackData, List.of(), false);
    }

    private ActionResult endTurn(Long campaignId, Long userId, PlayerAction action) {
        GameEntity actor = resolveActor(campaignId, userId, action);
        if (actor == null) {
            return ActionResult.error("No se encontró el personaje activo");
        }
        CombatState state = campaigns.activeCombat(campaignId);
        if (state == null || !state.isActive()) {
            return ActionResult.error("No hay combate activo");
        }
        if (!state.isCombatantTurn(actor.getId())) {
            return ActionResult.error("No es tu turno");
        }
        combat.advanceTurn(campaignId, state);
        List<GameEvent> recorded = new ArrayList<>(combat.runAutomaticTurns(campaignId, state));

        String narration = null;
        CheckRequest checkRequest = null;
        List<String> dmActions = new ArrayList<>();
        if (!state.isActive()) {
            DmApplication aftermath = aftermath(campaignId, state);
            recorded.addAll(aftermath.events());
            narration = aftermath.narration();
            checkRequest = aftermath.checkRequest();
            dmActions = aftermath.actions();
        }
        return new ActionResult(true, null, narration, recorded, checkRequest, null, null, dmActions, false);
    }

    private ActionResult dialogue(Long campaignId, Long userId, PlayerAction action) {
        String text = action.getText() == null ? "El jugador habla." : action.getText();
        GameEntity actor = resolveActor(campaignId, userId, action);
        if (actor != null && isDowned(actor)) {
            return ActionResult.error("Estás moribundo; no puedes hablar.");
        }
        String actorName = actor != null ? actor.getName() : "El grupo";
        // Record what the player said so it appears in the log and the chronicle.
        GameEvent spoken = events.record(campaignId, activeSceneId(campaignId), "PLAYER_ACTION", actorName, text, Map.of());
        DmResponse dm = respond(campaignId, "DIALOGUE", text, null, null);
        DmApplication application = applyDm(campaignId, userId, dm);
        List<GameEvent> all = new ArrayList<>();
        all.add(spoken);
        all.addAll(application.events());
        return new ActionResult(true, null, dm.narration(), all, application.checkRequest(), null, null,
                application.actions(), application.adventureEnded());
    }

    private ActionResult narrate(Long campaignId, Long userId, PlayerAction action) {
        DmResponse dm = respond(campaignId, "NARRATE", action.getText(), null, null);
        DmApplication application = applyDm(campaignId, userId, dm);
        return new ActionResult(true, null, dm.narration(), application.events(), application.checkRequest(), null,
                null, application.actions(), application.adventureEnded());
    }

    private ActionResult pass(Long campaignId, Long userId, PlayerAction action) {
        campaigns.rotateSpotlight(campaignId);
        // Fresh turn economy when the narrative spotlight moves on.
        Long sceneId = activeSceneId(campaignId);
        if (sceneId != null) {
            campaigns.entitiesInScene(sceneId).stream()
                    .filter(entity -> entity.getKind() == EntityKind.PLAYER)
                    .forEach(entity -> campaigns.resetTurnUsage(entity.getId()));
        }
        String name = campaigns.spotlightName(campaignId);
        DmResponse dm = respond(campaignId, "SPOTLIGHT", "El grupo cede el turno a " + name + ".", null, null);
        DmApplication application = applyDm(campaignId, userId, dm);
        return new ActionResult(true, null, dm.narration(), application.events(), application.checkRequest(),
                null, null, application.actions(), application.adventureEnded());
    }

    /** Cinematic intro data: title, premise lines and the party. */
    public java.util.Map<String, Object> intro(Long campaignId) {
        GameState state = campaigns.state(campaignId);
        java.util.Map<String, Object> out = new java.util.LinkedHashMap<>();
        out.put("title", state.getCampaign().getName());
        out.put("image", state.getCampaign().getImage());
        out.put("setting", state.getCampaign().getSetting());
        out.put("villain", state.getCampaign().getVillain());
        out.put("objective", state.getCampaign().getObjective());
        String premise = state.getCampaign().getPremise();
        out.put("premise", premise == null ? java.util.List.of() : java.util.List.of(premise.split("\n")));
        java.util.List<java.util.Map<String, Object>> party = new java.util.ArrayList<>();
        for (PlayerCharacter character : state.getParty()) {
            java.util.Map<String, Object> view = new java.util.LinkedHashMap<>();
            view.put("name", character.getName());
            view.put("race", character.getRace());
            view.put("cls", character.getCls());
            view.put("color", character.getColor());
            view.put("portrait", character.getPortrait());
            party.add(view);
        }
        out.put("party", party);
        out.put("spotlight", campaigns.spotlightName(campaignId));
        return out;
    }

    /** Standing up from prone costs half the creature's walking speed. */
    private ActionResult standUp(Long campaignId, Long userId, PlayerAction action) {
        GameEntity actor = resolveActor(campaignId, userId, action);
        if (actor == null) {
            return ActionResult.error("No se encontró el personaje activo");
        }
        if (!ownedBy(actor, userId)) {
            return ActionResult.error("No controlas ese personaje");
        }
        if (isDowned(actor)) {
            return ActionResult.error("Estás moribundo; solo puedes tirar una salvación de muerte.");
        }
        CombatState state = campaigns.activeCombat(campaignId);
        if (state != null && state.isActive() && !state.isCombatantTurn(actor.getId())) {
            return ActionResult.error("No es tu turno");
        }
        ConditionSet conditions = Conditions.of(actor);
        if (!conditions.has(Condition.PRONE)) {
            return ActionResult.error("No estás derribado.");
        }
        if (conditions.incapacitated()) {
            return ActionResult.error("Estás incapacitado y no puedes levantarte.");
        }
        if (conditions.speedZero()) {
            return ActionResult.error("No puedes levantarte: tu velocidad es 0.");
        }

        int walkingSpeed = ConditionRules.effectiveSpeed(actor.getSpeed(), conditions.without(Condition.PRONE));
        int cost = Math.max(1, (walkingSpeed + 1) / 2);
        int used = com.dndmasters.engine.combat.TurnEconomy.movementUsed(actor.getData());
        int allowance = com.dndmasters.engine.combat.TurnEconomy
                .movementAllowance(actor.getData(), walkingSpeed);
        boolean dashing = com.dndmasters.engine.combat.TurnEconomy.flag(actor.getData(), "dashUsed");
        allowance = Math.min(allowance, walkingSpeed * (dashing ? 2 : 1));
        if (used + cost > allowance) {
            return ActionResult.error("No te queda movimiento para levantarte: necesitas " + cost + " casillas.");
        }

        Conditions.remove(actor, Condition.PRONE);
        actor.getData().put("movementUsed", used + cost);
        actor.getData().put("movementMax", walkingSpeed * (dashing ? 2 : 1));
        campaigns.saveEntity(actor);
        GameEvent event = events.record(campaignId, activeSceneId(campaignId), "STAND_UP", actor.getName(),
                actor.getName() + " se levanta (coste " + cost + " casillas).",
                Map.of("entityId", actor.getId(), "cost", cost));
        return new ActionResult(true, null, null, List.of(event), null, null, null, List.of(), false);
    }

    /** Dash, Dodge and Disengage: standard combat actions that consume the action. */
    private ActionResult combatAction(Long campaignId, Long userId, PlayerAction action, String type) {
        GameEntity actor = resolveActor(campaignId, userId, action);
        if (actor == null) {
            return ActionResult.error("No se encontró el personaje activo");
        }
        if (!ownedBy(actor, userId)) {
            return ActionResult.error("No controlas ese personaje");
        }
        if (isDowned(actor)) {
            return ActionResult.error("Estás moribundo; solo puedes tirar una salvación de muerte.");
        }
        if (Conditions.of(actor).incapacitated()) {
            return ActionResult.error("Estás incapacitado y no puedes realizar esa acción.");
        }
        CombatState state = campaigns.activeCombat(campaignId);
        if (state != null && state.isActive() && !state.isCombatantTurn(actor.getId())) {
            return ActionResult.error("No es tu turno");
        }
        if (Boolean.TRUE.equals(actor.getData().get("actionUsed"))) {
            return ActionResult.error("Ya has usado tu acción este turno.");
        }

        String message;
        switch (type) {
            case PlayerAction.DASH -> {
                int effectiveSpeed = ConditionRules.effectiveSpeed(actor.getSpeed(), Conditions.of(actor));
                if (effectiveSpeed <= 0) {
                    return ActionResult.error("No puedes correr: tu velocidad es 0.");
                }
                int dashMax = effectiveSpeed * 2;
                actor.getData().put("movementMax", dashMax);
                actor.getData().put("dashUsed", true);
                message = actor.getName() + " corre: movimiento ampliado a " + dashMax + " casillas.";
            }
            case PlayerAction.DODGE -> {
                actor.getData().put("dodging", true);
                message = actor.getName() + " esquiva: los ataques contra él tienen desventaja.";
            }
            case PlayerAction.DISENGAGE -> {
                actor.getData().put("disengaged", true);
                message = actor.getName()
                        + " se retira con cuidado: su movimiento no provoca ataques de oportunidad.";
            }
            default -> {
                return ActionResult.error("Acción de combate desconocida");
            }
        }
        actor.getData().put("actionUsed", true);
        campaigns.saveEntity(actor);
        GameEvent event = events.record(campaignId, activeSceneId(campaignId), type, actor.getName(), message,
                Map.of("entityId", actor.getId()));
        return new ActionResult(true, null, null, List.of(event), null, null, null, List.of(), false);
    }

    private ActionResult deathSave(Long campaignId, Long userId, PlayerAction action) {        GameEntity actor = resolveActor(campaignId, userId, action);
        if (actor == null) {
            return ActionResult.error("No se encontró el personaje activo");
        }
        if (!ownedBy(actor, userId)) {
            return ActionResult.error("No controlas ese personaje");
        }
        DeathSaveRules.State current = combat.deathState(actor);
        if (!current.dying()) {
            return ActionResult.error(current.stable()
                    ? "Estás estable; no necesitas salvación."
                    : "No estás moribundo.");
        }
        CombatState state = campaigns.activeCombat(campaignId);
        if (state != null && state.isActive() && !state.isCombatantTurn(actor.getId())) {
            return ActionResult.error("No es tu turno");
        }

        int d20 = dice.roll("1d20").rolls().get(0);
        DeathSaveRules.State next = DeathSaveRules.roll(current, d20);
        combat.applyDeathState(actor, next);
        campaigns.saveEntity(actor);

        Long sceneId = activeSceneId(campaignId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("entityId", actor.getId());
        data.put("roll", d20);
        data.put("successes", next.successes());
        data.put("failures", next.failures());
        data.put("dead", next.dead());
        data.put("stable", next.stable());

        List<GameEvent> recorded = new ArrayList<>();
        recorded.add(events.record(campaignId, sceneId, "DEATH_SAVE", actor.getName(),
                "Salvación de muerte: " + d20 + " → " + next.successes() + " éxitos, "
                        + next.failures() + " fallos.", data));

        if (next.dead()) {
            if (state != null) {
                combat.markParticipantDead(state, actor.getId());
            }
            recorded.add(events.record(campaignId, sceneId, "DEATH", actor.getName(),
                    actor.getName() + " muere.", Map.of("entityId", actor.getId())));
            if (state != null && combat.isFinished(state)) {
                combat.finish(campaignId, state);
                recorded.addAll(aftermath(campaignId, state).events());
            }
        } else if (next.stable()) {
            recorded.add(events.record(campaignId, sceneId, "STABILIZED", actor.getName(),
                    actor.getName() + " se estabiliza.", Map.of("entityId", actor.getId())));
        } else if (next.hpAfter() > 0) {
            recorded.add(events.record(campaignId, sceneId, "REVIVED", actor.getName(),
                    actor.getName() + " recupera la consciencia con 1 PG.", Map.of("entityId", actor.getId())));
        }

        Map<String, Object> roll = new LinkedHashMap<>();
        roll.put("expression", "1d20");
        roll.put("rolls", List.of(d20));
        roll.put("modifier", 0);
        roll.put("total", d20);
        roll.put("natural20", d20 == 20);
        roll.put("natural1", d20 == 1);
        return new ActionResult(true, null, null, recorded, null, roll, null, List.of(), false);
    }

    private ActionResult useItem(Long campaignId, Long userId, PlayerAction action) {
        GameEntity actor = resolveActor(campaignId, userId, action);
        if (actor == null) {
            return ActionResult.error("No se encontró el personaje activo");
        }
        if (isDowned(actor)) {
            return ActionResult.error("Estás moribundo; solo puedes tirar una salvación de muerte.");
        }
        if (Conditions.of(actor).incapacitated()) {
            return ActionResult.error("Estás incapacitado y no puedes usar objetos.");
        }
        if (Boolean.TRUE.equals(actor.getData().get("actionUsed"))) {
            return ActionResult.error("Ya has usado tu acción este turno.");
        }
        if (action.getItem() != null && action.getItem().toLowerCase().contains("poción")) {
            DiceRollResult heal = dice.roll("2d4+2");
            int maxHp = actor.getMaxHp();
            int newHp = Math.min(maxHp, actor.getHp() + heal.total());
            actor.setHp(newHp);
            actor.setAlive(true);
            actor.getData().put("dying", false);
            actor.getData().put("stable", false);
            actor.getData().put("deathSuccesses", 0);
            actor.getData().put("deathFailures", 0);
            actor.getData().put("actionUsed", true);
            campaigns.saveEntity(actor);
            GameEvent event = events.record(campaignId, activeSceneId(campaignId), "HEAL", actor.getName(),
                    actor.getName() + " bebe una poción y recupera " + heal.total() + " HP.",
                    Map.of("entityId", actor.getId(), "heal", heal.total(), "hpAfter", newHp));
            return new ActionResult(true, null, null, List.of(event), null, null, null, List.of(), false);
        }
        return ActionResult.error("No puedes usar ese objeto");
    }

    private DmApplication aftermath(Long campaignId, CombatState state) {
        boolean victory = combat.isVictory(state);
        String trigger = victory ? "COMBAT_VICTORY" : "COMBAT_DEFEAT";
        DmResponse dm = respond(campaignId, trigger, victory ? "La compañía gana el combate." : "La compañía cae.", null, null);
        return applyDm(campaignId, null, dm);
    }

    private DmResponse respond(Long campaignId, String trigger, String lastAction, String skill, Boolean checkSuccess) {
        DmContext context = buildContext(campaignId, trigger, lastAction, skill, checkSuccess);
        return dungeonMaster.respond(context);
    }

    private DmContext buildContext(Long campaignId, String trigger, String lastAction, String skill, Boolean checkSuccess) {
        GameState state = campaigns.state(campaignId);
        DmContext context = new DmContext();
        context.setTrigger(trigger);
        context.setCampaignId(campaignId);
        context.setCampaignName(state.getCampaign().getName());
        context.setPhase(state.getCampaign().getPhase().name());
        context.setObjective(state.getObjective());
        if (state.getScene() != null) {
            context.setSceneOrder(state.getScene().orderIndex());
            context.setSceneTitle(state.getScene().title());
            context.setSceneKind(state.getScene().kind());
            context.setSceneDescription(state.getScene().description());
        }
        CombatState active = state.getCombat();
        context.setCombatActive(active != null && active.isActive());
        if (active != null) {
            context.setRound(active.getRound());
            if (active.currentParticipant() != null) {
                context.setCurrentTurn(active.currentParticipant().getName());
            }
        }
        if (!context.isCombatActive()) {
            context.setSpotlight(campaigns.spotlightName(campaignId));
        }
        for (GameEntity entity : state.getEntities()) {
            Map<String, Object> view = new LinkedHashMap<>();
            view.put("id", entity.getId());
            view.put("name", entity.getName());
            view.put("hp", entity.getHp());
            view.put("maxHp", entity.getMaxHp());
            view.put("ac", entity.getAc());
            view.put("conditions", Conditions.codes(entity));
            if (entity.getKind() == EntityKind.PLAYER) {
                view.put("controller", entity.getData().get("controller"));
                context.getCharacters().add(view);
            } else if (entity.getKind() == EntityKind.ENEMY && entity.isAlive()) {
                context.getEnemies().add(view);
            } else if (entity.getKind() == EntityKind.NPC) {
                context.getNpcs().add(view);
            }
        }
        int from = Math.max(0, state.getRecentEvents().size() - 8);
        for (GameEvent event : state.getRecentEvents().subList(from, state.getRecentEvents().size())) {
            context.getRecentEvents().add(event.message());
        }
        context.setLastAction(lastAction);
        context.setSkill(skill);
        context.setCheckSuccess(checkSuccess);
        return context;
    }

    /** Applies DM-proposed actions, engine-validated. Scene shifts run before combat starts. */
    private DmApplication applyDm(Long campaignId, Long userId, DmResponse dm) {
        List<GameEvent> eventsOut = new ArrayList<>();
        List<String> actionsOut = new ArrayList<>();
        CheckRequest checkRequest = null;
        boolean adventureEnded = false;
        Long sceneId = activeSceneId(campaignId);
        int sceneOrder = campaigns.sceneOrder(sceneId).intValue();

        // Persist the DM's narration so it appears in the event history and the chronicle.
        if (dm.narration() != null && !dm.narration().isBlank()) {
            eventsOut.add(events.record(campaignId, sceneId, "NARRATION", "Dungeon Master", dm.narration(), Map.of()));
        }

        List<DmAction> ordered = new ArrayList<>(dm.actions());
        ordered.sort(Comparator.comparingInt(a -> actionPriority(a.type())));

        for (DmAction action : ordered) {
            switch (action.type()) {
                case "SHIFT_SCENE" -> {
                    Long target = campaigns.sceneIdByOrder(campaignId, action.sceneOrder());
                    if (target != null && !target.equals(sceneId)) {
                        sceneId = target;
                        sceneOrder = action.sceneOrder();
                        campaigns.updateCampaignPhase(campaignId, phaseForScene(target, campaignId), target);
                        campaigns.setShopOpen(campaignId, false);
                        campaigns.ensurePartyInScene(campaignId, target);
                        eventsOut.add(events.record(campaignId, target, "SCENE_CHANGED", "Dungeon Master",
                                "La escena cambia.", Map.of("sceneOrder", sceneOrder)));
                        actionsOut.add("SHIFT_SCENE:" + sceneOrder);
                    }
                }
                case "START_COMBAT" -> {
                    try {
                        CombatState started = combat.startCombat(campaignId, sceneId);
                        campaigns.updateCampaignPhase(campaignId, phaseForScene(sceneId, campaignId), sceneId);
                        actionsOut.add("START_COMBAT");
                        // If the first turn belongs to an enemy or ally, resolve until a player's turn.
                        eventsOut.addAll(combat.runAutomaticTurns(campaignId, started));
                    } catch (RuntimeException e) {
                        actionsOut.add("START_COMBAT_FAILED");
                    }
                }
                case "REQUEST_CHECK" -> {
                    checkRequest = new CheckRequest(action.skill(), action.dc(), null,
                            "Prueba de " + action.skill() + " (CD " + action.dc() + ")");
                    eventsOut.add(events.record(campaignId, sceneId, "CHECK_REQUESTED", "Dungeon Master",
                            "El Dungeon Master pide una prueba de " + action.skill() + " (CD " + action.dc() + ").",
                            Map.of("skill", action.skill(), "dc", action.dc())));
                    actionsOut.add("REQUEST_CHECK:" + action.skill() + ":" + action.dc());
                }
                case "GRANT_REWARD" -> {
                    grantReward(campaignId, userId, action.itemName());
                    eventsOut.add(events.record(campaignId, sceneId, "REWARD", "Dungeon Master",
                            "Recompensa obtenida: " + action.itemName() + ".", Map.of("item", action.itemName())));
                    actionsOut.add("GRANT_REWARD:" + action.itemName());
                }
                case "END_ADVENTURE" -> {
                    campaigns.updateCampaignStatus(campaignId, CampaignStatus.FINISHED);
                    campaigns.updateCampaignPhase(campaignId, GamePhase.COMPLETE, sceneId);
                    eventsOut.add(events.record(campaignId, sceneId, "ADVENTURE_ENDED", "Dungeon Master",
                            "La aventura ha concluido.", Map.of()));
                    actionsOut.add("END_ADVENTURE");
                    adventureEnded = true;
                }
                case "OPEN_SHOP" -> {
                    campaigns.setShopOpen(campaignId, true);
                    eventsOut.add(events.record(campaignId, sceneId, "SHOP_OPENED", "Dungeon Master",
                            "Un mercader despliega su mercancía ante vosotros.", Map.of()));
                    actionsOut.add("OPEN_SHOP");
                }
                case "APPLY_CONDITION" -> {
                    applyCondition(campaignId, sceneId, action, eventsOut);
                    actionsOut.add("APPLY_CONDITION:" + action.condition());
                }
                case "REMOVE_CONDITION" -> {
                    removeCondition(campaignId, sceneId, action, eventsOut);
                    actionsOut.add("REMOVE_CONDITION:" + action.condition());
                }
                default -> {
                    // NARRATE / SPAWN_ENEMY are non-mechanical for this prototype.
                }
            }
        }
        return new DmApplication(eventsOut, checkRequest, actionsOut, adventureEnded, dm.narration());
    }

    /** Test/internal seam: applies structured DM actions without narration. */
    @Transactional
    List<GameEvent> applyDmActions(Long campaignId, Long userId, List<DmAction> actions) {
        DmResponse response = new DmResponse("", actions == null ? List.of() : actions, false, null);
        return applyDm(campaignId, userId, response).events();
    }

    private void applyCondition(Long campaignId, Long sceneId, DmAction action, List<GameEvent> eventsOut) {
        if (action.entityId() == null) {
            return;
        }
        GameEntity target = campaigns.entity(action.entityId()).orElse(null);
        if (target == null || !target.isAlive()) {
            return;
        }
        // Conditions only apply to the scene the players are currently in.
        if (sceneId != null && !sceneId.equals(target.getSceneId())) {
            return;
        }
        Condition condition = Condition.fromCode(action.condition());
        if (condition == null) {
            return;
        }
        Conditions.add(target, condition, null, null, null, action.rounds());
        campaigns.saveEntity(target);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("entityId", target.getId());
        data.put("condition", condition.name());
        data.put("rounds", action.rounds() == null ? 0 : action.rounds());
        eventsOut.add(events.record(campaignId, sceneId, "CONDITION_APPLIED", target.getName(),
                target.getName() + " sufre " + condition.label().toLowerCase() + ".", data));
    }

    private void removeCondition(Long campaignId, Long sceneId, DmAction action, List<GameEvent> eventsOut) {
        if (action.entityId() == null) {
            return;
        }
        GameEntity target = campaigns.entity(action.entityId()).orElse(null);
        if (target == null) {
            return;
        }
        Condition condition = Condition.fromCode(action.condition());
        if (condition == null || !Conditions.remove(target, condition)) {
            return;
        }
        campaigns.saveEntity(target);
        eventsOut.add(events.record(campaignId, sceneId, "CONDITION_REMOVED", target.getName(),
                target.getName() + " se recupera de " + condition.label().toLowerCase() + ".",
                Map.of("entityId", target.getId(), "condition", condition.name())));
    }

    private void grantReward(Long campaignId, Long userId, String itemName) {        if (userId == null || itemName == null) {
            return;
        }
        Long characterId = campaigns.characterIdForUser(campaignId, userId);
        if (characterId == null) {
            return;
        }
        characters.findById(characterId).ifPresent(entity -> {
            PlayerCharacter character = characterMapper.toDomain(entity);
            List<InventoryItem> inventory = new ArrayList<>(character.getInventory());
            boolean present = inventory.stream().anyMatch(i -> i.name().equalsIgnoreCase(itemName));
            if (!present) {
                inventory.add(new InventoryItem(itemName, "Reliquia recuperada en la Cripta de Valdros.", "gem", 1));
                character.setInventory(inventory);
                CharacterEntity updated = characterMapper.toEntity(character);
                updated.setOwnerId(character.getOwnerId());
                updated.setCreatedAt(entity.getCreatedAt());
                characters.save(updated);
            }
        });
    }

    private int actionPriority(String type) {
        return switch (type) {
            case "SHIFT_SCENE" -> 0;
            case "START_COMBAT" -> 1;
            default -> 2;
        };
    }

    private GamePhase phaseForScene(Long sceneId, Long campaignId) {
        int order = campaigns.sceneOrder(sceneId).intValue();
        return switch (order) {
            case 1 -> GamePhase.COMBAT;
            case 2 -> GamePhase.BOSS;
            default -> GamePhase.EXPLORATION;
        };
    }

    private GameEntity resolveActor(Long campaignId, Long userId, PlayerAction action) {
        if (action.getActorEntityId() != null) {
            return campaigns.entity(action.getActorEntityId()).orElse(null);
        }
        Long characterId = campaigns.characterIdForUser(campaignId, userId);
        if (characterId == null) {
            return null;
        }
        GameState state = campaigns.state(campaignId);
        return state.getEntities().stream()
                .filter(e -> e.getKind() == EntityKind.PLAYER && characterId.equals(e.getRefId()))
                .findFirst().orElse(null);
    }

    private boolean ownedBy(GameEntity entity, Long userId) {
        Object owner = entity.getData().get("ownerId");
        if (owner instanceof Number n) {
            return userId != null && userId.longValue() == n.longValue();
        }
        return true;
    }

    /** True while the creature is dying or stabilised at 0 HP (cannot take normal actions). */
    private boolean isDowned(GameEntity entity) {
        return Boolean.TRUE.equals(entity.getData().get("dying"))
                || Boolean.TRUE.equals(entity.getData().get("stable"));
    }

    private Set<com.dndmasters.domain.Cell> occupiedCells(Long campaignId, long exceptEntityId) {
        Set<com.dndmasters.domain.Cell> occupied = new HashSet<>();
        GameState state = campaigns.state(campaignId);
        for (GameEntity e : state.getEntities()) {
            if (e.isAlive() && e.getId() != exceptEntityId
                    && (e.getKind() == EntityKind.PLAYER || e.getKind() == EntityKind.ENEMY)) {
                occupied.add(e.cell());
            }
        }
        return occupied;
    }

    private Attack chooseAttack(GameEntity actor, String attackName) {
        List<Attack> attacks = entityMapper.attacksOf(actor);
        if (attacks.isEmpty()) {
            return Attack.melee("Golpe", 2, "1d4");
        }
        if (attackName == null) {
            return attacks.get(0);
        }
        return attacks.stream().filter(a -> a.name().equalsIgnoreCase(attackName)).findFirst().orElse(attacks.get(0));
    }

    private CombatState transientState(Long campaignId) {
        CombatState state = new CombatState();
        state.setCampaignId(campaignId);
        state.setSceneId(activeSceneId(campaignId));
        state.setActive(false);
        state.setRound(0);
        state.setParticipants(new ArrayList<>());
        return state;
    }

    private Long activeSceneId(Long campaignId) {
        GameState state = campaigns.state(campaignId);
        return state.getScene() == null ? null : state.getScene().id();
    }

    private record DmApplication(List<GameEvent> events, CheckRequest checkRequest, List<String> actions,
                                 boolean adventureEnded, String narration) {
    }
}
