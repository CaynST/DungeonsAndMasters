package com.dndmasters.service;

import com.dndmasters.domain.Attack;
import com.dndmasters.domain.Ability;
import com.dndmasters.domain.Cell;
import com.dndmasters.domain.CombatParticipant;
import com.dndmasters.domain.CombatState;
import com.dndmasters.domain.EntityKind;
import com.dndmasters.domain.GameEntity;
import com.dndmasters.domain.GameMap;
import com.dndmasters.domain.GamePhase;
import com.dndmasters.engine.board.GridMath;
import com.dndmasters.engine.combat.CombatEngine;
import com.dndmasters.engine.combat.ConditionRules;
import com.dndmasters.engine.combat.ConditionSet;
import com.dndmasters.engine.combat.Conditions;
import com.dndmasters.engine.combat.DeathSaveRules;
import com.dndmasters.engine.combat.SurpriseRules;
import com.dndmasters.engine.combat.TurnEconomy;
import com.dndmasters.engine.combat.TurnManager;
import com.dndmasters.engine.dice.DiceEngine;
import com.dndmasters.engine.dice.DiceRollResult;
import com.dndmasters.engine.rules.AttackResolver;
import com.dndmasters.engine.rules.AttackResult;
import com.dndmasters.engine.rules.ClassFeatures;
import com.dndmasters.engine.rules.Combatant;
import com.dndmasters.engine.rules.Combatants;
import com.dndmasters.engine.rules.CoverRules;
import com.dndmasters.engine.rules.DamageResolver;
import com.dndmasters.engine.rules.DamageResult;
import com.dndmasters.engine.rules.FightingStyleRules;
import com.dndmasters.engine.rules.Modifier;
import com.dndmasters.engine.rules.RageRules;
import com.dndmasters.engine.rules.SneakAttackRules;
import com.dndmasters.engine.spells.Spell;
import com.dndmasters.persistence.entity.CombatParticipantEntity;
import com.dndmasters.persistence.entity.CombatStateEntity;
import com.dndmasters.persistence.mapper.GameEntityMapper;
import com.dndmasters.persistence.repository.BoardEntityRepository;
import com.dndmasters.persistence.repository.CombatParticipantRepository;
import com.dndmasters.persistence.repository.CombatStateRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Combate: iniciativa, turnos, ataques, daño y resolución automática de enemigos/aliados. */
@Service
public class CombatService {

    private final CampaignService campaigns;
    private final GameEventService events;
    private final GameEntityMapper entityMapper;
    private final CombatStateRepository combatStates;
    private final CombatParticipantRepository combatParticipants;
    private final BoardEntityRepository boardEntities;
    private final CombatEngine combatEngine;
    private final TurnManager turns;
    private final AttackResolver attackResolver;
    private final DamageResolver damageResolver;
    private final DiceEngine dice;

    public CombatService(CampaignService campaigns, GameEventService events, GameEntityMapper entityMapper,
                         CombatStateRepository combatStates, CombatParticipantRepository combatParticipants,
                         BoardEntityRepository boardEntities, CombatEngine combatEngine, TurnManager turns,
                         AttackResolver attackResolver, DamageResolver damageResolver, DiceEngine dice) {
        this.campaigns = campaigns;
        this.events = events;
        this.entityMapper = entityMapper;
        this.combatStates = combatStates;
        this.combatParticipants = combatParticipants;
        this.boardEntities = boardEntities;
        this.combatEngine = combatEngine;
        this.turns = turns;
        this.attackResolver = attackResolver;
        this.damageResolver = damageResolver;
        this.dice = dice;
    }

    @Transactional
    public CombatState startCombat(Long campaignId, Long sceneId) {
        List<GameEntity> entities = campaigns.entitiesInScene(sceneId);
        List<Combatant> combatants = entities.stream()
                .filter(GameEntity::isCombatant)
                .filter(GameEntity::isAlive)
                .map(Combatants::of)
                .toList();
        if (combatants.size() < 2) {
            throw new IllegalStateException("Not enough combatants to start a combat");
        }
        CombatState state = combatEngine.start(campaignId, sceneId, combatants);
        persist(state);
        // Fresh turn economy, class feature uses and one-use combat-action flags at combat start.
        state.getParticipants().forEach(p -> {
            campaigns.entity(p.getEntityId()).ifPresent(entity -> {
                entity.getData().put("helpAdvantage", false);
                entity.getData().remove("helpedBy");
                entity.getData().put("hidden", false);
                campaigns.saveEntity(entity);
            });
            campaigns.resetTurnUsage(p.getEntityId());
            campaigns.resetFeatureUses(p.getEntityId());
        });
        campaigns.updateCampaignPhase(campaignId, GamePhase.COMBAT, sceneId);

        List<Map<String, Object>> order = state.getParticipants().stream()
                .map(p -> Map.<String, Object>of("name", p.getName(), "initiative", p.getInitiative(), "entityId", p.getEntityId()))
                .toList();
        events.record(campaignId, sceneId, "COMBAT_STARTED", "Dungeon Master",
                "¡Comienza el combate! Orden de iniciativa: " + String.join(", ",
                        state.getParticipants().stream().map(p -> p.getName() + " (" + p.getInitiative() + ")").toList()),
                Map.of("order", order));
        // A surprised creature loses its first turn, even if combat opens on that turn.
        skipSurprisedAtStart(campaignId, state);
        events.record(campaignId, sceneId, "TURN_CHANGED", state.currentParticipant().getName(),
                "Turno de " + state.currentParticipant().getName() + " (ronda " + state.getRound() + ").",
                Map.of("entityId", state.currentParticipant().getEntityId(), "round", state.getRound()));
        return state;
    }

    /**
     * Adds a creature to a running combat with its own initiative. The list stays sorted by
     * initiative; if the newcomer is inserted before the active participant, the turn pointer is
     * shifted so the creature that was acting keeps its turn. Returns the existing participant
     * when the creature is already in the fight, or {@code null} when the combat is not running
     * in the creature's scene.
     */
    @Transactional
    public CombatParticipant addParticipant(Long campaignId, CombatState state, GameEntity entity) {
        if (state == null || !state.isActive() || entity == null || entity.getId() == null) {
            return null;
        }
        if (state.getSceneId() != null && !state.getSceneId().equals(entity.getSceneId())) {
            return null;
        }
        CombatParticipant existing = state.getParticipants().stream()
                .filter(p -> entity.getId().equals(p.getEntityId()))
                .findFirst().orElse(null);
        if (existing != null) {
            return existing;
        }

        int dieRoll = dice.roll("1d20").rolls().get(0);
        int initiative = dieRoll + Combatants.of(entity).dexterityModifier();
        List<CombatParticipant> participants = state.getParticipants();
        int insertAt = 0;
        while (insertAt < participants.size() && participants.get(insertAt).getInitiative() >= initiative) {
            insertAt++;
        }
        boolean hadCurrent = state.currentParticipant() != null;
        CombatParticipant participant = new CombatParticipant(
                null, entity.getId(), entity.getName(), entity.getKind(), initiative, insertAt, true);
        participants.add(insertAt, participant);
        if (hadCurrent && insertAt <= state.getCurrentTurnIndex()) {
            state.setCurrentTurnIndex(state.getCurrentTurnIndex() + 1);
        }
        for (int i = 0; i < participants.size(); i++) {
            participants.get(i).setOrderIndex(i);
        }
        persist(state);

        // The newcomer has not spent action, movement, bonus action or reaction yet.
        campaigns.resetTurnUsage(entity.getId());
        campaigns.resetFeatureUses(entity.getId());
        return participant;
    }

    @Transactional
    public void persist(CombatState state) {
        CombatStateEntity entity = state.getId() == null
                ? new CombatStateEntity()
                : combatStates.findById(state.getId()).orElseGet(CombatStateEntity::new);
        boolean isNew = entity.getId() == null;
        entity.setCampaignId(state.getCampaignId());
        entity.setSceneId(state.getSceneId());
        entity.setActive(state.isActive());
        entity.setRound(state.getRound());
        entity.setCurrentTurnIndex(state.getCurrentTurnIndex());
        entity.setStatus(state.getStatus());
        if (isNew) {
            entity.setStartedAt(Instant.now().toString());
        }
        combatStates.save(entity);
        state.setId(entity.getId());

        combatParticipants.deleteByCombatId(entity.getId());
        List<CombatParticipantEntity> rows = new ArrayList<>();
        for (CombatParticipant p : state.getParticipants()) {
            CombatParticipantEntity row = new CombatParticipantEntity();
            row.setCombatId(entity.getId());
            row.setEntityId(p.getEntityId());
            row.setInitiative(p.getInitiative());
            row.setOrderIndex(p.getOrderIndex());
            row.setAlive(p.isAlive());
            rows.add(row);
        }
        combatParticipants.saveAll(rows);
    }

    @Transactional
    public void finish(Long campaignId, CombatState state) {        boolean victory = state.aliveEnemies() == 0;
        boolean defeat = state.aliveHeroes() == 0;
        turns.finish(state);
        CombatStateEntity entity = combatStates.findById(state.getId()).orElseThrow();
        entity.setActive(false);
        entity.setStatus(state.getStatus());
        entity.setEndedAt(Instant.now().toString());
        combatStates.save(entity);

        String message = victory ? "¡Los enemigos han caído!" : defeat ? "La compañía ha sido derrotada..." : "El combate termina.";
        events.record(campaignId, state.getSceneId(), "COMBAT_ENDED", "Dungeon Master", message,
                Map.of("victory", victory, "defeat", defeat));
    }

    /** Advances the turn pointer and records the change. */
    @Transactional
    public CombatParticipant advanceTurn(Long campaignId, CombatState state) {
        CombatParticipant next = advanceSkippingSurprise(campaignId, state);
        if (next != null) {
            persist(state);
            campaigns.startTurn(campaignId, next.getEntityId());
            events.record(campaignId, state.getSceneId(), "TURN_CHANGED", next.getName(),
                    "Turno de " + next.getName() + " (ronda " + state.getRound() + ").",
                    Map.of("entityId", next.getEntityId(), "round", state.getRound()));
        }
        return next;
    }

    /** Advances past any surprised participants, consuming their flag exactly once. */
    private CombatParticipant advanceSkippingSurprise(Long campaignId, CombatState state) {
        CombatParticipant next = turns.advance(state);
        int guard = 0;
        while (next != null && guard++ <= state.getParticipants().size()
                && consumeSurprise(campaignId, state, next)) {
            next = turns.advance(state);
        }
        return next;
    }

    /** Skips surprised creatures that would act when combat opens. */
    private void skipSurprisedAtStart(Long campaignId, CombatState state) {
        int guard = 0;
        while (guard++ <= state.getParticipants().size()
                && consumeSurprise(campaignId, state, state.currentParticipant())) {
            turns.advance(state);
        }
        persist(state);
    }

    /** Clears the entity's surprise flag, records the lost turn and reports the skip. */
    private boolean consumeSurprise(Long campaignId, CombatState state, CombatParticipant participant) {
        if (participant == null) {
            return false;
        }
        GameEntity entity = campaigns.entity(participant.getEntityId()).orElse(null);
        if (entity == null || !SurpriseRules.consumeSurprised(entity)) {
            return false;
        }
        campaigns.saveEntity(entity);
        events.record(campaignId, state.getSceneId(), "SURPRISED", entity.getName(),
                entity.getName() + " está sorprendido y pierde su turno.",
                Map.of("entityId", entity.getId()));
        return true;
    }

    /**
     * Resolves every following turn until control returns to a human player or combat ends.
     * Enemies and DM-controlled allies move toward their nearest target and attack.
     */
    @Transactional
    public List<com.dndmasters.domain.GameEvent> runAutomaticTurns(Long campaignId, CombatState state) {
        List<com.dndmasters.domain.GameEvent> recorded = new ArrayList<>();
        int guard = 0;
        while (state.isActive() && guard++ < 60) {
            CombatParticipant current = state.currentParticipant();
            if (current == null) {
                break;
            }
            GameEntity actor = campaigns.entity(current.getEntityId()).orElse(null);
            if (actor == null || !actor.isAlive()) {
                turns.markDead(state, current.getEntityId());
                CombatParticipant next = advanceSkippingSurprise(campaignId, state);
                persist(state);
                if (next != null) {
                    campaigns.startTurn(campaignId, next.getEntityId());
                }
                continue;
            }
            if (isPlayerControlled(actor)) {
                break;
            }
            recorded.addAll(resolveAutomaticTurn(campaignId, state, actor));
            if (turns.isFinished(state)) {
                finish(campaignId, state);
                break;
            }
            CombatParticipant next = advanceSkippingSurprise(campaignId, state);
            persist(state);
            if (next != null) {
                campaigns.startTurn(campaignId, next.getEntityId());
                recorded.add(events.record(campaignId, state.getSceneId(), "TURN_CHANGED", next.getName(),
                        "Turno de " + next.getName() + " (ronda " + state.getRound() + ").",
                        Map.of("entityId", next.getEntityId(), "round", state.getRound())));
            }
        }
        // Every advance above already called startTurn, so the human's economy is fresh.
        return recorded;
    }

    private List<com.dndmasters.domain.GameEvent> resolveAutomaticTurn(Long campaignId, CombatState state, GameEntity actor) {        List<com.dndmasters.domain.GameEvent> recorded = new ArrayList<>();
        List<GameEntity> sceneEntities = campaigns.entitiesInScene(state.getSceneId());
        List<GameEntity> targets = sceneEntities.stream()
                .filter(GameEntity::isAlive)
                .filter(e -> e.getId() != actor.getId())
                .filter(e -> isEnemy(actor) ? e.getKind() == EntityKind.PLAYER : e.getKind() == EntityKind.ENEMY)
                .toList();
        if (targets.isEmpty()) {
            return recorded;
        }

        GameEntity nearest = targets.stream()
                .min(Comparator.comparingInt(t -> actor.cell().chebyshevTo(t.cell())))
                .orElseThrow();

        if (Conditions.of(actor).incapacitated()) {
            recorded.add(events.record(campaignId, state.getSceneId(), "NPC_TURN", actor.getName(),
                    actor.getName() + " está incapacitado y pierde su turno.",
                    Map.of("entityId", actor.getId())));
            return recorded;
        }

        recorded.add(events.record(campaignId, state.getSceneId(), "NPC_TURN", actor.getName(),
                actor.getName() + " actúa.", Map.of("entityId", actor.getId())));

        moveToward(campaignId, state, actor, nearest, sceneEntities, recorded);
        // A readied action or opportunity attack provoked by the move may have killed it.
        if (!actor.isAlive()) {
            return recorded;
        }

        List<Attack> attacks = entityMapper.attacksOf(actor);
        int distance = actor.cell().chebyshevTo(nearest.cell());
        Attack attack = chooseAttack(attacks, distance);
        if (attack != null) {
            int attackCount = "AGGRESSIVE".equals(String.valueOf(actor.getData().get("behavior"))) ? 2 : 1;
            for (int i = 0; i < attackCount && nearest.isAlive(); i++) {
                attack(campaignId, state, actor, nearest, attack,
                        com.dndmasters.engine.rules.Advantage.NONE, true, recorded);
            }
        }
        return recorded;
    }

    private void moveToward(Long campaignId, CombatState state, GameEntity actor, GameEntity target,
                            List<GameEntity> sceneEntities, List<com.dndmasters.domain.GameEvent> recorded) {
        GameMap map = campaigns.mapForScene(state.getSceneId());
        int speed = ConditionRules.effectiveSpeed(actor.getSpeed(), Conditions.of(actor));
        if (map == null || speed <= 0) {
            return;
        }
        Set<Cell> occupied = new HashSet<>();
        sceneEntities.stream().filter(GameEntity::isAlive).filter(e -> e.getId() != actor.getId())
                .forEach(e -> occupied.add(e.cell()));

        Cell position = actor.cell();
        int moved = 0;
        for (int step = 0; step < speed; step++) {
            if (position.isAdjacent(target.cell())) {
                break;
            }
            Cell next = greedyStep(map, position, target.cell(), occupied);
            if (next == null) {
                break;
            }
            occupied.remove(position);
            position = next;
            occupied.add(position);
            moved++;
        }
        if (moved > 0) {
            Cell previous = actor.cell();
            actor.moveTo(position);
            campaigns.saveEntity(actor);
            recorded.add(events.record(campaignId, state.getSceneId(), "MOVE", actor.getName(),
                    actor.getName() + " se mueve " + moved + " casilla(s).",
                    Map.of("entityId", actor.getId(), "x", position.x(), "y", position.y(), "cost", moved)));
            // Symmetric reactions: players may strike a fleeing enemy and use a readied action
            // when an enemy walks into their reach.
            recorded.addAll(opportunityAttacks(campaignId, state, actor, previous));
            recorded.addAll(readiedAttacks(campaignId, state, actor, previous));
        }
    }

    private Cell greedyStep(GameMap map, Cell from, Cell to, Set<Cell> occupied) {
        Cell best = null;
        int bestDistance = from.chebyshevTo(to);
        for (Cell neighbor : GridMath.neighbors(from)) {
            if (map.blocksMovement(neighbor) || occupied.contains(neighbor)) {
                continue;
            }
            int distance = neighbor.chebyshevTo(to);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = neighbor;
            }
        }
        return best;
    }

    private Attack chooseAttack(List<Attack> attacks, int distance) {
        if (attacks.isEmpty()) {
            return Attack.melee("Golpe", 2, "1d4");
        }
        Attack melee = attacks.stream().filter(a -> !a.ranged()).findFirst().orElse(null);
        Attack ranged = attacks.stream().filter(Attack::ranged).findFirst().orElse(null);
        if (distance <= 1 && melee != null) {
            return melee;
        }
        if (ranged != null && distance <= ranged.range()) {
            return ranged;
        }
        return distance <= 1 ? attacks.get(0) : null;
    }

    @Transactional
    public List<com.dndmasters.domain.GameEvent> attack(Long campaignId, CombatState state, GameEntity attacker,
                                                        GameEntity target, Attack attack) {
        return attack(campaignId, state, attacker, target, attack,
                com.dndmasters.engine.rules.Advantage.NONE, true, new ArrayList<>());
    }

    @Transactional
    public List<com.dndmasters.domain.GameEvent> attack(Long campaignId, CombatState state, GameEntity attacker,
                                                        GameEntity target, Attack attack,
                                                        com.dndmasters.engine.rules.Advantage advantage) {
        return attack(campaignId, state, attacker, target, attack, advantage, true, new ArrayList<>());
    }

    /** {@code weaponAttack=false} keeps Rage/Sneak Attack out of spell attacks. */
    @Transactional
    public List<com.dndmasters.domain.GameEvent> attack(Long campaignId, CombatState state, GameEntity attacker,
                                                        GameEntity target, Attack attack,
                                                        com.dndmasters.engine.rules.Advantage advantage,
                                                        boolean weaponAttack) {
        return attack(campaignId, state, attacker, target, attack, advantage, weaponAttack, new ArrayList<>());
    }

    private List<com.dndmasters.domain.GameEvent> attack(Long campaignId, CombatState state, GameEntity attacker,
                                                         GameEntity target, Attack attack,
                                                         com.dndmasters.engine.rules.Advantage advantage,
                                                         boolean weaponAttack,
                                                         List<com.dndmasters.domain.GameEvent> recorded) {
        GameMap map = state.getSceneId() == null ? null : campaigns.mapForScene(state.getSceneId());
        CoverRules.Result cover = CoverRules.check(map, attacker.cell(), target.cell(), attack.ranged());
        if (cover.blocksAttack()) {
            Map<String, Object> blocked = new LinkedHashMap<>();
            blocked.put("attackerId", attacker.getId());
            blocked.put("attackerName", attacker.getName());
            blocked.put("targetId", target.getId());
            blocked.put("targetName", target.getName());
            blocked.put("attack", attack.name());
            blocked.put("cover", cover.cover().name());
            blocked.put("coverBonus", cover.acBonus());
            blocked.put("reason", cover.reason());
            recorded.add(events.record(campaignId, state.getSceneId(), "ATTACK_BLOCKED", attacker.getName(),
                    cover.reason(), blocked));
            return recorded;
        }

        Combatant attackerC = Combatants.of(attacker);
        Combatant targetC = Combatants.of(target);
        targetC.setAc(target.getAc() + cover.acBonus());
        boolean melee = !attack.ranged();
        ConditionSet attackerConditions = Conditions.of(attacker);
        ConditionSet targetConditions = Conditions.of(target);
        boolean dodging = Boolean.TRUE.equals(target.getData().get("dodging"));
        // Conditions and Dodge combine into a single 5e roll; advantage and disadvantage cancel.
        com.dndmasters.engine.rules.Advantage effective = ConditionRules
                .attackAdvantage(attackerConditions, targetConditions, melee, dodging);
        effective = combine(effective, advantage);
        // One-use Help and Hide flags are consumed by the next attack that actually rolls.
        boolean helped = consumeHelpAdvantage(attacker, target);
        boolean hidden = Boolean.TRUE.equals(attacker.getData().get("hidden"));
        if (hidden) {
            attacker.getData().put("hidden", false);
            campaigns.saveEntity(attacker);
        }
        if (helped) {
            effective = combine(effective, com.dndmasters.engine.rules.Advantage.ADVANTAGE);
        }
        if (hidden) {
            effective = combine(effective, com.dndmasters.engine.rules.Advantage.ADVANTAGE);
        }
        // Protection (automatic reaction approximation): a living, conscious ally with the
        // style within 5 ft of the target spends its reaction to impose disadvantage.
        GameEntity protector = findProtector(state, attacker, target);
        if (protector != null) {
            protector.getData().put("reactionUsed", true);
            campaigns.saveEntity(protector);
            effective = combine(effective, com.dndmasters.engine.rules.Advantage.DISADVANTAGE);
            recorded.add(events.record(campaignId, state.getSceneId(), "PROTECTION", protector.getName(),
                    protector.getName() + " usa su reacción para proteger a " + target.getName()
                            + " (desventaja para " + attacker.getName() + ").",
                    Map.of("protectorId", protector.getId(), "protectedId", target.getId(),
                            "attackerId", attacker.getId())));
        }
        int blessBonus = Boolean.TRUE.equals(attacker.getData().get("blessed"))
                ? dice.roll("1d4").total() : 0;
        int inspirationBonus = consumeInspiration(attacker);
        Object styleValue = attacker.getData().get("fightingStyle");
        String fightingStyle = styleValue instanceof String value ? value : "";
        int styleAttackBonus = weaponAttack
                ? FightingStyleRules.archeryBonus(fightingStyle, attack.ranged()) : 0;
        int attackBonusBonus = blessBonus + styleAttackBonus + inspirationBonus;
        Attack rollAttack = attackBonusBonus > 0 ? attack.withAttackBonus(attackBonusBonus) : attack;
        AttackResult resolved = attackResolver.resolve(attackerC, targetC, rollAttack, effective);
        boolean autoCritical = resolved.hit() && ConditionRules.automaticCritical(targetConditions, melee);
        AttackResult attackResult = autoCritical && !resolved.critical()
                ? new AttackResult(resolved.attackName(), resolved.dieRoll(), resolved.attackBonus(),
                        resolved.total(), resolved.targetAc(), true, true, resolved.fumble())
                : resolved;

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("attackerId", attacker.getId());
        data.put("attackerName", attacker.getName());
        data.put("targetId", target.getId());
        data.put("targetName", target.getName());
        data.put("attack", attack.name());
        data.put("roll", attackResult.dieRoll());
        data.put("total", attackResult.total());
        data.put("ac", attackResult.targetAc());
        data.put("hit", attackResult.hit());
        data.put("critical", attackResult.critical());
        data.put("fumble", attackResult.fumble());
        data.put("advantage", effective.name());
        data.put("helped", helped);
        data.put("hidden", hidden);
        data.put("autoCritical", autoCritical);
        data.put("cover", cover.cover().name());
        data.put("coverBonus", cover.acBonus());
        data.put("blessBonus", blessBonus);
        data.put("fightingStyleAttackBonus", styleAttackBonus);
        data.put("inspirationBonus", inspirationBonus);
        data.put("protection", protector != null);
        if (protector != null) {
            data.put("protectorId", protector.getId());
            data.put("protectorName", protector.getName());
        }

        String coverNote = cover.acBonus() > 0 ? " (cobertura +" + cover.acBonus() + " CA)" : "";

        if (attackResult.hit()) {
            int rageBonus = weaponAttack ? RageRules.damageBonus(attacker, melee) : 0;
            int duelingBonus = weaponAttack
                    ? FightingStyleRules.duelingBonus(fightingStyle, melee, attack.twoHanded()) : 0;
            boolean greatWeaponFighting = weaponAttack
                    && FightingStyleRules.rerollsLowDamageDice(fightingStyle, melee, attack.twoHanded());
            int bonusDamage = rageBonus + duelingBonus;
            boolean sneakAttack = false;
            int sneakDamage = 0;
            if (weaponAttack) {
                boolean allyAdjacent = state.getSceneId() != null
                        && SneakAttackRules.allyAdjacent(attacker, target,
                                campaigns.entitiesInScene(state.getSceneId()));
                if (SneakAttackRules.canUse(attacker,
                        effective == com.dndmasters.engine.rules.Advantage.ADVANTAGE,
                        effective == com.dndmasters.engine.rules.Advantage.DISADVANTAGE,
                        allyAdjacent)) {
                    sneakAttack = true;
                    sneakDamage = dice.roll(
                            SneakAttackRules.diceCount(ClassFeatures.level(attacker)) + "d6").total();
                    bonusDamage += sneakDamage;
                }
            }
            boolean resistance = weaponAttack && RageRules.resistsPhysicalDamage(target);
            DamageResult damage = damageResolver.apply(attack, attackResult, targetC, bonusDamage,
                    resistance, greatWeaponFighting);
            if (sneakAttack) {
                attacker.getData().put("sneakAttackUsed", true);
                campaigns.saveEntity(attacker);
            }
            data.put("rageBonus", rageBonus);
            data.put("duelingBonus", duelingBonus);
            data.put("greatWeaponFighting", greatWeaponFighting);
            data.put("greatWeaponRerolls", damage.rerolls());
            data.put("greatWeaponBonus", damage.rerollBonus());
            data.put("sneakAttack", sneakAttack);
            data.put("sneakAttackDamage", sneakDamage);
            data.put("resisted", resistance);

            String verb = attackResult.critical() ? "¡CRÍTICO! " + attacker.getName() + " impacta a "
                    : attacker.getName() + " impacta a ";
            applyDamageAndConsequences(campaignId, state, attacker, target, attackResult, damage, data, recorded,
                    "ATTACK", verb + target.getName() + " con " + attack.name() + " ("
                            + attackResult.total() + " vs CA " + attackResult.targetAc() + ")" + coverNote
                            + " y causa " + damage.total() + " de daño.");
        } else {
            recorded.add(events.record(campaignId, state.getSceneId(), "ATTACK_MISS", attacker.getName(),
                    attacker.getName() + " falla su ataque contra " + target.getName() + " (" + attackResult.total()
                            + " vs CA " + attackResult.targetAc() + ")" + coverNote + ".",
                    data));
        }
        return recorded;
    }

    /**
     * Applies already-rolled damage and handles dying/death. Shared by weapon attacks and
     * spell damage so both paths persist the same state and emit the same lifecycle events.
     */
    private void applyDamageAndConsequences(Long campaignId, CombatState state, GameEntity attacker,
                                            GameEntity target, AttackResult attackResult, DamageResult damage,
                                            Map<String, Object> data,
                                            List<com.dndmasters.domain.GameEvent> recorded,
                                            String eventType, String message) {
        target.setHp(damage.targetHpAfter());
        data.put("damage", damage.total());
        data.put("damageRolls", damage.rolls());
        data.put("hpAfter", damage.targetHpAfter());
        data.put("dead", damage.dead());

        boolean concentrationBroken = false;
        if (target.getKind() == EntityKind.PLAYER
                && target.getData().get("concentrationSpell") != null
                && damage.total() > 0) {
            int conMod = Modifier.of(target.getStats().get(Ability.CONSTITUTION));
            int bless = Boolean.TRUE.equals(target.getData().get("blessed"))
                    ? dice.roll("1d4").total() : 0;
            int inspiration = consumeInspiration(target);
            int saveTotal = dice.roll("1d20").total() + conMod + bless + inspiration;
            int dc = Math.max(10, damage.total() / 2);
            data.put("concentrationSave", saveTotal);
            data.put("concentrationDc", dc);
            data.put("concentrationBless", bless);
            data.put("concentrationInspiration", inspiration);
            if (saveTotal < dc) {
                concentrationBroken = true;
                campaigns.clearConcentration(campaignId, target.getId());
                target.getData().put("concentrationSpell", null);
                target.getData().put("concentrationName", null);
                target.getData().put("concentrationRounds", 0);
                target.getData().put("blessed", false);
                target.getData().put("blessedBy", null);
                data.put("concentrationBroken", true);
            }
        }

        boolean playerDown = damage.targetHpAfter() <= 0 && target.getKind() == EntityKind.PLAYER;
        if (playerDown) {
            DeathSaveRules.State current = deathState(target);
            DeathSaveRules.State next = current.dying()
                    ? DeathSaveRules.onDamage(current, damage.total(), target.getMaxHp(), attackResult.critical())
                    : DeathSaveRules.State.startDying();
            applyDeathState(target, next);
            campaigns.saveEntity(target);
            data.put("dying", !next.dead());
            data.put("successes", next.successes());
            data.put("failures", next.failures());
        } else {
            target.setAlive(!damage.dead());
            campaigns.saveEntity(target);
        }

        recorded.add(events.record(campaignId, state.getSceneId(), eventType, attacker.getName(), message, data));

        if (concentrationBroken) {
            recorded.add(events.record(campaignId, state.getSceneId(), "CONCENTRATION_BROKEN", target.getName(),
                    target.getName() + " pierde la concentración.", Map.of("entityId", target.getId())));
        }

        if (playerDown) {
            DeathSaveRules.State stateAfter = deathState(target);
            if (stateAfter.dead()) {
                turns.markDead(state, target.getId());
                if (state.getId() != null) {
                    persist(state);
                }
                recorded.add(events.record(campaignId, state.getSceneId(), "DEATH", target.getName(),
                        target.getName() + " muere.", Map.of("entityId", target.getId())));
            } else {
                recorded.add(events.record(campaignId, state.getSceneId(), "DYING", target.getName(),
                        target.getName() + " cae moribundo (" + stateAfter.successes() + " éxitos, "
                                + stateAfter.failures() + " fallos).",
                        Map.of("entityId", target.getId(), "successes", stateAfter.successes(),
                                "failures", stateAfter.failures())));
            }
        } else if (!target.isAlive()) {
            turns.markDead(state, target.getId());
            if (state.getId() != null) {
                persist(state);
            }
            recorded.add(events.record(campaignId, state.getSceneId(), "DEATH", target.getName(),
                    target.getName() + " cae derrotado.", Map.of("entityId", target.getId())));
        }
    }

    /** Applies spell damage without an attack roll (auto-hit or failed save). */
    @Transactional
    public List<com.dndmasters.domain.GameEvent> spellDamage(Long campaignId, CombatState state,
                                                             GameEntity caster, GameEntity target,
                                                             Spell spell, boolean critical) {
        Combatant targetC = Combatants.of(target);
        Attack damageAttack = Attack.ranged(spell.name(), 0, spell.damage(), spell.range());
        AttackResult hit = new AttackResult(spell.name(), 0, 0, 0, target.getAc(), true, critical, false);
        DamageResult damage = damageResolver.apply(damageAttack, hit, targetC);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("casterId", caster.getId());
        data.put("casterName", caster.getName());
        data.put("targetId", target.getId());
        data.put("targetName", target.getName());
        data.put("spell", spell.id());
        data.put("spellName", spell.name());
        data.put("damageType", spell.damageType());
        data.put("attack", spell.name());
        data.put("attackerId", caster.getId());
        data.put("attackerName", caster.getName());
        data.put("ac", target.getAc());
        data.put("hit", true);
        data.put("roll", 0);
        data.put("total", 0);
        data.put("fumble", false);
        data.put("critical", critical);

        List<com.dndmasters.domain.GameEvent> recorded = new ArrayList<>();
        applyDamageAndConsequences(campaignId, state, caster, target, hit, damage, data, recorded,
                "SPELL_DAMAGE", caster.getName() + " causa " + damage.total() + " de daño a "
                        + target.getName() + " con " + spell.name() + ".");
        return recorded;
    }

    /** Reads the death-save state stored on an entity's data map. */
    public DeathSaveRules.State deathState(GameEntity entity) {
        return new DeathSaveRules.State(
                Boolean.TRUE.equals(entity.getData().get("dying")),
                Boolean.TRUE.equals(entity.getData().get("stable")),
                intOf(entity.getData().get("deathSuccesses")),
                intOf(entity.getData().get("deathFailures")),
                false,
                entity.getHp());
    }

    /** Writes the death-save state back to the entity (HP, alive flag and counters). */
    public void applyDeathState(GameEntity entity, DeathSaveRules.State state) {
        entity.getData().put("dying", state.dying());
        entity.getData().put("stable", state.stable());
        entity.getData().put("deathSuccesses", state.successes());
        entity.getData().put("deathFailures", state.failures());
        entity.setHp(state.hpAfter());
        entity.setAlive(!state.dead());
    }

    /** Marks a participant dead and persists the combat state. */
    public void markParticipantDead(CombatState state, long entityId) {
        turns.markDead(state, entityId);
        if (state.getId() != null) {
            persist(state);
        }
    }

    private int intOf(Object value) {
        return value instanceof Number n ? n.intValue() : 0;
    }

    /** Rolls and clears a prepared Bardic Inspiration die; returns 0 when none is pending. */
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

    /** Advantage and disadvantage cancel when both are present. */
    private com.dndmasters.engine.rules.Advantage combine(
            com.dndmasters.engine.rules.Advantage left,
            com.dndmasters.engine.rules.Advantage right) {
        if (left == right) {
            return left;
        }
        if (left == com.dndmasters.engine.rules.Advantage.NONE) {
            return right;
        }
        if (right == com.dndmasters.engine.rules.Advantage.NONE) {
            return left;
        }
        return com.dndmasters.engine.rules.Advantage.NONE;
    }

    /**
     * Creatures adjacent to the mover's previous cell may use their reaction to make one melee
     * attack when the mover leaves their reach. The rule is symmetric: enemies provoke player
     * opportunity attacks just like players provoke enemy ones. Disengage prevents it and each
     * creature has a single reaction per turn.
     */
    @Transactional
    public List<com.dndmasters.domain.GameEvent> opportunityAttacks(Long campaignId, CombatState state,
                                                                    GameEntity mover, Cell previousCell) {
        List<com.dndmasters.domain.GameEvent> recorded = new ArrayList<>();
        if (state.getSceneId() == null) {
            return recorded;
        }
        EntityKind attackerKind = switch (mover.getKind()) {
            case PLAYER -> EntityKind.ENEMY;
            case ENEMY -> EntityKind.PLAYER;
            default -> null;
        };
        if (attackerKind == null) {
            return recorded;
        }
        if (TurnEconomy.flag(mover.getData(), "disengaged")) {
            return recorded;
        }
        List<GameEntity> sceneEntities = campaigns.entitiesInScene(state.getSceneId());
        List<GameEntity> attackers = sceneEntities.stream()
                .filter(e -> e.getKind() == attackerKind)
                .filter(GameEntity::isAlive)
                .filter(e -> e.getId() != mover.getId())
                .filter(e -> !TurnEconomy.flag(e.getData(), "reactionUsed"))
                .filter(e -> !Conditions.of(e).incapacitated())
                .filter(e -> e.cell().chebyshevTo(previousCell) <= 1)
                .filter(e -> e.cell().chebyshevTo(mover.cell()) > 1)
                .toList();
        for (GameEntity attacker : attackers) {
            if (!mover.isAlive()) {
                break;
            }
            Attack melee = entityMapper.attacksOf(attacker).stream()
                    .filter(a -> !a.ranged()).findFirst().orElse(null);
            if (melee == null) {
                continue;
            }
            attacker.getData().put("reactionUsed", true);
            campaigns.saveEntity(attacker);
            recorded.add(events.record(campaignId, state.getSceneId(), "OPPORTUNITY_ATTACK", attacker.getName(),
                    attacker.getName() + " realiza un ataque de oportunidad contra " + mover.getName() + ".",
                    Map.of("entityId", attacker.getId(), "targetId", mover.getId())));
            recorded.addAll(attack(campaignId, state, attacker, mover, melee,
                    com.dndmasters.engine.rules.Advantage.NONE));
        }
        return recorded;
    }

    /**
     * Minimal Ready (R-03): a player who readied a melee attack may use its reaction when an enemy
     * enters its reach. The stance is consumed by the trigger and expires at the player's next turn.
     */
    @Transactional
    public List<com.dndmasters.domain.GameEvent> readiedAttacks(Long campaignId, CombatState state,
                                                                GameEntity mover, Cell previousCell) {
        List<com.dndmasters.domain.GameEvent> recorded = new ArrayList<>();
        if (state.getSceneId() == null || mover.getKind() != EntityKind.ENEMY) {
            return recorded;
        }
        List<GameEntity> ready = campaigns.entitiesInScene(state.getSceneId()).stream()
                .filter(e -> e.getKind() == EntityKind.PLAYER)
                .filter(GameEntity::isAlive)
                .filter(e -> e.getId() != mover.getId())
                .filter(e -> TurnEconomy.flag(e.getData(), "readied"))
                .filter(e -> !TurnEconomy.flag(e.getData(), "reactionUsed"))
                .filter(e -> !Conditions.of(e).incapacitated())
                .filter(e -> e.cell().chebyshevTo(previousCell) > 1)
                .filter(e -> e.cell().chebyshevTo(mover.cell()) <= 1)
                .toList();
        for (GameEntity attacker : ready) {
            if (!mover.isAlive()) {
                break;
            }
            Attack melee = entityMapper.attacksOf(attacker).stream()
                    .filter(a -> !a.ranged()).findFirst().orElse(null);
            if (melee == null) {
                continue;
            }
            attacker.getData().put("readied", false);
            attacker.getData().put("reactionUsed", true);
            campaigns.saveEntity(attacker);
            recorded.add(events.record(campaignId, state.getSceneId(), "READIED_ATTACK", attacker.getName(),
                    attacker.getName() + " usa su acción preparada contra " + mover.getName() + ".",
                    Map.of("entityId", attacker.getId(), "targetId", mover.getId())));
            recorded.addAll(attack(campaignId, state, attacker, mover, melee,
                    com.dndmasters.engine.rules.Advantage.NONE));
        }
        return recorded;
    }

    /**
     * Finds a living, conscious player with the Protection style within 5 ft of the target.
     * The protector cannot be the attacker or the target; the lowest entity id wins when
     * several allies qualify, which keeps the automatic choice deterministic.
     */
    private GameEntity findProtector(CombatState state, GameEntity attacker, GameEntity target) {
        if (state.getSceneId() == null || target.getKind() != EntityKind.PLAYER) {
            return null;
        }
        return campaigns.entitiesInScene(state.getSceneId()).stream()
                .filter(e -> e.getKind() == EntityKind.PLAYER)
                .filter(e -> !java.util.Objects.equals(e.getId(), target.getId()))
                .filter(e -> !java.util.Objects.equals(e.getId(), attacker.getId()))
                .filter(GameEntity::isAlive)
                .filter(e -> !Conditions.of(e).incapacitated())
                .filter(e -> !TurnEconomy.flag(e.getData(), "reactionUsed"))
                .filter(e -> e.cell().chebyshevTo(target.cell()) <= 1)
                .filter(e -> FightingStyleRules.hasProtection(
                        e.getData().get("fightingStyle") instanceof String value ? value : ""))
                .min(Comparator.comparingLong(GameEntity::getId))
                .orElse(null);
    }

    /**
     * Consumes a pending Help flag. Advantage applies only when the attack target is within 5 ft
     * of the helper, as the PHB requires; the flag is spent either way (next attack).
     */
    private boolean consumeHelpAdvantage(GameEntity attacker, GameEntity target) {
        if (!Boolean.TRUE.equals(attacker.getData().get("helpAdvantage"))) {
            return false;
        }
        boolean advantage = false;
        Object helperId = attacker.getData().get("helpedBy");
        if (helperId instanceof Number number) {
            GameEntity helper = campaigns.entity(number.longValue()).orElse(null);
            advantage = helper != null && helper.isAlive()
                    && helper.cell().chebyshevTo(target.cell()) <= 1;
        }
        attacker.getData().put("helpAdvantage", false);
        attacker.getData().remove("helpedBy");
        campaigns.saveEntity(attacker);
        return advantage;
    }

    public boolean isFinished(CombatState state) {
        return turns.isFinished(state);
    }

    public boolean isVictory(CombatState state) {
        return state.aliveEnemies() == 0;
    }

    public boolean isPlayerControlledEntity(GameEntity entity) {
        return isPlayerControlled(entity);
    }

    private boolean isPlayerControlled(GameEntity entity) {
        return "PLAYER".equals(entity.getData().get("controller"));
    }

    private boolean isEnemy(GameEntity entity) {
        return entity.getKind() == EntityKind.ENEMY;
    }
}
