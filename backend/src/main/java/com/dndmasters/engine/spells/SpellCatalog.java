package com.dndmasters.engine.spells;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Static spell lists for the classes supported by the one-shot. */
public final class SpellCatalog {

    private static final List<Spell> WIZARD = List.of(
            Spell.attack("fire_bolt", "Fuego feérico", 0, "Evocación",
                    "Un rayo de fuego a distancia. Ataque de conjuro; 1d10 de daño de fuego.", 24, "1d10", "fire"),
            Spell.attack("ray_of_frost", "Rayo de escarcha", 0, "Evocación",
                    "Un rayo gélido. Ataque de conjuro; 1d8 de daño de frío.", 12, "1d8", "cold"),
            Spell.auto("magic_missile", "Proyectil mágico", 1, "Evocación",
                    "Tres dardos que impactan automáticamente; 3d4+3 de daño de fuerza.", 24, "3d4+3", "force"),
            Spell.area("thunderwave", "Onda de trueno", 1, "Evocación",
                    "Cubo de 15 pies centrado en ti: las criaturas a 2 casillas hacen salvación de "
                            + "Constitución; 2d8 de daño de trueno.", 2, "2d8", "thunder", "CONSTITUTION"));

    private static final List<Spell> CLERIC = List.of(
            Spell.save("sacred_flame", "Llama sagrada", 0, "Evocación",
                    "Llama descendente. El objetivo hace una salvación de Destreza; 1d8 de daño radiante.", 12,
                    "1d8", "radiant", "DEXTERITY"),
            Spell.heal("cure_wounds", "Curar heridas", 1, "Evocación",
                    "Toque curativo: 1d8 + modificador de Sabiduría PG.", 1, "1d8", false),
            Spell.heal("healing_word", "Palabra curativa", 1, "Evocación",
                    "Palabra a distancia como acción adicional: 1d4 + modificador de Sabiduría PG.", 12, "1d4", true),
            Spell.buff("bless", "Bendición", 1, "Encantamiento",
                    "Concentración: el objetivo suma 1d4 a sus tiradas de ataque y salvaciones durante 10 rondas.",
                    6, "bless", false, true));

    private static final Map<String, Spellbook> BOOKS = Map.of(
            "wizard", new Spellbook(WIZARD, 2),
            "cleric", new Spellbook(CLERIC, 2));

    private static final Spellbook EMPTY = new Spellbook(List.of(), 0);

    private SpellCatalog() {
    }

    public static Spellbook forClass(String classId) {
        if (classId == null) {
            return EMPTY;
        }
        return BOOKS.getOrDefault(classId, EMPTY);
    }

    public static Map<String, Spellbook> spellbooks() {
        return BOOKS;
    }

    public static Optional<Spell> byId(String classId, String spellId) {
        if (spellId == null) {
            return Optional.empty();
        }
        return forClass(classId).spells().stream()
                .filter(spell -> spell.id().equalsIgnoreCase(spellId))
                .findFirst();
    }
}
