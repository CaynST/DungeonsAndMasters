package com.dndmasters.engine.combat;

/**
 * Death and dying rules (PHB): a creature at 0 HP is dying and makes a death saving throw
 * at the start of each of its turns. 3 successes stabilise it; 3 failures kill it. A natural
 * 20 restores 1 HP; a natural 1 counts as two failures. Damage at 0 HP adds a failure (two on
 * a critical), and damage equal to or greater than max HP kills instantly.
 */
public final class DeathSaveRules {

    private DeathSaveRules() {
    }

    public record State(
            boolean dying,
            boolean stable,
            int successes,
            int failures,
            boolean dead,
            int hpAfter) {

        public static State healthy() {
            return new State(false, false, 0, 0, false, 0);
        }

        public static State startDying() {
            return new State(true, false, 0, 0, false, 0);
        }

        public static State deadState() {
            return new State(false, false, 3, 3, true, 0);
        }
    }

    /** Resolves one death saving throw (d20 face). */
    public static State roll(State state, int d20) {
        if (d20 == 20) {
            // Regain 1 HP and become conscious.
            return new State(false, false, 0, 0, false, 1);
        }
        int failures = state.failures() + (d20 == 1 ? 2 : (d20 >= 10 ? 0 : 1));
        int successes = state.successes() + (d20 >= 10 ? 1 : 0);

        if (failures >= 3) {
            return new State(false, false, successes, failures, true, 0);
        }
        if (successes >= 3) {
            return new State(false, true, successes, failures, false, 0);
        }
        return new State(true, false, successes, failures, false, 0);
    }

    /** Damage taken while already dying. */
    public static State onDamage(State state, int damage, int maxHp, boolean critical) {
        if (damage >= maxHp) {
            return State.deadState();
        }
        int failures = state.failures() + (critical ? 2 : 1);
        if (failures >= 3) {
            return new State(false, false, state.successes(), failures, true, 0);
        }
        return new State(true, false, state.successes(), failures, false, 0);
    }
}
