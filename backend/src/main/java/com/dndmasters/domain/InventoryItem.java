package com.dndmasters.domain;

/**
 * One inventory entry. {@code weight} is the weight of a single unit in pounds and is
 * multiplied by {@code quantity} when the carried total is computed.
 *
 * <p>Legacy characters stored before R-15 have no {@code weight} property in their
 * {@code inventory_json}; Jackson deserializes the missing primitive as {@code 0}.
 * The four-argument constructor keeps existing call sites compatible.</p>
 */
public record InventoryItem(String name, String description, String icon, int quantity, double weight) {

    /** Backward-compatible constructor: unknown weight is treated as zero. */
    public InventoryItem(String name, String description, String icon, int quantity) {
        this(name, description, icon, quantity, 0);
    }

    public static InventoryItem of(String name, String description) {
        return new InventoryItem(name, description, "package", 1, 0);
    }

    public static InventoryItem of(String name, String description, double weight) {
        return new InventoryItem(name, description, "package", 1, weight);
    }

    public static InventoryItem of(String name, String description, String icon, int quantity, double weight) {
        return new InventoryItem(name, description, icon, quantity, weight);
    }

    /** Weight carried by this line: unit weight times quantity. */
    public double totalWeight() {
        return weight * quantity;
    }
}
