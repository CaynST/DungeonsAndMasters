package com.dndmasters.engine.combat;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ConditionSetTest {

    @Test
    void parsesMapsAndLegacyStringCodes() {
        Map<String, Object> data = new HashMap<>();
        data.put("conditions", List.of(
                Map.of("code", "POISONED", "rounds", 2),
                "PRONE"));

        ConditionSet set = ConditionSet.from(data);

        assertThat(set.has(Condition.POISONED)).isTrue();
        assertThat(set.get(Condition.POISONED).rounds()).isEqualTo(2);
        assertThat(set.has(Condition.PRONE)).isTrue();
    }

    @Test
    void writeToRoundTripsCodesAndMetadata() {
        ConditionSet set = ConditionSet.empty()
                .with(new ActiveCondition(Condition.PARALYZED, 7L, 11, "CON", 3, 0))
                .with(new ActiveCondition(Condition.EXHAUSTION, null, null, null, null, 2));

        Map<String, Object> data = new HashMap<>();
        set.writeTo(data);
        ConditionSet back = ConditionSet.from(data);

        assertThat(back.codes()).containsExactlyInAnyOrder("PARALYZED", "EXHAUSTION");
        assertThat(back.get(Condition.PARALYZED).sourceId()).isEqualTo(7L);
        assertThat(back.get(Condition.PARALYZED).saveDc()).isEqualTo(11);
        assertThat(back.get(Condition.PARALYZED).rounds()).isEqualTo(3);
        assertThat(back.exhaustionLevel()).isEqualTo(2);
    }

    @Test
    void tickExpiresFiniteDurationsButKeepsIndefinite() {
        ConditionSet set = ConditionSet.empty()
                .with(new ActiveCondition(Condition.POISONED, null, null, null, 1, 0))
                .with(new ActiveCondition(Condition.PRONE, null, null, null, null, 0));

        ConditionSet after = set.tick();

        assertThat(after.has(Condition.POISONED)).isFalse();
        assertThat(after.has(Condition.PRONE)).isTrue();
    }

    @Test
    void withoutRemovesOnlyRequestedCondition() {
        ConditionSet set = ConditionSet.empty()
                .with(ActiveCondition.of(Condition.PRONE))
                .with(ActiveCondition.of(Condition.POISONED));

        ConditionSet after = set.without(Condition.PRONE);

        assertThat(after.has(Condition.PRONE)).isFalse();
        assertThat(after.has(Condition.POISONED)).isTrue();
    }

    @Test
    void derivesRuleQueriesFromActiveConditions() {
        ConditionSet set = ConditionSet.empty()
                .with(ActiveCondition.of(Condition.PARALYZED))
                .with(new ActiveCondition(Condition.EXHAUSTION, null, null, null, null, 2));

        assertThat(set.incapacitated()).isTrue();
        assertThat(set.speedZero()).isTrue();
        assertThat(set.autoFailsStrDexSaves()).isTrue();
        assertThat(set.meleeCritAgainst()).isTrue();
        assertThat(set.exhaustionLevel()).isEqualTo(2);
    }
}
