package com.dndmasters.engine.dice;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure dice roller supporting {@code NdM}, {@code NdM+K}, {@code NdM-K} and bare
 * constants. All randomness is injected so tests are deterministic.
 */
public class DiceEngine {

    private static final Pattern DICE = Pattern.compile("^(\\d*)d(\\d+)([+-]\\d+)?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern CONSTANT = Pattern.compile("^[+-]?\\d+$");
    private static final int MAX_DICE = 100;
    private static final int MAX_SIDES = 1000;

    private final Random random;

    public DiceEngine() {
        this(new Random());
    }

    public DiceEngine(Random random) {
        this.random = random;
    }

    public DiceRollResult roll(String rawExpression) {
        if (rawExpression == null || rawExpression.isBlank()) {
            throw new DiceParseException("Empty dice expression");
        }
        String expression = rawExpression.replaceAll("\\s+", "");

        if (CONSTANT.matcher(expression).matches()) {
            int value = Integer.parseInt(expression);
            return new DiceRollResult(expression, List.of(value), 0, value, false, false);
        }

        Matcher matcher = DICE.matcher(expression);
        if (!matcher.matches()) {
            throw new DiceParseException("Unrecognized dice expression: '" + rawExpression + "'");
        }
        int count = matcher.group(1).isEmpty() ? 1 : Integer.parseInt(matcher.group(1));
        int sides = Integer.parseInt(matcher.group(2));
        int modifier = matcher.group(3) == null ? 0 : Integer.parseInt(matcher.group(3));

        if (count < 1 || count > MAX_DICE) {
            throw new DiceParseException("Dice count out of range (1.." + MAX_DICE + "): " + count);
        }
        if (sides < 1 || sides > MAX_SIDES) {
            throw new DiceParseException("Dice sides out of range (1.." + MAX_SIDES + "): " + sides);
        }

        List<Integer> rolls = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            rolls.add(random.nextInt(sides) + 1);
        }
        int diceTotal = rolls.stream().mapToInt(Integer::intValue).sum();
        int total = diceTotal + modifier;

        boolean natural20 = count == 1 && sides == 20 && rolls.get(0) == 20;
        boolean natural1 = count == 1 && sides == 20 && rolls.get(0) == 1;

        return new DiceRollResult(canonical(count, sides, modifier), List.copyOf(rolls), modifier, total, natural20, natural1);
    }

    /** Convenience helper returning just the face of a single d20. */
    public int rollD20() {
        return random.nextInt(20) + 1;
    }

    private String canonical(int count, int sides, int modifier) {
        StringBuilder sb = new StringBuilder().append(count).append('d').append(sides);
        if (modifier > 0) {
            sb.append('+').append(modifier);
        } else if (modifier < 0) {
            sb.append(modifier);
        }
        return sb.toString();
    }
}
