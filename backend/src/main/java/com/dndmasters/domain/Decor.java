package com.dndmasters.domain;

public record Decor(int x, int y, String icon, String kind) {

    public Cell cell() {
        return new Cell(x, y);
    }
}
