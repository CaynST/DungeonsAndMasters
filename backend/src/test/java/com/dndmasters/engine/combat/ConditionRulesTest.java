package com.dndmasters.engine.combat;

import static org.assertj.core.api.Assertions.assertThat;

import com.dndmasters.engine.rules.Advantage;
import org.junit.jupiter.api.Test;

class ConditionRulesTest {

    private static ConditionSet set(Condition... conditions) {
        ConditionSet result = ConditionSet.empty();
        for (Condition condition : conditions) {
            result = result.with(ActiveCondition.of(condition));
        }
        return result;
    }

    private static ConditionSet exhaustion(int level) {
        return ConditionSet.empty()
                .with(new ActiveCondition(Condition.EXHAUSTION, null, null, null, null, level));
    }

    @Test
    void blindedAttackerHasDisadvantage() {
        assertThat(ConditionRules.attackAdvantage(set(Condition.BLINDED), ConditionSet.empty(), true, false))
                .isEqualTo(Advantage.DISADVANTAGE);
    }

    @Test
    void invisibleAttackerHasAdvantage() {
        assertThat(ConditionRules.attackAdvantage(set(Condition.INVISIBLE), ConditionSet.empty(), true, false))
                .isEqualTo(Advantage.ADVANTAGE);
    }

    @Test
    void proneTargetGivesAdvantageToMeleeAndDisadvantageToRanged() {
        ConditionSet prone = set(Condition.PRONE);
        assertThat(ConditionRules.attackAdvantage(ConditionSet.empty(), prone, true, false))
                .isEqualTo(Advantage.ADVANTAGE);
        assertThat(ConditionRules.attackAdvantage(ConditionSet.empty(), prone, false, false))
                .isEqualTo(Advantage.DISADVANTAGE);
    }

    @Test
    void advantageAndDisadvantageCancel() {
        assertThat(ConditionRules.attackAdvantage(set(Condition.BLINDED), set(Condition.PRONE), true, false))
                .isEqualTo(Advantage.NONE);
    }

    @Test
    void dodgingTargetImposesDisadvantage() {
        assertThat(ConditionRules.attackAdvantage(ConditionSet.empty(), ConditionSet.empty(), true, true))
                .isEqualTo(Advantage.DISADVANTAGE);
    }

    @Test
    void paralyzedAndUnconsciousTargetsAreAutoCritOnlyInMelee() {
        assertThat(ConditionRules.automaticCritical(set(Condition.PARALYZED), true)).isTrue();
        assertThat(ConditionRules.automaticCritical(set(Condition.UNCONSCIOUS), true)).isTrue();
        assertThat(ConditionRules.automaticCritical(set(Condition.PARALYZED), false)).isFalse();
        assertThat(ConditionRules.automaticCritical(set(Condition.STUNNED), true)).isFalse();
    }

    @Test
    void restrainedTargetGrantsAdvantageAndRestrainedAttackerHasDisadvantage() {
        assertThat(ConditionRules.attackAdvantage(ConditionSet.empty(), set(Condition.RESTRAINED), true, false))
                .isEqualTo(Advantage.ADVANTAGE);
        assertThat(ConditionRules.attackAdvantage(set(Condition.RESTRAINED), ConditionSet.empty(), true, false))
                .isEqualTo(Advantage.DISADVANTAGE);
    }

    @Test
    void poisonedAndFrightenedImposeCheckDisadvantage() {
        assertThat(ConditionRules.checkAdvantage(set(Condition.POISONED), "perception"))
                .isEqualTo(Advantage.DISADVANTAGE);
        assertThat(ConditionRules.checkAdvantage(set(Condition.FRIGHTENED), "athletics"))
                .isEqualTo(Advantage.DISADVANTAGE);
        assertThat(ConditionRules.checkAdvantage(set(Condition.DEAFENED), "perception"))
                .isEqualTo(Advantage.DISADVANTAGE);
        assertThat(ConditionRules.checkAdvantage(set(Condition.INVISIBLE), "stealth"))
                .isEqualTo(Advantage.ADVANTAGE);
        assertThat(ConditionRules.checkAdvantage(ConditionSet.empty(), "perception"))
                .isEqualTo(Advantage.NONE);
    }

    @Test
    void exhaustionPenaltyIsMinusTwoPerLevel() {
        assertThat(ConditionRules.checkPenalty(exhaustion(2))).isEqualTo(-4);
        assertThat(ConditionRules.checkPenalty(ConditionSet.empty())).isZero();
    }

    @Test
    void effectiveSpeedRespectsExhaustionGrappleAndProne() {
        assertThat(ConditionRules.effectiveSpeed(6, ConditionSet.empty())).isEqualTo(6);
        assertThat(ConditionRules.effectiveSpeed(6, exhaustion(2))).isEqualTo(3);
        assertThat(ConditionRules.effectiveSpeed(6, exhaustion(5))).isZero();
        assertThat(ConditionRules.effectiveSpeed(6, set(Condition.GRAPPLED))).isZero();
        assertThat(ConditionRules.effectiveSpeed(6, set(Condition.PRONE))).isEqualTo(3);
    }
}
