package com.dndmasters.content;

import com.dndmasters.domain.Attack;
import com.dndmasters.domain.Stats;
import java.util.List;
import java.util.Random;

/**
 * Monster catalogue used by the adventure generator. Simplified SRD stats, grouped by
 * tier so encounters scale from skirmish to boss fight.
 */
public final class Bestiary {

    public enum Tier { MINION, STANDARD, ELITE, BOSS }

    public record Monster(
            String id, String name, Tier tier, int hp, int ac, int speed, Stats stats,
            List<Attack> attacks, String behavior, int xp, String color, String icon, String portrait) {
    }

    private static final List<Monster> MONSTERS = List.of(
            m("rat", "Rata gigante", Tier.MINION, 7, 12, 8, s(7, 15, 10, 2, 10, 4),
                    List.of(Attack.melee("Mordisco", 4, "1d4+2")), "MELEE", 25, "#8B7355", "skull"),
            m("kobold", "Kobold", Tier.MINION, 5, 12, 6, s(7, 15, 9, 8, 7, 8),
                    List.of(Attack.melee("Daga", 4, "1d4+2")), "MELEE", 25, "#A0522D", "skull"),
            m("goblin", "Goblin", Tier.MINION, 7, 13, 6, s(8, 14, 10, 10, 8, 8),
                    List.of(Attack.melee("Cimitarra", 4, "1d6+2")), "MELEE", 50, "#7BA05B", "skull"),
            m("goblin-archer", "Goblin arquero", Tier.MINION, 7, 12, 6, s(8, 15, 10, 10, 8, 8),
                    List.of(Attack.ranged("Arco corto", 4, "1d6+2", 16)), "RANGED", 50, "#9BBF7B", "crosshair"),
            m("cultist", "Cultista", Tier.MINION, 9, 12, 6, s(11, 12, 10, 10, 11, 10),
                    List.of(Attack.melee("Cimitarra", 3, "1d6+1")), "MELEE", 50, "#6B21A8", "flame"),
            m("bandit", "Bandido", Tier.STANDARD, 11, 12, 6, s(11, 12, 12, 10, 10, 10),
                    List.of(Attack.melee("Cimitarra", 3, "1d6+1")), "MELEE", 50, "#8B4513", "sword"),
            m("skeleton", "Esqueleto", Tier.STANDARD, 13, 13, 6, s(10, 14, 15, 6, 8, 5),
                    List.of(Attack.melee("Espada corta", 4, "1d6+2")), "MELEE", 50, "#D8C39A", "skull"),
            m("wolf", "Lobo", Tier.STANDARD, 11, 13, 8, s(12, 15, 12, 3, 12, 6),
                    List.of(Attack.melee("Mordisco", 4, "2d4+2")), "MELEE", 50, "#6B7280", "paw"),
            m("hobgoblin", "Hobgoblin", Tier.STANDARD, 11, 18, 6, s(13, 12, 12, 10, 10, 9),
                    List.of(Attack.melee("Espada larga", 3, "1d8+1")), "MELEE", 100, "#B45309", "shield"),
            m("zombie", "Zombi", Tier.STANDARD, 22, 8, 4, s(13, 6, 16, 3, 6, 5),
                    List.of(Attack.melee("Golpe", 3, "1d6+1")), "MELEE", 50, "#4B5563", "skull"),
            m("spider", "Araña gigante", Tier.STANDARD, 26, 14, 8, s(14, 16, 12, 2, 11, 4),
                    List.of(Attack.melee("Mordisco venenoso", 5, "1d8+3")), "MELEE", 200, "#374151", "bug"),
            m("ghoul", "Ghoul", Tier.ELITE, 22, 12, 6, s(13, 15, 10, 7, 10, 6),
                    List.of(Attack.melee("Garras paralizantes", 4, "2d4+2")), "MELEE", 200, "#5B7C4A", "skull"),
            m("orc", "Orco", Tier.ELITE, 15, 13, 6, s(16, 12, 14, 7, 11, 10),
                    List.of(Attack.melee("Hacha grande", 5, "1d12+3")), "MELEE", 100, "#4D7C0F", "sword"),
            m("animated-armor", "Armadura animada", Tier.ELITE, 33, 18, 6, s(14, 11, 13, 1, 3, 1),
                    List.of(Attack.melee("Golpe", 4, "1d6+2")), "AGGRESSIVE", 200, "#94A3B8", "shield"),
            m("wight", "Wight", Tier.ELITE, 45, 14, 6, s(15, 14, 16, 10, 13, 15),
                    List.of(Attack.melee("Espada larga", 4, "1d8+2"), Attack.ranged("Arco largo", 4, "1d8+2", 20)),
                    "AGGRESSIVE", 700, "#6B7280", "sword"),
            m("ogre", "Ogro", Tier.BOSS, 59, 11, 8, s(19, 8, 16, 5, 7, 7),
                    List.of(Attack.melee("Garrote", 6, "2d8+4")), "AGGRESSIVE", 450, "#78716C", "club"),
            m("owlbear", "Oso búho", Tier.BOSS, 59, 13, 8, s(20, 12, 17, 3, 12, 7),
                    List.of(Attack.melee("Garras", 7, "2d8+5"), Attack.melee("Pico", 7, "1d10+5")),
                    "AGGRESSIVE", 700, "#A16207", "paw"),
            m("guardian", "Guardián de piedra", Tier.BOSS, 60, 17, 5, s(18, 10, 16, 8, 10, 10),
                    List.of(Attack.melee("Golpe sepulcral", 6, "2d6+3"), Attack.ranged("Descarga necrótica", 5, "2d8", 12)),
                    "AGGRESSIVE", 1100, "#8B5CF6", "shield"),
            m("necromancer", "Nigromante", Tier.BOSS, 45, 12, 6, s(9, 14, 12, 17, 12, 11),
                    List.of(Attack.ranged("Descarga arcana", 5, "3d6", 18)), "RANGED", 1100, "#7C3AED", "sparkles"),
            m("dragon-wyrmling", "Cría de dragón", Tier.BOSS, 75, 17, 8, s(19, 14, 17, 14, 11, 15),
                    List.of(Attack.melee("Mordisco", 6, "2d10+4"), Attack.ranged("Aliento", 5, "4d6", 12)),
                    "AGGRESSIVE", 2300, "#B91C1C", "flame"));

    private Bestiary() {
    }

    public static List<Monster> all() {
        return MONSTERS;
    }

    public static Monster byId(String id) {
        return MONSTERS.stream().filter(m -> m.id().equalsIgnoreCase(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown monster: " + id));
    }

    public static List<Monster> byTier(Tier tier) {
        return MONSTERS.stream().filter(m -> m.tier() == tier).toList();
    }

    /** Picks {@code count} monsters of the given tier, allowing repeats. */
    public static List<Monster> pick(Tier tier, int count, Random random) {
        List<Monster> pool = byTier(tier);
        return java.util.stream.IntStream.range(0, Math.max(0, count))
                .mapToObj(i -> pool.get(random.nextInt(pool.size())))
                .toList();
    }

    private static Monster m(String id, String name, Tier tier, int hp, int ac, int speed, Stats stats,
                             List<Attack> attacks, String behavior, int xp, String color, String icon) {
        return new Monster(id, name, tier, hp, ac, speed, stats, attacks, behavior, xp, color, icon,
                "/images/enemies/" + id + ".png");
    }

    private static Stats s(int str, int dex, int con, int intel, int wis, int cha) {
        return new Stats(str, dex, con, intel, wis, cha);
    }
}
