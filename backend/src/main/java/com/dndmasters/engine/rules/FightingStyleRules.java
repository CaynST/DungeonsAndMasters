package com.dndmasters.engine.rules;

import java.util.Locale;
import java.util.Set;

/**
 * Fighting Style bonuses supported by the prototype: Defense, Archery, Dueling,
 * Great Weapon Fighting and Protection.
 */
public final class FightingStyleRules {

    public static final String DEFENSE = "defense";
    public static final String ARCHERY = "archery";
    public static final String DUELING = "dueling";
    public static final String GREAT_WEAPON = "great-weapon";
    public static final String PROTECTION = "protection";

    private static final Set<String> KNOWN =
            Set.of(DEFENSE, ARCHERY, DUELING, GREAT_WEAPON, PROTECTION);

    private FightingStyleRules() {
    }

    public static String normalize(String style) {
        return style == null ? null : style.trim().toLowerCase(Locale.ROOT);
    }

    public static boolean isKnown(String style) {
        String normalized = normalize(style);
        return normalized != null && KNOWN.contains(normalized);
    }

    /** Defense: +1 AC while wearing armor. */
    public static int defenseBonus(String style, boolean wearingArmor) {
        return DEFENSE.equals(normalize(style)) && wearingArmor ? 1 : 0;
    }

    /** Archery: +2 to ranged weapon attack rolls. */
    public static int archeryBonus(String style, boolean ranged) {
        return ARCHERY.equals(normalize(style)) && ranged ? 2 : 0;
    }

    /** Dueling: +2 damage with a one-handed melee weapon. */
    public static int duelingBonus(String style, boolean melee, boolean twoHanded) {
        return DUELING.equals(normalize(style)) && melee && !twoHanded ? 2 : 0;
    }

    /**
     * Great Weapon Fighting: reroll damage dice that show 1 or 2 when attacking with a
     * melee weapon wielded in two hands. The prototype models the two-handed property on
     * {@code Attack.twoHanded}; versatile-weapon switching is not modelled yet.
     */
    public static boolean rerollsLowDamageDice(String style, boolean melee, boolean twoHanded) {
        return GREAT_WEAPON.equals(normalize(style)) && melee && twoHanded;
    }

    /** Protection: reactive style that imposes disadvantage on attacks against nearby allies. */
    public static boolean hasProtection(String style) {
        return PROTECTION.equals(normalize(style));
    }

    public static String label(String style) {
        String normalized = normalize(style);
        if (DEFENSE.equals(normalized)) {
            return "Defensa";
        }
        if (ARCHERY.equals(normalized)) {
            return "Arquería";
        }
        if (DUELING.equals(normalized)) {
            return "Duelo";
        }
        if (GREAT_WEAPON.equals(normalized)) {
            return "Gran arma";
        }
        if (PROTECTION.equals(normalized)) {
            return "Protección";
        }
        return "Sin estilo";
    }
}
