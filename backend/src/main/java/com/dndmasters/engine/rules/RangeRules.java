package com.dndmasters.engine.rules;

import com.dndmasters.domain.Attack;

/**
 * Distance rules for attacks (PHB): melee attacks reach 1 square; ranged attacks have a
 * normal range and a long range (up to twice the normal) at disadvantage. Beyond long
 * range the attack is impossible.
 */
public final class RangeRules {

    private RangeRules() {
    }

    public record Result(boolean allowed, boolean disadvantage, String reason) {
        public static Result ok(boolean disadvantage) {
            return new Result(true, disadvantage, null);
        }

        public static Result rejected(String reason) {
            return new Result(false, false, reason);
        }
    }

    /** @param distance distance in grid squares (5 ft each) */
    public static Result check(Attack attack, int distance) {
        int d = Math.max(0, distance);
        if (!attack.ranged()) {
            if (d <= 1) {
                return Result.ok(false);
            }
            return Result.rejected("El objetivo está fuera de tu alcance cuerpo a cuerpo ("
                    + d + " casillas; alcance 1).");
        }
        int normal = Math.max(1, attack.range());
        if (d <= normal) {
            return Result.ok(false);
        }
        if (d <= normal * 2) {
            return Result.ok(true);
        }
        return Result.rejected("El objetivo está fuera de alcance (" + d + " casillas; máximo "
                + (normal * 2) + ").");
    }
}
