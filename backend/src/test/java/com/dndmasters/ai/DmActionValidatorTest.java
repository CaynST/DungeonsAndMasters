package com.dndmasters.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.dndmasters.ai.dm.DmAction;
import com.dndmasters.ai.dm.DmActionValidator;
import com.dndmasters.ai.dm.DmContext;
import com.dndmasters.ai.dm.DmResponse;
import org.junit.jupiter.api.Test;

class DmActionValidatorTest {

    private final DmActionValidator validator = new DmActionValidator();

    @Test
    void acceptsAndNormalisesKnownConditions() {
        DmResponse raw = new DmResponse("n", java.util.List.of(
                DmAction.applyCondition(7L, "poisoned", 2)), false, null);

        DmResponse clean = validator.sanitize(raw, new DmContext());

        assertThat(clean.actions()).hasSize(1);
        assertThat(clean.actions().get(0).type()).isEqualTo("APPLY_CONDITION");
        assertThat(clean.actions().get(0).condition()).isEqualTo("POISONED");
        assertThat(clean.actions().get(0).entityId()).isEqualTo(7L);
        assertThat(clean.actions().get(0).rounds()).isEqualTo(2);
    }

    @Test
    void dropsUnknownConditionsAndMissingTargets() {
        DmResponse unknown = new DmResponse("n", java.util.List.of(
                DmAction.applyCondition(7L, "banana", 2)), false, null);
        DmResponse noTarget = new DmResponse("n", java.util.List.of(
                DmAction.applyCondition(null, "POISONED", 2)), false, null);

        assertThat(validator.sanitize(unknown, new DmContext()).actions()).isEmpty();
        assertThat(validator.sanitize(noTarget, new DmContext()).actions()).isEmpty();
    }

    @Test
    void clampsConditionRounds() {
        DmResponse raw = new DmResponse("n", java.util.List.of(
                DmAction.applyCondition(7L, "PRONE", 999)), false, null);

        DmResponse clean = validator.sanitize(raw, new DmContext());

        assertThat(clean.actions().get(0).rounds()).isLessThanOrEqualTo(20);
    }

    @Test
    void acceptsRemovingAKnownCondition() {
        DmResponse raw = new DmResponse("n", java.util.List.of(
                DmAction.removeCondition(7L, "prone")), false, null);

        DmResponse clean = validator.sanitize(raw, new DmContext());

        assertThat(clean.actions()).hasSize(1);
        assertThat(clean.actions().get(0).condition()).isEqualTo("PRONE");
    }
}
