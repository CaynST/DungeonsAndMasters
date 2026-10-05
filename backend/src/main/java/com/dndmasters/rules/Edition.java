package com.dndmasters.rules;

/** Supported D&D rulesets. The campaign pins one; character creation respects it. */
public enum Edition {
    DND_5E_2014("D&D 5e (2014)", "2014"),
    DND_5E_2024("D&D 5e (2024)", "2024");

    private final String displayName;
    private final String year;

    Edition(String displayName, String year) {
        this.displayName = displayName;
        this.year = year;
    }

    public String displayName() {
        return displayName;
    }

    public String year() {
        return year;
    }

    public static Edition fromId(String id) {
        if (id == null) {
            return DND_5E_2014;
        }
        return switch (id.toLowerCase().replace("-", "_")) {
            case "dnd_5e_2024", "2024", "5e_2024" -> DND_5E_2024;
            default -> DND_5E_2014;
        };
    }
}
