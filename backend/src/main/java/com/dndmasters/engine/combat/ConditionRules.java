package com.dndmasters.engine.combat;

import com.dndmasters.domain.Ability;
import com.dndmasters.engine.rules.Advantage;
import com.dndmasters.engine.rules.Skills;

/**
 * Pure 5e condition math. Advantage and disadvantage cancel each other, as the PHB states.
 * The engine applies these decisions; conditions never bypass the dice resolver.
 */
public final class ConditionRules {

    private ConditionRules() {
    }

    /**
     * Combined attack modifier from both creatures' conditions plus Dodge.
     *
     * @param melee true for melee attacks (prone targets grant advantage only in melee)
     */
    public static Advantage attackAdvantage(ConditionSet attacker, ConditionSet target,
                                            boolean melee, boolean targetDodging) {
        boolean advantage = attacker.has(Condition.INVISIBLE)
                || target.grantsAdvantageToAttackers()
                || (target.has(Condition.PRONE) && melee);
        boolean disadvantage = attacker.attackerDisadvantage()
                || attacker.exhaustionLevel() >= 3
                || target.has(Condition.INVISIBLE)
                || (target.has(Condition.PRONE) && !melee)
                || targetDodging;
        if (advantage && disadvantage) {
            return Advantage.NONE;
        }
        if (advantage) {
            return Advantage.ADVANTAGE;
        }
        return disadvantage ? Advantage.DISADVANTAGE : Advantage.NONE;
    }

    /** Melee hits against paralysed or unconscious creatures are critical hits. */
    public static boolean automaticCritical(ConditionSet target, boolean melee) {
        return melee && target.meleeCritAgainst();
    }

    public static Advantage checkAdvantage(ConditionSet conditions, String skill) {
        Ability ability = Skills.abilityFor(skill);
        boolean advantage = conditions.has(Condition.INVISIBLE) && "stealth".equalsIgnoreCase(skill);
        boolean disadvantage = conditions.has(Condition.FRIGHTENED)
                || conditions.has(Condition.POISONED)
                || (conditions.has(Condition.DEAFENED) && "perception".equalsIgnoreCase(skill))
                || (conditions.has(Condition.RESTRAINED) && ability == Ability.DEXTERITY);
        if (advantage && disadvantage) {
            return Advantage.NONE;
        }
        if (advantage) {
            return Advantage.ADVANTAGE;
        }
        return disadvantage ? Advantage.DISADVANTAGE : Advantage.NONE;
    }

    /** Exhaustion imposes -2 per level on d20 tests (ability checks). */
    public static int checkPenalty(ConditionSet conditions) {
        return -2 * conditions.exhaustionLevel();
    }

    /**
     * Effective walking speed. Grappled/restrained/paralysed/... are 0, exhaustion 5+ is 0,
     * exhaustion 2+ halves speed and prone creatures can only crawl at half speed.
     */
    public static int effectiveSpeed(int baseSpeed, ConditionSet conditions) {
        if (conditions.speedZero() || conditions.exhaustionLevel() >= 5) {
            return 0;
        }
        if (conditions.exhaustionLevel() >= 2) {
            return Math.max(0, baseSpeed / 2);
        }
        if (conditions.has(Condition.PRONE)) {
            return Math.max(1, baseSpeed / 2);
        }
        return baseSpeed;
    }
}
