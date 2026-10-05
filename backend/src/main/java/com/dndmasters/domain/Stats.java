package com.dndmasters.domain;

public record Stats(
        int strength,
        int dexterity,
        int constitution,
        int intelligence,
        int wisdom,
        int charisma) {

    public int get(Ability ability) {
        return switch (ability) {
            case STRENGTH -> strength;
            case DEXTERITY -> dexterity;
            case CONSTITUTION -> constitution;
            case INTELLIGENCE -> intelligence;
            case WISDOM -> wisdom;
            case CHARISMA -> charisma;
        };
    }
}
