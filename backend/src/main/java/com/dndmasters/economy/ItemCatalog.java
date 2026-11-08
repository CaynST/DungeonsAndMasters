package com.dndmasters.economy;

import java.util.List;

/** Shop catalogue with simplified SRD prices (in gold pieces) and weights (in pounds). */
public final class ItemCatalog {

    public record ItemDef(String id, String name, String kind, int price, String icon, String description,
                          double weight) {
    }

    private static final List<ItemDef> ITEMS = List.of(
            new ItemDef("longsword", "Espada larga", "WEAPON", 15, "sword", "1d8 cortante.", 3),
            new ItemDef("shortsword", "Espada corta", "WEAPON", 10, "sword", "1d6 perforante, ligera.", 2),
            new ItemDef("greataxe", "Hacha grande", "WEAPON", 30, "club", "1d12 cortante, a dos manos.", 7),
            new ItemDef("warhammer", "Martillo de guerra", "WEAPON", 15, "club", "1d8 contundente.", 2),
            new ItemDef("mace", "Maza", "WEAPON", 5, "club", "1d6 contundente.", 4),
            new ItemDef("dagger", "Daga", "WEAPON", 2, "dagger", "1d4 perforante, arrojadiza.", 1),
            new ItemDef("rapier", "Estoque", "WEAPON", 25, "sword", "1d8 perforante, sutil.", 2),
            new ItemDef("shortbow", "Arco corto", "WEAPON", 25, "crosshair", "1d6 perforante, a distancia.", 2),
            new ItemDef("longbow", "Arco largo", "WEAPON", 50, "crosshair", "1d8 perforante, a distancia.", 2),
            new ItemDef("leather", "Armadura de cuero", "ARMOR", 10, "shield", "CA 11 + DES.", 10),
            new ItemDef("studded", "Cuero tachonado", "ARMOR", 45, "shield", "CA 12 + DES.", 13),
            new ItemDef("chain", "Cota de mallas", "ARMOR", 75, "shield", "CA 16, requiere FUE 13.", 55),
            new ItemDef("plate", "Armadura de placas", "ARMOR", 1500, "shield", "CA 18, requiere FUE 15.", 65),
            new ItemDef("shield", "Escudo", "ARMOR", 10, "shield", "+2 a la CA.", 6),
            new ItemDef("potion-healing", "Poción de Curación", "POTION", 50, "flask", "Recupera 2d4+2 HP.", 0.5),
            new ItemDef("potion-greater", "Poción de Curación superior", "POTION", 150, "flask", "Recupera 4d4+4 HP.", 0.5),
            new ItemDef("rope", "Cuerda (15 m)", "GEAR", 1, "key", "Cuerda de cáñamo.", 10),
            new ItemDef("torch", "Antorcha", "GEAR", 1, "flame", "Ilumina 6 casillas.", 1),
            new ItemDef("rations", "Raciones", "GEAR", 1, "flask", "Comida para un día.", 2),
            new ItemDef("thieves-tools", "Herramientas de ladrón", "GEAR", 25, "key", "Abre cerraduras y trampas.", 1),
            new ItemDef("holy-symbol", "Símbolo sagrado", "GEAR", 5, "sparkles", "Canaliza poder divino.", 1),
            new ItemDef("spellbook", "Grimorio", "GEAR", 50, "sparkles", "Contiene tus conjuros.", 3));

    private ItemCatalog() {
    }

    public static List<ItemDef> all() {
        return ITEMS;
    }

    public static ItemDef byId(String id) {
        return ITEMS.stream().filter(i -> i.id().equalsIgnoreCase(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown item: " + id));
    }

    public static ItemDef byName(String name) {
        return ITEMS.stream().filter(i -> i.name().equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    public static int sellPrice(ItemDef item) {
        return Math.max(1, item.price() / 2);
    }
}
