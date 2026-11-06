package com.dndmasters.domain;

public record Attack(
        String name,
        int attackBonus,
        String damageExpression,
        int range,
        boolean ranged,
        boolean twoHanded) {

    public static Attack melee(String name, int attackBonus, String damageExpression) {
        return new Attack(name, attackBonus, damageExpression, 1, false, false);
    }

    public static Attack meleeTwoHanded(String name, int attackBonus, String damageExpression) {
        return new Attack(name, attackBonus, damageExpression, 1, false, true);
    }

    public static Attack ranged(String name, int attackBonus, String damageExpression, int range) {
        return new Attack(name, attackBonus, damageExpression, range, true, false);
    }

    /** Returns a copy with an extra attack bonus (Bless, Archery, etc.). */
    public Attack withAttackBonus(int bonus) {
        return new Attack(name, attackBonus + bonus, damageExpression, range, ranged, twoHanded);
    }
}
