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
        Spec spec = parse(rawExpression);
        if (spec.isConstant()) {
            int value = spec.constant();
            return new DiceRollResult(spec.expression(), List.of(value), 0, value, false, false);
        }
        List<Integer> rolls = new ArrayList<>(spec.count());
        for (int i = 0; i < spec.count(); i++) {
            rolls.add(random.nextInt(spec.sides()) + 1);
        }
        return build(spec, rolls, List.of(), 0);
    }

    /**
     * Rolls the expression and rerolls each die whose face is at or below
     * {@code rerollThreshold} exactly once, keeping the replacement even if it is also low.
     * This is the Great Weapon Fighting mechanic; the dice engine stays unaware of which
     * game rule requested it.
     */
    public DiceRollResult rollRerollingLowFaces(String rawExpression, int rerollThreshold) {
        Spec spec = parse(rawExpression);
        if (spec.isConstant()) {
            int value = spec.constant();
            return new DiceRollResult(spec.expression(), List.of(value), 0, value, false, false);
        }
        List<Integer> rolls = new ArrayList<>(spec.count());
        List<Integer> rerolledFrom = new ArrayList<>();
        int rerollBonus = 0;
        for (int i = 0; i < spec.count(); i++) {
            int face = random.nextInt(spec.sides()) + 1;
            if (rerollThreshold > 0 && face <= rerollThreshold) {
                int replacement = random.nextInt(spec.sides()) + 1;
                rerolledFrom.add(face);
                rerollBonus += replacement - face;
                face = replacement;
            }
            rolls.add(face);
        }
        return build(spec, rolls, rerolledFrom, rerollBonus);
    }

    /** Convenience helper returning just the face of a single d20. */
    public int rollD20() {
        return random.nextInt(20) + 1;
    }

    private DiceRollResult build(Spec spec, List<Integer> rolls, List<Integer> rerolledFrom,
                                 int rerollBonus) {
        int diceTotal = rolls.stream().mapToInt(Integer::intValue).sum();
        int total = diceTotal + spec.modifier();
        boolean natural20 = spec.count() == 1 && spec.sides() == 20 && rolls.get(0) == 20;
        boolean natural1 = spec.count() == 1 && spec.sides() == 20 && rolls.get(0) == 1;
        return new DiceRollResult(spec.expression(), List.copyOf(rolls), spec.modifier(), total,
                natural20, natural1, List.copyOf(rerolledFrom), rerollBonus);
    }

    /** Parsed form of a supported expression; {@code constant} is set for bare numbers. */
    private record Spec(String expression, int count, int sides, int modifier, Integer constant) {

        boolean isConstant() {
            return constant != null;
        }
    }

    private Spec parse(String rawExpression) {
        if (rawExpression == null || rawExpression.isBlank()) {
            throw new DiceParseException("Empty dice expression");
        }
        String expression = rawExpression.replaceAll("\\s+", "");

        if (CONSTANT.matcher(expression).matches()) {
            return new Spec(expression, 1, 0, 0, Integer.parseInt(expression));
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
        return new Spec(canonical(count, sides, modifier), count, sides, modifier, null);
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
