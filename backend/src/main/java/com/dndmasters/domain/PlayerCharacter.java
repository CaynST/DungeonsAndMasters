package com.dndmasters.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlayerCharacter {

    /** SRD carrying capacity: 15 pounds per point of Strength. */
    public static final int POUNDS_PER_STRENGTH = 15;

    private Long id;
    private Long ownerId;
    private String name;
    private String cls;
    private String race;
    private int level = 1;
    private int hp;
    private int maxHp;
    private int ac;
    private Stats stats;
    private int speed = 6;
    private String color;
    private String icon;
    private String portrait;
    private List<Attack> attacks = new ArrayList<>();
    private List<InventoryItem> inventory = new ArrayList<>();
    private boolean predefined;
    private Map<String, Object> creationData = new HashMap<>();
    private int gold;
    private int silver;
    private int copper;

    /** Total wealth expressed in gold pieces (1 gp = 10 sp = 100 cp). */
    public double totalGold() {
        return gold + silver / 10.0 + copper / 100.0;
    }

    /** Total weight in pounds carried by the current inventory. */
    public double totalCarriedWeight() {
        if (inventory == null) {
            return 0;
        }
        return inventory.stream()
                .filter(item -> item != null)
                .mapToDouble(InventoryItem::totalWeight)
                .sum();
    }

    /** SRD carrying capacity in pounds (Strength × 15). */
    public int carryingCapacity() {
        return Math.max(0, stats == null ? 0 : stats.strength()) * POUNDS_PER_STRENGTH;
    }

    /**
     * Display-only flag for R-15: true when the carried weight exceeds capacity.
     * Movement and speed are intentionally unchanged until encumbrance rules are
     * integrated into the turn economy.
     */
    public boolean isEncumbered() {
        return totalCarriedWeight() > carryingCapacity();
    }

    // Jackson serializes these getters so the sheet and shop can display the values
    // without duplicating the rule on the client.
    public double getCarriedWeight() {
        return totalCarriedWeight();
    }

    public int getCarryingCapacity() {
        return carryingCapacity();
    }

    public boolean alive() {
        return hp > 0;
    }
}
