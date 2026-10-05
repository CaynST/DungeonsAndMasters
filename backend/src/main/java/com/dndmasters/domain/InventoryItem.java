package com.dndmasters.domain;

public record InventoryItem(String name, String description, String icon, int quantity) {

    public static InventoryItem of(String name, String description) {
        return new InventoryItem(name, description, "package", 1);
    }
}
