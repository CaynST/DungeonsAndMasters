package com.dndmasters.engine.combat;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TurnEconomyTest {

    @Test
    void resetClearsUsageAndRestoresMovement() {
        Map<String, Object> data = new HashMap<>();
        data.put("actionUsed", true);
        data.put("bonusUsed", true);
        data.put("movementUsed", 5);
        data.put("dodging", true);
        data.put("disengaged", true);
        data.put("reactionUsed", true);

        TurnEconomy.reset(data, 6);

        assertThat(TurnEconomy.flag(data, "actionUsed")).isFalse();
        assertThat(TurnEconomy.flag(data, "bonusUsed")).isFalse();
        assertThat(TurnEconomy.flag(data, "dodging")).isFalse();
        assertThat(TurnEconomy.flag(data, "disengaged")).isFalse();
        assertThat(TurnEconomy.flag(data, "reactionUsed")).isFalse();
        assertThat(TurnEconomy.movementUsed(data)).isZero();
        assertThat(TurnEconomy.movementAllowance(data, 6)).isEqualTo(6);
    }

    @Test
    void allowanceFallsBackToSpeedWithoutDash() {
        assertThat(TurnEconomy.movementAllowance(new HashMap<>(), 6)).isEqualTo(6);
    }

    @Test
    void allowanceUsesDashValue() {
        Map<String, Object> data = new HashMap<>();
        data.put("movementMax", 12);
        assertThat(TurnEconomy.movementAllowance(data, 6)).isEqualTo(12);
    }
}
