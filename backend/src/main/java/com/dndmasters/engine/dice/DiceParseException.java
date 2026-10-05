package com.dndmasters.engine.dice;

/** Thrown when a dice expression cannot be parsed. */
public class DiceParseException extends RuntimeException {

    public DiceParseException(String message) {
        super(message);
    }
}
