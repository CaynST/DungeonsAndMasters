package com.dndmasters.content;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** The DM can name a monster loosely; lookup must be case-insensitive and never throw. */
class BestiaryTest {

    @Test
    void findsMonstersByDisplayNameIgnoringCaseAndSurroundingSpaces() {
        assertThat(Bestiary.findByName("goblin")).get()
                .extracting(Bestiary.Monster::id).isEqualTo("goblin");
        assertThat(Bestiary.findByName("  OGRO ")).get()
                .extracting(Bestiary.Monster::id).isEqualTo("ogre");
        assertThat(Bestiary.findByName("Oso Búho")).get()
                .extracting(Bestiary.Monster::id).isEqualTo("owlbear");
    }

    @Test
    void alsoAcceptsTheStableMonsterId() {
        assertThat(Bestiary.findByName("goblin-archer")).get()
                .extracting(Bestiary.Monster::name).isEqualTo("Goblin arquero");
    }

    @Test
    void returnsEmptyForUnknownBlankOrNullNames() {
        assertThat(Bestiary.findByName("dragón de humo")).isEmpty();
        assertThat(Bestiary.findByName("   ")).isEmpty();
        assertThat(Bestiary.findByName(null)).isEmpty();
    }
}
