package com.dndmasters.domain;

public record Door(int x, int y, boolean open) {

    public Cell cell() {
        return new Cell(x, y);
    }

    public boolean blocksMovement() {
        return !open;
    }
}
