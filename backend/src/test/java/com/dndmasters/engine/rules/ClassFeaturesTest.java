package com.dndmasters.engine.rules;

import static org.assertj.core.api.Assertions.assertThat;

import com.dndmasters.domain.GameEntity;
import org.junit.jupiter.api.Test;

class ClassFeaturesTest {

    private static GameEntity entity(String cls) {
        GameEntity entity = new GameEntity();
        entity.getData().put("class", cls);
        return entity;
    }

    @Test
    void mapsEnglishAndSpanishClassNames() {
        assertThat(ClassFeatures.idOf("Fighter")).isEqualTo("fighter");
        assertThat(ClassFeatures.idOf("Guerrero")).isEqualTo("fighter");
        assertThat(ClassFeatures.idOf("Bárbaro")).isEqualTo("barbarian");
        assertThat(ClassFeatures.idOf("Pícaro")).isEqualTo("rogue");
        assertThat(ClassFeatures.idOf("Mago")).isEqualTo("wizard");
        assertThat(ClassFeatures.idOf("Clérigo")).isEqualTo("cleric");
        assertThat(ClassFeatures.idOf("banana")).isNull();
    }

    @Test
    void explicitClassIdWinsOverDisplayName() {
        GameEntity entity = entity("Fighter");
        entity.getData().put("classId", "rogue");

        assertThat(ClassFeatures.idOf(entity)).isEqualTo("rogue");
    }

    @Test
    void fallsBackToDisplayNameAndDefaultsLevelToOne() {
        GameEntity entity = entity("Bárbaro");

        assertThat(ClassFeatures.idOf(entity)).isEqualTo("barbarian");
        assertThat(ClassFeatures.level(entity)).isEqualTo(1);
        entity.getData().put("level", 3);
        assertThat(ClassFeatures.level(entity)).isEqualTo(3);
    }
}
