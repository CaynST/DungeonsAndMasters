package com.dndmasters.engine;

import com.dndmasters.engine.board.MovementValidator;
import com.dndmasters.engine.combat.CombatEngine;
import com.dndmasters.engine.combat.TurnManager;
import com.dndmasters.engine.dice.DiceEngine;
import com.dndmasters.engine.rules.AttackResolver;
import com.dndmasters.engine.rules.CheckResolver;
import com.dndmasters.engine.rules.DamageResolver;
import com.dndmasters.engine.rules.InitiativeEngine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Wires the framework-free rules engine as Spring beans. */
@Configuration
public class EngineConfig {

    @Bean
    public DiceEngine diceEngine() {
        return new DiceEngine();
    }

    @Bean
    public CheckResolver checkResolver(DiceEngine diceEngine) {
        return new CheckResolver(diceEngine);
    }

    @Bean
    public AttackResolver attackResolver(DiceEngine diceEngine) {
        return new AttackResolver(diceEngine);
    }

    @Bean
    public DamageResolver damageResolver(DiceEngine diceEngine) {
        return new DamageResolver(diceEngine);
    }

    @Bean
    public InitiativeEngine initiativeEngine(DiceEngine diceEngine) {
        return new InitiativeEngine(diceEngine);
    }

    @Bean
    public TurnManager turnManager() {
        return new TurnManager();
    }

    @Bean
    public CombatEngine combatEngine(InitiativeEngine initiativeEngine, TurnManager turnManager) {
        return new CombatEngine(initiativeEngine, turnManager);
    }

    @Bean
    public MovementValidator movementValidator() {
        return new MovementValidator();
    }
}
