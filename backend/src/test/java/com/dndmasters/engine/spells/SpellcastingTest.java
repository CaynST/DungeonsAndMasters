package com.dndmasters.engine.spells;

import static org.assertj.core.api.Assertions.assertThat;

import com.dndmasters.domain.Ability;
import com.dndmasters.domain.GameEntity;
import com.dndmasters.domain.Stats;
import org.junit.jupiter.api.Test;

class SpellcastingTest {

    private static GameEntity caster(String cls, int level, int intelligence, int wisdom) {
        GameEntity entity = new GameEntity();
        entity.getData().put("class", cls);
        entity.getData().put("level", level);
        entity.setStats(new Stats(10, 10, 10, intelligence, wisdom, 10));
        return entity;
    }

    @Test
    void castingAbilityDependsOnClass() {
        assertThat(Spellcasting.abilityFor("wizard")).isEqualTo(Ability.INTELLIGENCE);
        assertThat(Spellcasting.abilityFor("cleric")).isEqualTo(Ability.WISDOM);
        assertThat(Spellcasting.abilityFor("fighter")).isNull();
    }

    @Test
    void attackBonusAndSaveDcUseProficiencyAndCastingModifier() {
        GameEntity wizard = caster("Wizard", 1, 16, 10); // INT +3, proficiency +2

        assertThat(Spellcasting.attackBonus(wizard, "wizard")).isEqualTo(5);
        assertThat(Spellcasting.saveDc(wizard, "wizard")).isEqualTo(13);
    }

    @Test
    void proficiencyScalesWithLevel() {
        assertThat(Spellcasting.proficiencyBonus(1)).isEqualTo(2);
        assertThat(Spellcasting.proficiencyBonus(5)).isEqualTo(3);
        assertThat(Spellcasting.proficiencyBonus(9)).isEqualTo(4);
    }

    @Test
    void slotsScaleWithCasterLevel() {
        assertThat(Spellcasting.maxSlots(1)).isEqualTo(2);
        assertThat(Spellcasting.maxSlots(2)).isEqualTo(3);
        assertThat(Spellcasting.maxSlots(3)).isEqualTo(4);
        assertThat(Spellcasting.maxSlots(0)).isZero();
    }
}
