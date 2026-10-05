package com.dndmasters.engine.rules;

import com.dndmasters.domain.Ability;
import com.dndmasters.domain.Stats;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A minimal combat-ready view of a creature. The engine operates on this instead of
 * the persistence entities so rules stay framework-free.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Combatant {

    private long id;
    private String name;
    private String kind;
    private int hp;
    private int maxHp;
    private int ac;
    private Stats stats = new Stats(10, 10, 10, 10, 10, 10);
    private boolean alive = true;

    public int modifier(Ability ability) {
        return Modifier.of(stats.get(ability));
    }

    public int dexterityModifier() {
        return modifier(Ability.DEXTERITY);
    }

    public void applyDamage(int amount) {
        this.hp = Math.max(0, this.hp - amount);
        this.alive = this.hp > 0;
    }
}
