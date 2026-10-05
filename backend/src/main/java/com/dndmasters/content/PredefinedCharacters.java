package com.dndmasters.content;

import com.dndmasters.domain.Attack;
import com.dndmasters.domain.InventoryItem;
import com.dndmasters.domain.PlayerCharacter;
import com.dndmasters.domain.Stats;
import java.util.List;

/** The four pre-generated heroes used by the demo. */
public final class PredefinedCharacters {

    private PredefinedCharacters() {
    }

    public static List<PlayerCharacter> all() {
        return List.of(kael(), lyra(), aria(), thorin());
    }

    public static PlayerCharacter kael() {
        PlayerCharacter c = base("Kael", "Fighter", "Human", 28, 16, 14, 16, 13, 10, 12, 8,
                "#C2410C", "sword", "/images/heroes/kael.png");
        c.setAttacks(List.of(
                Attack.melee("Longsword", 5, "1d8+3"),
                Attack.ranged("Jabalina", 4, "1d6+2", 8)));
        c.setInventory(List.of(
                InventoryItem.of("Escudo", "+2 a la CA frente a ataques cuerpo a cuerpo."),
                InventoryItem.of("Poción de Curación", "Recupera 2d4+2 HP."),
                InventoryItem.of("Raciones", "Provisión de viaje."),
                InventoryItem.of("Antorcha", "Ilumina 6 casillas.")));
        return c;
    }

    public static PlayerCharacter lyra() {
        PlayerCharacter c = base("Lyra", "Wizard", "Elf", 18, 12, 8, 14, 12, 16, 13, 10,
                "#7C3AED", "sparkles", "/images/heroes/lyra.png");
        c.setAttacks(List.of(
                Attack.ranged("Fuego feérico", 5, "1d10", 16),
                Attack.melee("Bastón", 1, "1d6")));
        c.setInventory(List.of(
                InventoryItem.of("Grimorio", "Hechizos de nivel 1: proyectil mágico, escudo."),
                InventoryItem.of("Componentes", "Bolsa de componentes arcanos."),
                InventoryItem.of("Poción de Curación", "Recupera 2d4+2 HP.")));
        return c;
    }

    public static PlayerCharacter aria() {
        PlayerCharacter c = base("Aria", "Rogue", "Half-Elf", 22, 14, 10, 17, 12, 13, 11, 14,
                "#0F766E", "dagger", "/images/heroes/aria.png");
        c.setAttacks(List.of(
                Attack.melee("Espada corta", 5, "1d6+3"),
                Attack.ranged("Arco corto", 5, "1d6+3", 16)));
        c.setInventory(List.of(
                InventoryItem.of("Herramientas de ladrón", "Abre cerraduras."),
                InventoryItem.of("Daga oculta", "Ligera y silenciosa."),
                InventoryItem.of("Cuerda", "15 metros de cuerda de seda.")));
        return c;
    }

    public static PlayerCharacter thorin() {
        PlayerCharacter c = base("Thorin", "Cleric", "Dwarf", 26, 16, 14, 10, 15, 9, 16, 11,
                "#B45309", "shield", "/images/heroes/thorin.png");
        c.setAttacks(List.of(
                Attack.melee("Martillo de guerra", 4, "1d8+2"),
                Attack.ranged("Llama sagrada", 4, "1d8", 12)));
        c.setInventory(List.of(
                InventoryItem.of("Símbolo sagrado", "Canaliza el poder de tu deidad."),
                InventoryItem.of("Escudo", "+2 a la CA."),
                InventoryItem.of("Poción de Curación", "Recupera 2d4+2 HP.")));
        return c;
    }

    private static PlayerCharacter base(
            String name, String cls, String race,
            int hp, int ac, int str, int dex, int con, int intel, int wis, int cha,
            String color, String icon, String portrait) {
        PlayerCharacter c = new PlayerCharacter();
        c.setName(name);
        c.setCls(cls);
        c.setRace(race);
        c.setLevel(1);
        c.setHp(hp);
        c.setMaxHp(hp);
        c.setAc(ac);
        c.setStats(new Stats(str, dex, con, intel, wis, cha));
        c.setSpeed(6);
        c.setColor(color);
        c.setIcon(icon);
        c.setPortrait(portrait);
        c.setPredefined(true);
        return c;
    }
}
