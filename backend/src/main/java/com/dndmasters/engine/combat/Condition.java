package com.dndmasters.engine.combat;

import java.util.Locale;

/**
 * D&D 5e conditions (PHB Appendix A). Codes are the stable English identifiers used in
 * persisted data and DM actions; labels and descriptions are shown to players in Spanish.
 */
public enum Condition {

    BLINDED("Cegado", "No puede ver. Sus ataques tienen desventaja y los ataques contra él, ventaja."),
    CHARMED("Encantado", "No puede atacar a quien lo encantó."),
    DEAFENED("Sordo", "No puede oír."),
    EXHAUSTION("Agotamiento", "Cada nivel impone -2 a las pruebas de d20; el nivel 2 reduce la velocidad a la mitad."),
    FRIGHTENED("Asustado", "Desventaja en pruebas y ataques mientras vea la fuente del miedo."),
    GRAPPLED("Agarrado", "Su velocidad es 0."),
    INCAPACITATED("Incapacitado", "No puede realizar acciones ni reacciones."),
    INVISIBLE("Invisible", "Los ataques contra él tienen desventaja y los suyos, ventaja."),
    PARALYZED("Paralizado",
            "Incapacitado y sin movimiento; falla salvaciones de FUE/DES; los ataques contra él tienen ventaja "
                    + "y los cuerpo a cuerpo son críticos."),
    PETRIFIED("Petrificado",
            "Incapacitado y sin movimiento; falla salvaciones de FUE/DES; resistencia a todo daño."),
    POISONED("Envenenado", "Desventaja en ataques y pruebas."),
    PRONE("Derribado",
            "Solo puede arrastrarse; ataques cuerpo a cuerpo contra él con ventaja, a distancia con desventaja; "
                    + "sus ataques con desventaja."),
    RESTRAINED("Apresado",
            "Velocidad 0; ataques contra él con ventaja; sus ataques y salvaciones de DES con desventaja."),
    STUNNED("Aturdido",
            "Incapacitado y sin movimiento; falla salvaciones de FUE/DES; ataques contra él con ventaja."),
    UNCONSCIOUS("Inconsciente",
            "Incapacitado, derribado y sin movimiento; falla salvaciones de FUE/DES; ataques contra él con ventaja "
                    + "y cuerpo a cuerpo críticos.");

    private final String label;
    private final String description;

    Condition(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String label() {
        return label;
    }

    public String description() {
        return description;
    }

    /** Resolves a case-insensitive code; returns null for unknown values. */
    public static Condition fromCode(String code) {
        if (code == null) {
            return null;
        }
        try {
            return valueOf(code.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static boolean isKnown(String code) {
        return fromCode(code) != null;
    }

    /** Incapacitated, paralysed, petrified, stunned or unconscious creatures cannot act. */
    public boolean incapacitating() {
        return this == INCAPACITATED || this == PARALYZED || this == PETRIFIED
                || this == STUNNED || this == UNCONSCIOUS;
    }

    /** Conditions that force speed to 0. Prone is handled separately (crawling). */
    public boolean speedZero() {
        return this == GRAPPLED || this == PARALYZED || this == PETRIFIED
                || this == RESTRAINED || this == STUNNED || this == UNCONSCIOUS;
    }

    /** Paralyzed, petrified, stunned and unconscious creatures auto-fail STR/DEX saves. */
    public boolean autoFailsStrDexSaves() {
        return this == PARALYZED || this == PETRIFIED || this == STUNNED || this == UNCONSCIOUS;
    }

    /** Conditions that impose disadvantage on the creature's own attack rolls. */
    public boolean attackerDisadvantage() {
        return this == BLINDED || this == FRIGHTENED || this == POISONED || this == PRONE || this == RESTRAINED;
    }

    /** Conditions that let attackers roll with advantage against the creature. */
    public boolean grantsAdvantageToAttackers() {
        return this == BLINDED || this == PARALYZED || this == PETRIFIED
                || this == RESTRAINED || this == STUNNED || this == UNCONSCIOUS;
    }

    /** Paralyzed and unconscious creatures suffer critical hits from melee attacks. */
    public boolean meleeCritAgainst() {
        return this == PARALYZED || this == UNCONSCIOUS;
    }
}
