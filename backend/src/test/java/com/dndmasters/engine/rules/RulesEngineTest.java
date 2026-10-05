package com.dndmasters.engine.rules;

import static org.assertj.core.api.Assertions.assertThat;

import com.dndmasters.domain.Ability;
import com.dndmasters.domain.Attack;
import com.dndmasters.domain.Stats;
import com.dndmasters.engine.dice.DiceEngine;
import com.dndmasters.testsupport.StubRandom;
import org.junit.jupiter.api.Test;

class RulesEngineTest {

    private static Combatant combatant(long id, int hp, int ac, Stats stats) {
        return new Combatant(id, "C" + id, "PLAYER", hp, hp, ac, stats, true);
    }

    @Test
    void modifierTableMatches5e() {
        assertThat(Modifier.of(8)).isEqualTo(-1);
        assertThat(Modifier.of(9)).isEqualTo(-1);
        assertThat(Modifier.of(10)).isEqualTo(0);
        assertThat(Modifier.of(11)).isEqualTo(0);
        assertThat(Modifier.of(12)).isEqualTo(1);
        assertThat(Modifier.of(14)).isEqualTo(2);
        assertThat(Modifier.of(20)).isEqualTo(5);
    }

    @Test
    void checkSucceedsAtBoundaryAndFailsJustBelow() {
        Stats stats = new Stats(10, 14, 10, 10, 10, 10); // DEX +2
        Combatant c = combatant(1, 10, 10, stats);
        CheckResolver resolver = new CheckResolver(new DiceEngine(StubRandom.of(11))); // d20 = 12

        CheckResult success = resolver.resolve(c, "stealth", 14); // 12 + 2 = 14
        assertThat(success.total()).isEqualTo(14);
        assertThat(success.success()).isTrue();

        CheckResult failure = resolver.resolve(c, "stealth", 15);
        assertThat(failure.success()).isFalse();
    }

    @Test
    void checkNaturalTwentyAlwaysSucceedsAndNaturalOneAlwaysFails() {
        Combatant c = combatant(1, 10, 10, new Stats(10, 10, 10, 10, 10, 10));
        CheckResolver resolver = new CheckResolver(new DiceEngine(StubRandom.of(19, 0)));

        CheckResult crit = resolver.resolve(c, "arcana", 30);
        assertThat(crit.success()).isTrue();
        assertThat(crit.natural20()).isTrue();

        CheckResult fumble = resolver.resolve(c, "arcana", 1);
        assertThat(fumble.success()).isFalse();
        assertThat(fumble.natural1()).isTrue();
    }

    @Test
    void attackHitsAtBoundaryAndCritsOnNaturalTwenty() {
        Combatant target = combatant(2, 20, 15, new Stats(10, 10, 10, 10, 10, 10));
        Attack attack = Attack.melee("Longsword", 5, "1d8+3");

        AttackResolver miss = new AttackResolver(new DiceEngine(StubRandom.of(8))); // face 9, 9+5=14
        assertThat(miss.resolve(combatant(1, 20, 15, new Stats(10, 10, 10, 10, 10, 10)), target, attack).hit()).isFalse();

        AttackResolver hit = new AttackResolver(new DiceEngine(StubRandom.of(9))); // face 10, 10+5=15
        assertThat(hit.resolve(combatant(1, 20, 15, new Stats(10, 10, 10, 10, 10, 10)), target, attack).hit()).isTrue();

        AttackResolver crit = new AttackResolver(new DiceEngine(StubRandom.of(19)));
        AttackResult critResult = crit.resolve(combatant(1, 20, 15, new Stats(10, 10, 10, 10, 10, 10)), target, attack);
        assertThat(critResult.hit()).isTrue();
        assertThat(critResult.critical()).isTrue();
    }

    @Test
    void naturalOneMissesEvenAgainstLowAc() {
        Combatant target = combatant(2, 20, 1, new Stats(10, 10, 10, 10, 10, 10));
        AttackResolver resolver = new AttackResolver(new DiceEngine(StubRandom.of(0)));
        AttackResult result = resolver.resolve(combatant(1, 20, 15, new Stats(10, 10, 10, 10, 10, 10)), target, Attack.melee("Axe", 10, "1d6"));
        assertThat(result.fumble()).isTrue();
        assertThat(result.hit()).isFalse();
    }

    @Test
    void criticalDoublesDamageDiceNotModifier() {
        Combatant target = combatant(2, 30, 10, new Stats(10, 10, 10, 10, 10, 10));
        Attack attack = Attack.melee("Longsword", 5, "1d8+3");
        // attack roll = 20, then damage d8 = 4
        DiceEngine dice = new DiceEngine(StubRandom.of(19, 3));
        DamageResolver resolver = new DamageResolver(dice);
        AttackResult attackResult = new AttackResolver(dice).resolve(combatant(1, 30, 10, new Stats(10, 10, 10, 10, 10, 10)), target, attack);

        DamageResult damage = resolver.apply(attack, attackResult, target);
        assertThat(attackResult.critical()).isTrue();
        assertThat(damage.diceTotal()).isEqualTo(8); // 4 * 2
        assertThat(damage.total()).isEqualTo(11); // 8 + 3
        assertThat(damage.targetHpAfter()).isEqualTo(19);
    }

    @Test
    void damageNeverDropsBelowZeroAndMarksDead() {
        Combatant target = combatant(2, 5, 10, new Stats(10, 10, 10, 10, 10, 10));
        Attack attack = Attack.melee("Maul", 5, "1d8+3");
        DiceEngine dice = new DiceEngine(StubRandom.of(10, 7)); // hit, d8=8 → 11 dmg
        AttackResult attackResult = new AttackResolver(dice).resolve(combatant(1, 5, 10, new Stats(10, 10, 10, 10, 10, 10)), target, attack);

        DamageResult damage = new DamageResolver(dice).apply(attack, attackResult, target);
        assertThat(damage.targetHpAfter()).isZero();
        assertThat(damage.dead()).isTrue();
    }

    @Test
    void initiativeOrdersByTotalThenDexThenId() {
        // combatants rolled in list order: 10, 5, 10
        DiceEngine dice = new DiceEngine(StubRandom.of(10, 5, 9));
        InitiativeEngine engine = new InitiativeEngine(dice);
        Combatant fast = combatant(1, 10, 10, new Stats(10, 12, 10, 10, 10, 10)); // die 11 + 1 = 12
        Combatant slow = combatant(3, 10, 10, new Stats(10, 14, 10, 10, 10, 10)); // die 6 + 2 = 8
        Combatant tie = combatant(2, 10, 10, new Stats(10, 10, 10, 10, 10, 10)); // die 10 + 0 = 10

        var order = engine.order(java.util.List.of(fast, slow, tie));
        assertThat(order).extracting(InitiativeEntry::entityId).containsExactly(1L, 2L, 3L);
        assertThat(order).extracting(InitiativeEntry::orderIndex).containsExactly(0, 1, 2);
    }
}
