package com.dndmasters.service;

import com.dndmasters.ai.DungeonMasterService;
import com.dndmasters.ai.dm.DmAction;
import com.dndmasters.ai.dm.DmContext;
import com.dndmasters.ai.dm.DmResponse;
import com.dndmasters.content.Bestiary;
import com.dndmasters.domain.Attack;
import com.dndmasters.domain.Ability;
import com.dndmasters.domain.Campaign;
import com.dndmasters.domain.CampaignPage;
import com.dndmasters.domain.CampaignStatus;
import com.dndmasters.domain.CombatParticipant;
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
import com.dndmasters.engine.combat.SurpriseRules;
import com.dndmasters.engine.combat.TurnEconomy;
import com.dndmasters.engine.dice.DiceEngine;
import com.dndmasters.engine.dice.DiceRollResult;
import com.dndmasters.engine.rules.CheckResolver;
import com.dndmasters.engine.rules.CheckResult;
import com.dndmasters.engine.rules.Advantage;
import com.dndmasters.engine.rules.AmmunitionRules;
import com.dndmasters.engine.rules.BardicInspirationRules;
import com.dndmasters.engine.rules.ClassFeatures;
import com.dndmasters.engine.rules.Combatant;
import com.dndmasters.engine.rules.CoverRules;
import com.dndmasters.engine.rules.Modifier;
import com.dndmasters.engine.rules.RageRules;
import com.dndmasters.engine.rules.RangeRules;
import com.dndmasters.engine.rules.RestRules;
import com.dndmasters.engine.rules.SaveResolver;
import com.dndmasters.engine.rules.SaveResult;
import com.dndmasters.engine.rules.SecondWindRules;
import com.dndmasters.engine.spells.Spell;
import com.dndmasters.engine.spells.SpellCatalog;
import com.dndmasters.engine.spells.SpellComponentRules;
import com.dndmasters.engine.spells.SpellTargeting;
import com.dndmasters.engine.spells.Spellbook;
import com.dndmasters.engine.spells.Spellcasting;
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
import java.util.Locale;
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
    private final SaveResolver saveResolver;
    private final MovementValidator movementValidator;
    private final GameEntityMapper entityMapper;
    private final CharacterRepository characters;
    private final CharacterMapper characterMapper;
    private final DiceRollRepository diceRolls;

    public GameService(CampaignService campaigns, CombatService combat, DungeonMasterService dungeonMaster,
                       GameEventService events, DiceEngine dice, CheckResolver checkResolver,
                       SaveResolver saveResolver, MovementValidator movementValidator, GameEntityMapper entityMapper,
                       CharacterRepository characters, CharacterMapper characterMapper, DiceRollRepository diceRolls) {
        this.campaigns = campaigns;
        this.combat = combat;
        this.dungeonMaster = dungeonMaster;
        this.events = events;
        this.dice = dice;
        this.checkResolver = checkResolver;
        this.saveResolver = saveResolver;
        this.movementValidator = movementValidator;
        this.entityMapper = entityMapper;
        this.characters = characters;
        this.characterMapper = characterMapper;
        this.diceRolls = diceRolls;
    }

    public List<Campaign> campaigns() {
        return campaigns.list();
    }

    public CampaignPage campaignsPage(int page, int size, String status) {
        return campaigns.listPage(page, size, status);
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
            case PlayerAction.SECOND_WIND -> classFeature(campaignId, userId, action, PlayerAction.SECOND_WIND);
            case PlayerAction.RAGE -> classFeature(campaignId, userId, action, PlayerAction.RAGE);
            case PlayerAction.CAST_SPELL -> castSpell(campaignId, userId, action);
            case PlayerAction.BARDIC_INSPIRATION ->
                    classFeature(campaignId, userId, action, PlayerAction.BARDIC_INSPIRATION);
            case PlayerAction.USE_INSPIRATION -> useInspiration(campaignId, userId, action);
            case PlayerAction.HELP -> help(campaignId, userId, action);
            case PlayerAction.HIDE -> hide(campaignId, userId, action);
            case PlayerAction.READY -> ready(campaignId, userId, action);
            case PlayerAction.SEARCH -> search(campaignId, userId, action);
            case PlayerAction.USE_OBJECT -> useObject(campaignId, userId, action);
            case PlayerAction.INTERACT -> interact(campaignId, userId, action);
            case PlayerAction.SHORT_REST -> rest(campaignId, userId, action, false);
            case PlayerAction.LONG_REST -> rest(campaignId, userId, action, true);
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
        int inspirationBonus = consumeInspiration(actor);
        CheckResult checkResult = checkResolver.resolve(Combatants.of(actor), skill, dc, advantage,
                penalty + inspirationBonus);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("skill", skill);
        data.put("dc", dc);
        data.put("advantage", advantage.name());
        data.put("penalty", penalty);
        data.put("inspirationBonus", inspirationBonus);
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
        GameMap attackMap = campaigns.mapForScene(activeSceneId(campaignId));
        CoverRules.Result cover = CoverRules.check(attackMap, actor.cell(), target.cell(), chosen.ranged());
        if (cover.blocksAttack()) {
            return ActionResult.error(cover.reason());
        }
        Advantage advantage = range.disadvantage() ? Advantage.DISADVANTAGE : Advantage.NONE;
        ActionResult ammunitionError = consumeAmmunition(actor, chosen);
        if (ammunitionError != null) {
            return ammunitionError;
        }
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

    /** Casts a cantrip or a level-1 spell; slots, action economy and targeting are server-side. */
    private ActionResult castSpell(Long campaignId, Long userId, PlayerAction action) {
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
            return ActionResult.error("Estás incapacitado y no puedes lanzar conjuros.");
        }
        CombatState state = campaigns.activeCombat(campaignId);
        if (state != null && state.isActive() && !state.isCombatantTurn(actor.getId())) {
            return ActionResult.error("No es tu turno");
        }

        String classId = ClassFeatures.idOf(actor);
        Spellbook book = SpellCatalog.forClass(classId);
        if (book.spells().isEmpty()) {
            return ActionResult.error("Tu clase no lanza conjuros.");
        }
        Spell spell = SpellCatalog.byId(classId, action.getSpell()).orElse(null);
        if (spell == null) {
            return ActionResult.error("No conoces ese conjuro.");
        }
        if (!SpellComponentRules.canCast(spell, hasSpellFocus(actor))) {
            return ActionResult.error("Necesitas un foco o una bolsa de componentes para lanzar "
                    + spell.name() + ".");
        }

        boolean bonusAction = spell.bonusAction();
        if (bonusAction && Boolean.TRUE.equals(actor.getData().get("bonusUsed"))) {
            return ActionResult.error("Ya has usado tu acción adicional este turno.");
        }
        if (!bonusAction && Boolean.TRUE.equals(actor.getData().get("actionUsed"))) {
            return ActionResult.error("Ya has usado tu acción este turno.");
        }
        int usedSlots = actor.getData().get("spellSlotsUsed") instanceof Number number
                ? number.intValue() : 0;
        int maxSlots = Spellcasting.maxSlots(ClassFeatures.level(actor));
        if (spell.level() > 0 && usedSlots >= maxSlots) {
            return ActionResult.error("No te quedan espacios de conjuro.");
        }

        GameEntity target = action.getTargetEntityId() == null
                ? null
                : campaigns.entity(action.getTargetEntityId()).orElse(null);
        if (spell.kind() == Spell.Kind.AREA) {
            // Self-centred area: no target selection is required.
        } else if (spell.kind() == Spell.Kind.HEAL || spell.kind() == Spell.Kind.BUFF) {
            if (target == null || target.getKind() != EntityKind.PLAYER) {
                target = actor;
            }
            if (!target.isAlive() && !isDowned(target)) {
                return ActionResult.error("No puedes curar a un objetivo muerto.");
            }
            int distance = actor.cell().chebyshevTo(target.cell());
            if (distance > spell.range()) {
                return ActionResult.error("El objetivo está fuera del alcance del conjuro ("
                        + distance + " > " + spell.range() + ").");
            }
        } else {
            if (target == null || !target.isAlive()) {
                return ActionResult.error("Selecciona un objetivo válido.");
            }
            if (target.getKind() != EntityKind.ENEMY) {
                return ActionResult.error("Selecciona un enemigo para ese conjuro.");
            }
            int distance = actor.cell().chebyshevTo(target.cell());
            if (distance > spell.range()) {
                return ActionResult.error("El objetivo está fuera del alcance del conjuro ("
                        + distance + " > " + spell.range() + ").");
            }
            if (spell.kind() == Spell.Kind.ATTACK) {
                GameMap map = campaigns.mapForScene(activeSceneId(campaignId));
                CoverRules.Result cover = CoverRules.check(map, actor.cell(), target.cell(), true);
                if (cover.blocksAttack()) {
                    return ActionResult.error(cover.reason());
                }
            }
        }

        if (spell.level() > 0) {
            actor.getData().put("spellSlotsUsed", usedSlots + 1);
        }
        actor.getData().put(bonusAction ? "bonusUsed" : "actionUsed", true);
        campaigns.saveEntity(actor);

        List<GameEvent> recorded = new ArrayList<>();
        Map<String, Object> castData = new LinkedHashMap<>();
        castData.put("entityId", actor.getId());
        castData.put("spell", spell.id());
        castData.put("spellName", spell.name());
        castData.put("level", spell.level());
        castData.put("slotsUsed", spell.level() > 0 ? usedSlots + 1 : usedSlots);
        recorded.add(events.record(campaignId, activeSceneId(campaignId), "SPELL_CAST", actor.getName(),
                actor.getName() + " lanza " + spell.name() + ".", castData));

        CombatState eventState = state != null ? state : transientState(campaignId);
        switch (spell.kind()) {
            case ATTACK -> {
                Attack spellAttack = Attack.ranged(spell.name(), Spellcasting.attackBonus(actor, classId),
                        spell.damage(), spell.range());
                recorded.addAll(combat.attack(campaignId, eventState, actor, target, spellAttack,
                        Advantage.NONE, false));
            }
            case AUTO -> recorded.addAll(combat.spellDamage(campaignId, eventState, actor, target, spell, false));
            case SAVE -> {
                Ability ability = Ability.valueOf(spell.saveAbility());
                int dc = Spellcasting.saveDc(actor, classId);
                boolean saved = resolveTargetSave(target, ability, dc, castData);
                if (saved) {
                    recorded.add(events.record(campaignId, activeSceneId(campaignId), "SPELL_SAVED", target.getName(),
                            target.getName() + " supera la salvación de " + ability.name()
                                    + " contra " + spell.name() + ".", castData));
                } else {
                    recorded.addAll(combat.spellDamage(campaignId, eventState, actor, target, spell, false));
                }
            }
            case HEAL -> {
                int healRoll = dice.roll(spell.heal()).total();
                int abilityMod = Modifier.of(actor.getStats().get(Spellcasting.abilityFor(classId)));
                int heal = Math.max(0, healRoll + abilityMod);
                int newHp = Math.min(target.getMaxHp(), target.getHp() + heal);
                target.setHp(newHp);
                target.setAlive(true);
                target.getData().put("dying", false);
                target.getData().put("stable", false);
                target.getData().put("deathSuccesses", 0);
                target.getData().put("deathFailures", 0);
                campaigns.saveEntity(target);
                Map<String, Object> healData = new LinkedHashMap<>(castData);
                healData.put("targetId", target.getId());
                healData.put("heal", heal);
                healData.put("hpAfter", newHp);
                recorded.add(events.record(campaignId, activeSceneId(campaignId), "SPELL_HEAL", actor.getName(),
                        actor.getName() + " cura " + heal + " PG a " + target.getName()
                                + " con " + spell.name() + ".", healData));
            }
            case BUFF -> {
                campaigns.clearConcentration(campaignId, actor.getId());
                target.getData().put("blessed", true);
                target.getData().put("blessedBy", actor.getId());
                actor.getData().put("concentrationSpell", spell.id());
                actor.getData().put("concentrationName", spell.name());
                actor.getData().put("concentrationRounds", 10);
                campaigns.saveEntity(target);
                campaigns.saveEntity(actor);
                Map<String, Object> buffData = new LinkedHashMap<>(castData);
                buffData.put("targetId", target.getId());
                buffData.put("buff", spell.buff());
                buffData.put("rounds", 10);
                recorded.add(events.record(campaignId, activeSceneId(campaignId), "SPELL_BUFF", actor.getName(),
                        actor.getName() + " bendice a " + target.getName()
                                + " con " + spell.name() + ".", buffData));
            }
            case AREA -> {
                Ability ability = Ability.valueOf(spell.saveAbility());
                int dc = Spellcasting.saveDc(actor, classId);
                List<GameEntity> targets = SpellTargeting.creaturesInCube(actor,
                        campaigns.entitiesInScene(activeSceneId(campaignId)), spell.range());
                for (GameEntity areaTarget : targets) {
                    Map<String, Object> saveData = new LinkedHashMap<>(castData);
                    saveData.put("targetId", areaTarget.getId());
                    saveData.put("targetName", areaTarget.getName());
                    boolean saved = resolveTargetSave(areaTarget, ability, dc, saveData);
                    if (saved) {
                        recorded.add(events.record(campaignId, activeSceneId(campaignId), "SPELL_SAVED",
                                areaTarget.getName(), areaTarget.getName() + " supera la salvación de "
                                        + ability.name() + " contra " + spell.name() + ".", saveData));
                    } else {
                        recorded.addAll(combat.spellDamage(campaignId, eventState, actor, areaTarget, spell, false));
                    }
                }
            }
        }

        Map<String, Object> attackData = recorded.stream()
                .filter(event -> "ATTACK".equals(event.type()) || "ATTACK_MISS".equals(event.type())
                        || "SPELL_DAMAGE".equals(event.type()))
                .findFirst().map(event -> Json.readMap(event.dataJson())).orElse(Map.of());
        return new ActionResult(true, null, null, recorded, null, null,
                attackData.isEmpty() ? null : attackData, List.of(), false);
    }

    /** Material components need a focus/pouch; existing tokens may predate the data flag. */
    private boolean hasSpellFocus(GameEntity actor) {
        Object stored = actor.getData().get("spellFocus");
        if (stored instanceof Boolean value) {
            return value;
        }
        if (actor.getRefId() == null) {
            return false;
        }
        return characters.findById(actor.getRefId())
                .map(characterMapper::toDomain)
                .map(SpellComponentRules::hasFocus)
                .orElse(false);
    }

    /** Resolves a target's save with its conditions and Bless bonus. */
    private boolean resolveTargetSave(GameEntity target, Ability ability, int dc, Map<String, Object> data) {
        ConditionSet conditions = Conditions.of(target);
        data.put("saveAbility", ability.name());
        data.put("saveDc", dc);
        if (ConditionRules.autoFailsSave(conditions, ability)) {
            data.put("saveAutoFail", true);
            data.put("saveSuccess", false);
            return false;
        }
        int blessBonus = Boolean.TRUE.equals(target.getData().get("blessed"))
                ? dice.roll("1d4").total() : 0;
        int inspirationBonus = consumeInspiration(target);
        SaveResult save = saveResolver.resolve(Combatants.of(target), ability, dc,
                ConditionRules.saveAdvantage(conditions, ability), blessBonus + inspirationBonus);
        data.put("blessBonus", blessBonus);
        data.put("inspirationBonus", inspirationBonus);
        data.put("saveDieRoll", save.dieRoll());
        data.put("saveModifier", save.modifier());
        data.put("saveTotal", save.total());
        data.put("saveSuccess", save.success());
        return save.success();
    }

    /** Uses a prepared Bardic Inspiration die on the next roll (no action cost). */
    private ActionResult useInspiration(Long campaignId, Long userId, PlayerAction action) {
        GameEntity actor = resolveActor(campaignId, userId, action);
        if (actor == null) {
            return ActionResult.error("No se encontró el personaje activo");
        }
        if (!ownedBy(actor, userId)) {
            return ActionResult.error("No controlas ese personaje");
        }
        if (isDowned(actor)) {
            return ActionResult.error("Estás moribundo; no puedes usar inspiración.");
        }
        CombatState state = campaigns.activeCombat(campaignId);
        if (state != null && state.isActive() && !state.isCombatantTurn(actor.getId())) {
            return ActionResult.error("No es tu turno");
        }
        int die = actor.getData().get("inspirationDie") instanceof Number number
                ? number.intValue() : 0;
        if (die <= 0) {
            return ActionResult.error("No tienes inspiración bárdica.");
        }
        if (Boolean.TRUE.equals(actor.getData().get("inspirationPending"))) {
            return ActionResult.error("Tu inspiración ya está preparada para la próxima tirada.");
        }
        actor.getData().put("inspirationPending", true);
        campaigns.saveEntity(actor);
        GameEvent event = events.record(campaignId, activeSceneId(campaignId), "INSPIRATION_READY",
                actor.getName(), actor.getName() + " prepara su inspiración bárdica (d" + die + ").",
                Map.of("entityId", actor.getId(), "die", die));
        return new ActionResult(true, null, null, List.of(event), null, null, null, List.of(), false);
    }

    /** Rolls and clears a prepared inspiration die; returns 0 when none is pending. */
    private int consumeInspiration(GameEntity entity) {
        if (!Boolean.TRUE.equals(entity.getData().get("inspirationPending"))) {
            return 0;
        }
        int die = entity.getData().get("inspirationDie") instanceof Number number
                ? number.intValue() : 0;
        if (die <= 0) {
            return 0;
        }
        entity.getData().put("inspirationPending", false);
        entity.getData().put("inspirationDie", 0);
        campaigns.saveEntity(entity);
        return dice.roll("1d" + die).total();
    }

    /** Class features that consume the bonus action: Second Wind, Rage and Bardic Inspiration. */
    private ActionResult classFeature(Long campaignId, Long userId, PlayerAction action, String type) {
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
            return ActionResult.error("Estás incapacitado y no puedes usar rasgos de clase.");
        }
        CombatState state = campaigns.activeCombat(campaignId);
        if (state != null && state.isActive() && !state.isCombatantTurn(actor.getId())) {
            return ActionResult.error("No es tu turno");
        }
        if (Boolean.TRUE.equals(actor.getData().get("bonusUsed"))) {
            return ActionResult.error("Ya has usado tu acción adicional este turno.");
        }

        Map<String, Object> data = new LinkedHashMap<>();
        String message;
        switch (type) {
            case PlayerAction.SECOND_WIND -> {
                if (!ClassFeatures.is(actor, "fighter")) {
                    return ActionResult.error("Solo los guerreros pueden usar Segundo aliento.");
                }
                if (Boolean.TRUE.equals(actor.getData().get("secondWindUsed"))) {
                    return ActionResult.error("Ya has usado Segundo aliento; necesitas un descanso.");
                }
                int roll = dice.roll("1d10").rolls().get(0);
                int heal = SecondWindRules.healAmount(roll, ClassFeatures.level(actor));
                int newHp = Math.min(actor.getMaxHp(), actor.getHp() + heal);
                actor.setHp(newHp);
                actor.setAlive(true);
                actor.getData().put("dying", false);
                actor.getData().put("stable", false);
                actor.getData().put("deathSuccesses", 0);
                actor.getData().put("deathFailures", 0);
                actor.getData().put("secondWindUsed", true);
                message = actor.getName() + " recupera " + heal + " PG con Segundo aliento ("
                        + newHp + "/" + actor.getMaxHp() + ").";
                data.put("heal", heal);
                data.put("hpAfter", newHp);
            }
            case PlayerAction.RAGE -> {
                if (!ClassFeatures.is(actor, "barbarian")) {
                    return ActionResult.error("Solo los bárbaros pueden enfurecerse.");
                }
                if (RageRules.isRaging(actor)) {
                    return ActionResult.error("Ya estás enfurecido.");
                }
                if (RageRules.ragesUsed(actor) >= RageRules.MAX_RAGES) {
                    return ActionResult.error("No te quedan usos de Furia; necesitas un descanso.");
                }
                actor.getData().put("raging", true);
                actor.getData().put("rageRounds", RageRules.DURATION_ROUNDS);
                actor.getData().put("ragesUsed", RageRules.ragesUsed(actor) + 1);
                message = actor.getName()
                        + " entra en Furia: +2 al daño cuerpo a cuerpo y resistencia al daño físico.";
                data.put("ragesUsed", actor.getData().get("ragesUsed"));
                data.put("rounds", RageRules.DURATION_ROUNDS);
            }
            case PlayerAction.BARDIC_INSPIRATION -> {
                if (!ClassFeatures.is(actor, "bard")) {
                    return ActionResult.error("Solo los bardos pueden usar Inspiración bárdica.");
                }
                GameEntity target = action.getTargetEntityId() == null
                        ? null
                        : campaigns.entity(action.getTargetEntityId()).orElse(null);
                int used = actor.getData().get("bardicInspirationUsed") instanceof Number number
                        ? number.intValue() : 0;
                if (target == null) {
                    return ActionResult.error("Selecciona un aliado para inspirarlo.");
                }
                if (!BardicInspirationRules.canInspire(actor, target, used)) {
                    return ActionResult.error("No puedes inspirar a ese objetivo (debe ser otro aliado vivo, "
                            + "a 12 casillas o menos, sin inspiración y con usos disponibles).");
                }
                target.getData().put("inspirationDie", BardicInspirationRules.DIE_SIDES);
                target.getData().put("inspirationRounds", BardicInspirationRules.ROUNDS);
                target.getData().put("inspirationPending", false);
                target.getData().put("inspirationSourceId", actor.getId());
                actor.getData().put("bardicInspirationUsed", used + 1);
                campaigns.saveEntity(target);
                message = actor.getName() + " inspira a " + target.getName() + " (d6).";
                data.put("targetId", target.getId());
                data.put("uses", used + 1);
            }
            default -> {
                return ActionResult.error("Rasgo de clase desconocido");
            }
        }
        actor.getData().put("bonusUsed", true);
        campaigns.saveEntity(actor);
        data.put("entityId", actor.getId());
        GameEvent event = events.record(campaignId, activeSceneId(campaignId), type, actor.getName(), message, data);
        return new ActionResult(true, null, null, List.of(event), null, null, null, List.of(), false);
    }

    /**
     * Short and long rests. Both require an out-of-combat, conscious and owned character.
     * Short rest spends exactly one hit die (healing die + CON, capped at max HP) and recharges
     * short-rest features; long rest restores HP, spell slots, class-feature uses and one-use
     * combat flags, and recovers at least half of the spent hit dice (minimum one).
     */
    private ActionResult rest(Long campaignId, Long userId, PlayerAction action, boolean longRest) {
        GameEntity actor = resolveActor(campaignId, userId, action);
        if (actor == null) {
            return ActionResult.error("No se encontró el personaje activo");
        }
        if (!ownedBy(actor, userId)) {
            return ActionResult.error("No controlas ese personaje");
        }
        if (!actor.isAlive()) {
            return ActionResult.error("Estás muerto y no puedes descansar.");
        }
        if (isDowned(actor)) {
            return ActionResult.error("Estás moribundo; solo puedes tirar una salvación de muerte.");
        }
        if (Conditions.of(actor).incapacitated()) {
            return ActionResult.error("Estás incapacitado y no puedes descansar.");
        }
        CombatState state = campaigns.activeCombat(campaignId);
        if (state != null && state.isActive()) {
            return ActionResult.error("No puedes descansar durante un combate.");
        }

        int maxHitDice = RestRules.hitDiceMax(actor);
        int remainingHitDice = RestRules.hitDiceRemaining(actor);
        String hitDie = RestRules.hitDieLabel(actor);
        int hpBefore = actor.getHp();
        Map<String, Object> data = new LinkedHashMap<>();
        String message;
        String type;

        if (longRest) {
            int restoredHitDice = RestRules.longRestHitDiceRemaining(maxHitDice, remainingHitDice);
            actor.setHp(actor.getMaxHp());
            actor.setAlive(true);
            actor.getData().put("hitDie", hitDie);
            actor.getData().put("hitDiceMax", maxHitDice);
            actor.getData().put("hitDiceRemaining", restoredHitDice);
            actor.getData().put("dying", false);
            actor.getData().put("stable", false);
            actor.getData().put("deathSuccesses", 0);
            actor.getData().put("deathFailures", 0);
            actor.getData().put("surprised", false);
            actor.getData().put("spellSlotsUsed", 0);
            actor.getData().put("secondWindUsed", false);
            actor.getData().put("raging", false);
            actor.getData().put("rageRounds", 0);
            actor.getData().put("ragesUsed", 0);
            actor.getData().put("bardicInspirationUsed", 0);
            actor.getData().put("inspirationDie", 0);
            actor.getData().put("inspirationPending", false);
            actor.getData().put("inspirationRounds", 0);
            actor.getData().put("inspirationSourceId", null);
            actor.getData().put("concentrationSpell", null);
            actor.getData().put("concentrationName", null);
            actor.getData().put("concentrationRounds", 0);
            actor.getData().put("blessed", false);
            actor.getData().put("blessedBy", null);
            actor.getData().put("hidden", false);
            actor.getData().put("helpAdvantage", false);
            actor.getData().put("helpedBy", null);
            TurnEconomy.reset(actor.getData(),
                    ConditionRules.effectiveSpeed(actor.getSpeed(), Conditions.of(actor)));
            // A long rest ends the character's own concentration and the buffs it granted.
            campaigns.clearConcentration(campaignId, actor.getId());
            campaigns.saveEntity(actor);

            type = PlayerAction.LONG_REST;
            message = actor.getName() + " completa un descanso largo: recupera todos sus PG ("
                    + actor.getHp() + "/" + actor.getMaxHp() + ") y " + restoredHitDice
                    + " dado(s) de golpe (" + restoredHitDice + "/" + maxHitDice + ").";
            data.put("restoredHitDice", restoredHitDice);
        } else {
            if (remainingHitDice <= 0) {
                return ActionResult.error("No te quedan dados de golpe; necesitas un descanso largo.");
            }
            int roll = dice.roll("1" + hitDie).rolls().get(0);
            int conModifier = Modifier.of(actor.getStats().constitution());
            int heal = RestRules.shortRestHeal(roll, conModifier);
            int hpAfter = Math.min(actor.getMaxHp(), hpBefore + heal);
            actor.setHp(hpAfter);
            actor.setAlive(true);
            actor.getData().put("hitDie", hitDie);
            actor.getData().put("hitDiceMax", maxHitDice);
            actor.getData().put("hitDiceRemaining", remainingHitDice - 1);
            // Second Wind recharges on a short rest; spell slots intentionally do not.
            actor.getData().put("secondWindUsed", false);
            campaigns.saveEntity(actor);

            type = PlayerAction.SHORT_REST;
            message = actor.getName() + " descansa una hora: gasta un dado de golpe (" + hitDie
                    + "), recupera " + (hpAfter - hpBefore) + " PG (" + hpAfter + "/"
                    + actor.getMaxHp() + "). Dados de golpe: " + (remainingHitDice - 1)
                    + "/" + maxHitDice + ".";
            data.put("dieRoll", roll);
            data.put("conModifier", conModifier);
            data.put("heal", hpAfter - hpBefore);
        }

        data.put("entityId", actor.getId());
        data.put("hitDie", hitDie);
        data.put("hitDiceMax", maxHitDice);
        data.put("hitDiceRemaining", actor.getData().get("hitDiceRemaining"));
        data.put("hpBefore", hpBefore);
        data.put("hpAfter", actor.getHp());
        GameEvent event = events.record(campaignId, activeSceneId(campaignId), type, actor.getName(),
                message, data);
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

    /**
     * Help (PHB): an ally within 5 ft gains advantage on its next attack against a target that is
     * also within 5 ft of the helper. The flag is consumed by {@link CombatService} on that attack.
     */
    private ActionResult help(Long campaignId, Long userId, PlayerAction action) {
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
            return ActionResult.error("Estás incapacitado y no puedes ayudar.");
        }
        CombatState state = campaigns.activeCombat(campaignId);
        if (state != null && state.isActive() && !state.isCombatantTurn(actor.getId())) {
            return ActionResult.error("No es tu turno");
        }
        if (Boolean.TRUE.equals(actor.getData().get("actionUsed"))) {
            return ActionResult.error("Ya has usado tu acción este turno.");
        }
        if (action.getTargetEntityId() == null) {
            return ActionResult.error("Selecciona un aliado al que ayudar.");
        }
        GameEntity ally = campaigns.entity(action.getTargetEntityId()).orElse(null);
        if (ally == null || !ally.isAlive() || ally.getKind() != EntityKind.PLAYER) {
            return ActionResult.error("El objetivo debe ser un aliado vivo.");
        }
        if (ally.getId().equals(actor.getId())) {
            return ActionResult.error("No puedes ayudarte a ti mismo.");
        }
        Long sceneId = activeSceneId(campaignId);
        if (sceneId != null && !sceneId.equals(ally.getSceneId())) {
            return ActionResult.error("Ese aliado no está en tu escena.");
        }
        int distance = actor.cell().chebyshevTo(ally.cell());
        if (distance > 1) {
            return ActionResult.error("El aliado debe estar a 5 pies (1 casilla) o menos.");
        }

        ally.getData().put("helpAdvantage", true);
        ally.getData().put("helpedBy", actor.getId());
        actor.getData().put("actionUsed", true);
        campaigns.saveEntity(ally);
        campaigns.saveEntity(actor);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("entityId", actor.getId());
        data.put("targetId", ally.getId());
        GameEvent event = events.record(campaignId, sceneId, PlayerAction.HELP, actor.getName(),
                actor.getName() + " ayuda a " + ally.getName()
                        + ": ventaja en su próximo ataque contra un objetivo a 5 pies del ayudante.",
                data);
        return new ActionResult(true, null, null, List.of(event), null, null, null, List.of(), false);
    }

    /**
     * Hide (PHB approximation): fails while a living enemy is adjacent. On success the character
     * gains advantage on its next attack (DM adjudication; no stealth roll is modelled yet).
     */
    private ActionResult hide(Long campaignId, Long userId, PlayerAction action) {
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
            return ActionResult.error("Estás incapacitado y no puedes ocultarte.");
        }
        CombatState state = campaigns.activeCombat(campaignId);
        if (state != null && state.isActive() && !state.isCombatantTurn(actor.getId())) {
            return ActionResult.error("No es tu turno");
        }
        if (Boolean.TRUE.equals(actor.getData().get("actionUsed"))) {
            return ActionResult.error("Ya has usado tu acción este turno.");
        }
        Long sceneId = activeSceneId(campaignId);
        boolean enemyAdjacent = sceneId != null && campaigns.entitiesInScene(sceneId).stream()
                .filter(entity -> entity.getKind() == EntityKind.ENEMY)
                .filter(GameEntity::isAlive)
                .anyMatch(entity -> entity.cell().chebyshevTo(actor.cell()) <= 1);
        if (enemyAdjacent) {
            return ActionResult.error("No puedes ocultarte con un enemigo a 5 pies o menos.");
        }

        actor.getData().put("hidden", true);
        actor.getData().put("actionUsed", true);
        campaigns.saveEntity(actor);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("entityId", actor.getId());
        GameEvent event = events.record(campaignId, sceneId, PlayerAction.HIDE, actor.getName(),
                actor.getName() + " se oculta: ventaja en su próximo ataque.", data);
        return new ActionResult(true, null, null, List.of(event), null, null, null, List.of(), false);
    }

    /**
     * Search (PHB): Perception or Investigation check that consumes the action. The check is
     * recorded like any other check so the frontend can animate the d20 result.
     */
    private ActionResult search(Long campaignId, Long userId, PlayerAction action) {
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
            return ActionResult.error("Estás incapacitado y no puedes buscar.");
        }
        CombatState state = campaigns.activeCombat(campaignId);
        if (state != null && state.isActive() && !state.isCombatantTurn(actor.getId())) {
            return ActionResult.error("No es tu turno");
        }
        if (Boolean.TRUE.equals(actor.getData().get("actionUsed"))) {
            return ActionResult.error("Ya has usado tu acción este turno.");
        }
        String skill = action.getSkill() == null
                ? "perception" : action.getSkill().toLowerCase(Locale.ROOT).trim();
        if (!"perception".equals(skill) && !"investigation".equals(skill)) {
            return ActionResult.error("Buscar usa Percepción o Investigación.");
        }
        int dc = action.getDc() == null ? 12 : action.getDc();

        ConditionSet conditions = Conditions.of(actor);
        Advantage advantage = ConditionRules.checkAdvantage(conditions, skill);
        int penalty = ConditionRules.checkPenalty(conditions);
        int inspirationBonus = consumeInspiration(actor);
        CheckResult checkResult = checkResolver.resolve(Combatants.of(actor), skill, dc, advantage,
                penalty + inspirationBonus);

        actor.getData().put("actionUsed", true);
        campaigns.saveEntity(actor);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("skill", skill);
        data.put("dc", dc);
        data.put("advantage", advantage.name());
        data.put("penalty", penalty);
        data.put("inspirationBonus", inspirationBonus);
        data.put("expression", "1d20");
        data.put("rolls", List.of(checkResult.dieRoll()));
        data.put("dieRoll", checkResult.dieRoll());
        data.put("modifier", checkResult.modifier());
        data.put("total", checkResult.total());
        data.put("success", checkResult.success());
        data.put("natural20", checkResult.natural20());
        data.put("natural1", checkResult.natural1());
        GameEvent event = events.record(campaignId, activeSceneId(campaignId), PlayerAction.SEARCH,
                actor.getName(), "Buscar con " + skill + ": " + checkResult.total() + " vs CD " + dc
                        + (checkResult.success() ? " → éxito" : " → fallo"), data);
        return new ActionResult(true, null, null, List.of(event), null, data, null, List.of(), false);
    }

    /**
     * Use an object (PHB): consumes the action to use a non-potion inventory item. Potions keep
     * their dedicated USE_ITEM path so healing is not duplicated.
     */
    private ActionResult useObject(Long campaignId, Long userId, PlayerAction action) {
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
            return ActionResult.error("Estás incapacitado y no puedes usar objetos.");
        }
        CombatState state = campaigns.activeCombat(campaignId);
        if (state != null && state.isActive() && !state.isCombatantTurn(actor.getId())) {
            return ActionResult.error("No es tu turno");
        }
        if (Boolean.TRUE.equals(actor.getData().get("actionUsed"))) {
            return ActionResult.error("Ya has usado tu acción este turno.");
        }
        String itemName = action.getItem() == null ? "" : action.getItem().trim();
        if (itemName.isEmpty()) {
            return ActionResult.error("Selecciona un objeto del inventario.");
        }
        String normalized = itemName.toLowerCase(Locale.ROOT);
        if (normalized.contains("poción") || normalized.contains("pocion") || normalized.contains("elixir")) {
            return ActionResult.error("Una poción se usa con su acción de inventario, no con Usar objeto.");
        }
        PlayerCharacter character = actor.getRefId() == null ? null
                : characters.findById(actor.getRefId()).map(characterMapper::toDomain).orElse(null);
        if (character == null) {
            return ActionResult.error("No se encontró el inventario del personaje.");
        }
        boolean inInventory = character.getInventory().stream()
                .anyMatch(item -> item.name() != null && item.name().equalsIgnoreCase(itemName));
        if (!inInventory) {
            return ActionResult.error("No llevas " + itemName + " en el inventario.");
        }

        String effect;
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("entityId", actor.getId());
        data.put("item", itemName);
        if (normalized.contains("antorcha") || normalized.contains("torch")) {
            effect = "torch";
            actor.getData().put("lightSource", "torch");
        } else if (normalized.contains("cuerda") || normalized.contains("rope")) {
            effect = "rope";
        } else if (normalized.contains("raciones") || normalized.contains("ration")) {
            effect = "rations";
        } else if (normalized.contains("herramientas") || normalized.contains("tools")) {
            effect = "tools";
        } else if (normalized.contains("símbolo") || normalized.contains("simbolo")
                || normalized.contains("holy")) {
            effect = "holy_symbol";
            actor.getData().put("holySymbolRaised", true);
        } else {
            effect = "generic";
        }
        data.put("effect", effect);
        actor.getData().put("actionUsed", true);
        campaigns.saveEntity(actor);
        GameEvent event = events.record(campaignId, activeSceneId(campaignId), PlayerAction.USE_OBJECT,
                actor.getName(), actor.getName() + " usa " + itemName + ".", data);
        return new ActionResult(true, null, null, List.of(event), null, null, null, List.of(), false);
    }

    /**
     * Ready (PHB minimal approximation): consumes the action and marks the character ready to
     * make one melee attack with its reaction when an enemy enters its reach. The stance expires
     * at the start of the character's next turn.
     */
    private ActionResult ready(Long campaignId, Long userId, PlayerAction action) {
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
            return ActionResult.error("Estás incapacitado y no puedes preparar una acción.");
        }
        CombatState state = campaigns.activeCombat(campaignId);
        if (state != null && state.isActive() && !state.isCombatantTurn(actor.getId())) {
            return ActionResult.error("No es tu turno");
        }
        if (Boolean.TRUE.equals(actor.getData().get("actionUsed"))) {
            return ActionResult.error("Ya has usado tu acción este turno.");
        }
        actor.getData().put("readied", true);
        actor.getData().put("actionUsed", true);
        campaigns.saveEntity(actor);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("entityId", actor.getId());
        GameEvent event = events.record(campaignId, activeSceneId(campaignId), PlayerAction.READY,
                actor.getName(), actor.getName()
                        + " se prepara para atacar con su reacción cuando un enemigo entre en su alcance.",
                data);
        return new ActionResult(true, null, null, List.of(event), null, null, null, List.of(), false);
    }

    /**
     * Free object interaction (PHB): once per turn, no action cost. The specific object is
     * descriptive; the mechanical effect is the spent interaction itself.
     */
    private ActionResult interact(Long campaignId, Long userId, PlayerAction action) {
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
            return ActionResult.error("Estás incapacitado y no puedes interactuar con objetos.");
        }
        CombatState state = campaigns.activeCombat(campaignId);
        if (state != null && state.isActive() && !state.isCombatantTurn(actor.getId())) {
            return ActionResult.error("No es tu turno");
        }
        if (Boolean.TRUE.equals(actor.getData().get("objectInteractionUsed"))) {
            return ActionResult.error("Ya has usado tu interacción gratuita con objetos este turno.");
        }
        actor.getData().put("objectInteractionUsed", true);
        campaigns.saveEntity(actor);

        String text = action.getText() == null || action.getText().isBlank()
                ? "interactúa con un objeto de la escena." : action.getText().trim();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("entityId", actor.getId());
        GameEvent event = events.record(campaignId, activeSceneId(campaignId), PlayerAction.INTERACT,
                actor.getName(), actor.getName() + " " + text, data);
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
                case "SPAWN_ENEMY" -> spawnEnemy(campaignId, sceneId, action.enemyName(), eventsOut, actionsOut);
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
                case "SET_SURPRISED" -> {
                    if (action.entityId() != null) {
                        GameEntity target = campaigns.entity(action.entityId()).orElse(null);
                        if (target != null && (sceneId == null || sceneId.equals(target.getSceneId()))) {
                            SurpriseRules.markSurprised(target);
                            campaigns.saveEntity(target);
                            eventsOut.add(events.record(campaignId, sceneId, "SURPRISE_APPLIED", target.getName(),
                                    target.getName() + " queda sorprendido y perderá su primer turno.",
                                    Map.of("entityId", target.getId())));
                            actionsOut.add("SET_SURPRISED");
                        }
                    }
                }
                default -> {
                    // NARRATE and unknown types need no mechanical state change.
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

    /** Test/internal seam: exposes the action ids produced while applying structured DM actions. */
    @Transactional
    List<String> applyDmActionNames(Long campaignId, Long userId, List<DmAction> actions) {
        DmResponse response = new DmResponse("", actions == null ? List.of() : actions, false, null);
        return applyDm(campaignId, userId, response).actions();
    }

    /**
     * Spawns a bestiary monster into the current scene. Unknown names and full maps are rejected
     * with a persisted event and a failure action id; they never escape as exceptions. When a
     * combat is running in this scene the creature joins it with its own initiative.
     */
    private void spawnEnemy(Long campaignId, Long sceneId, String enemyName,
                            List<GameEvent> eventsOut, List<String> actionsOut) {
        var monster = Bestiary.findByName(enemyName);
        if (monster.isEmpty()) {
            rejectSpawn(campaignId, sceneId, enemyName, "UNKNOWN_MONSTER",
                    "No se pudo invocar a \"" + (enemyName == null ? "" : enemyName)
                            + "\": esa criatura no existe en el bestiario.", eventsOut, actionsOut);
            return;
        }
        if (sceneId == null) {
            rejectSpawn(campaignId, sceneId, enemyName, "NO_ACTIVE_SCENE",
                    "No se pudo invocar a " + monster.get().name() + ": no hay una escena activa.",
                    eventsOut, actionsOut);
            return;
        }
        GameEntity enemy = campaigns.spawnEnemy(sceneId, monster.get()).orElse(null);
        if (enemy == null) {
            rejectSpawn(campaignId, sceneId, enemyName, "NO_FREE_CELL",
                    "No se pudo invocar a " + monster.get().name()
                            + ": no queda ninguna casilla libre en el mapa.", eventsOut, actionsOut);
            return;
        }

        CombatState combatState = campaigns.activeCombat(campaignId);
        CombatParticipant participant = combat.addParticipant(campaignId, combatState, enemy);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("entityId", enemy.getId());
        data.put("sceneId", sceneId);
        data.put("monsterId", monster.get().id());
        data.put("name", enemy.getName());
        data.put("x", enemy.getX());
        data.put("y", enemy.getY());
        data.put("joinedCombat", participant != null);
        if (participant != null) {
            data.put("combatId", combatState.getId());
            data.put("initiative", participant.getInitiative());
        }
        eventsOut.add(events.record(campaignId, sceneId, "ENEMY_SPAWNED", enemy.getName(),
                enemy.getName() + " irrumpe en la escena.", data));
        actionsOut.add("SPAWN_ENEMY:" + enemy.getName());
    }

    private void rejectSpawn(Long campaignId, Long sceneId, String enemyName, String reason, String message,
                             List<GameEvent> eventsOut, List<String> actionsOut) {
        eventsOut.add(events.record(campaignId, sceneId, "ENEMY_SPAWN_FAILED", "Dungeon Master", message,
                Map.of("enemyName", enemyName == null ? "" : enemyName, "reason", reason)));
        actionsOut.add("SPAWN_ENEMY_FAILED:" + reason);
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
            // Spawn before opening combat so a newly summoned creature joins the initiative order.
            case "SPAWN_ENEMY" -> 1;
            case "START_COMBAT" -> 2;
            default -> 3;
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

    /**
     * Consumes one ammunition item from the persisted character sheet before a ranged weapon
     * attack. Returns {@code null} when the attack can continue or needs no tracked ammunition,
     * and a Spanish error when the player has none. Enemy/NPC attacks never enter this player
     * action path, and spell attacks use {@code CombatService} with {@code weaponAttack=false}.
     */
    private ActionResult consumeAmmunition(GameEntity actor, Attack attack) {
        String ammunition = AmmunitionRules.ammunitionFor(attack).orElse(null);
        if (ammunition == null || actor.getKind() != EntityKind.PLAYER) {
            return null;
        }
        if (actor.getRefId() == null) {
            return ActionResult.error("No se encontró la ficha del personaje para consumir munición.");
        }
        CharacterEntity stored = characters.findById(actor.getRefId()).orElse(null);
        if (stored == null) {
            return ActionResult.error("No se encontró la ficha del personaje para consumir munición.");
        }
        PlayerCharacter character = characterMapper.toDomain(stored);
        List<InventoryItem> inventory = new ArrayList<>(
                character.getInventory() == null ? List.of() : character.getInventory());
        int slot = AmmunitionRules.findAmmunitionSlot(inventory, ammunition).orElse(-1);
        if (slot < 0 || inventory.get(slot).quantity() <= 0) {
            return ActionResult.error("No te quedan " + ammunition.toLowerCase(Locale.ROOT)
                    + " para atacar con " + attack.name() + ". Consigue munición antes de volver a disparar.");
        }
        InventoryItem current = inventory.get(slot);
        int remaining = current.quantity() - 1;
        if (remaining == 0) {
            inventory.remove(slot);
        } else {
            inventory.set(slot, new InventoryItem(current.name(), current.description(), current.icon(),
                    remaining, current.weight()));
        }
        character.setInventory(inventory);
        CharacterEntity updated = characterMapper.toEntity(character);
        updated.setOwnerId(stored.getOwnerId());
        updated.setCreatedAt(stored.getCreatedAt());
        characters.save(updated);
        return null;
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
