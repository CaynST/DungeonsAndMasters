package com.dndmasters.economy;

import java.util.List;

/** Shop catalogue with simplified SRD prices (in gold pieces). */
public final class ItemCatalog {

    public record ItemDef(String id, String name, String kind, int price, String icon, String description) {
    }

    private static final List<ItemDef> ITEMS = List.of(
            new ItemDef("longsword", "Espada larga", "WEAPON", 15, "sword", "1d8 cortante."),
            new ItemDef("shortsword", "Espada corta", "WEAPON", 10, "sword", "1d6 perforante, ligera."),
            new ItemDef("greataxe", "Hacha grande", "WEAPON", 30, "club", "1d12 cortante, a dos manos."),
            new ItemDef("warhammer", "Martillo de guerra", "WEAPON", 15, "club", "1d8 contundente."),
            new ItemDef("mace", "Maza", "WEAPON", 5, "club", "1d6 contundente."),
            new ItemDef("dagger", "Daga", "WEAPON", 2, "dagger", "1d4 perforante, arrojadiza."),
            new ItemDef("rapier", "Estoque", "WEAPON", 25, "sword", "1d8 perforante, sutil."),
            new ItemDef("shortbow", "Arco corto", "WEAPON", 25, "crosshair", "1d6 perforante, a distancia."),
            new ItemDef("longbow", "Arco largo", "WEAPON", 50, "crosshair", "1d8 perforante, a distancia."),
            new ItemDef("leather", "Armadura de cuero", "ARMOR", 10, "shield", "CA 11 + DES."),
            new ItemDef("studded", "Cuero tachonado", "ARMOR", 45, "shield", "CA 12 + DES."),
            new ItemDef("chain", "Cota de mallas", "ARMOR", 75, "shield", "CA 16, requiere FUE 13."),
            new ItemDef("plate", "Armadura de placas", "ARMOR", 1500, "shield", "CA 18, requiere FUE 15."),
            new ItemDef("shield", "Escudo", "ARMOR", 10, "shield", "+2 a la CA."),
            new ItemDef("potion-healing", "Poción de Curación", "POTION", 50, "flask", "Recupera 2d4+2 HP."),
            new ItemDef("potion-greater", "Poción de Curación superior", "POTION", 150, "flask", "Recupera 4d4+4 HP."),
            new ItemDef("rope", "Cuerda (15 m)", "GEAR", 1, "key", "Cuerda de cáñamo."),
            new ItemDef("torch", "Antorcha", "GEAR", 1, "flame", "Ilumina 6 casillas."),
            new ItemDef("rations", "Raciones", "GEAR", 1, "flask", "Comida para un día."),
            new ItemDef("thieves-tools", "Herramientas de ladrón", "GEAR", 25, "key", "Abre cerraduras y trampas."),
            new ItemDef("holy-symbol", "Símbolo sagrado", "GEAR", 5, "sparkles", "Canaliza poder divino."),
            new ItemDef("spellbook", "Grimorio", "GEAR", 50, "sparkles", "Contiene tus conjuros."));

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
