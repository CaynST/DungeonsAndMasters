package com.dndmasters.domain;

public record Attack(
        String name,
        int attackBonus,
        String damageExpression,
        int range,
        boolean ranged) {

    public static Attack melee(String name, int attackBonus, String damageExpression) {
        return new Attack(name, attackBonus, damageExpression, 1, false);
    }

    public static Attack ranged(String name, int attackBonus, String damageExpression, int range) {
        return new Attack(name, attackBonus, damageExpression, range, true);
    }
}
