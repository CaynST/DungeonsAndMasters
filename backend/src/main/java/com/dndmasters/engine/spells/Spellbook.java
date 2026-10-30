package com.dndmasters.engine.spells;

import java.util.List;

/** The spells known by a class and its level-1 spell-slot pool. */
public record Spellbook(List<Spell> spells, int slots) {
}
