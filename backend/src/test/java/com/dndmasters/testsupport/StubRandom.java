package com.dndmasters.testsupport;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Deterministic {@link Random} for tests. Returns the configured 0-based values in
 * order (cycling). To simulate a d20 face of 20 pass {@code 19}.
 */
public final class StubRandom extends Random {

    private final int[] values;
    private final AtomicInteger index = new AtomicInteger();

    public StubRandom(int... zeroBasedValues) {
        this.values = zeroBasedValues.length == 0 ? new int[]{0} : zeroBasedValues;
    }

    @Override
    public int nextInt(int bound) {
        int value = values[index.getAndIncrement() % values.length];
        return Math.floorMod(value, bound);
    }

    public static Random of(int... zeroBasedValues) {
        return new StubRandom(zeroBasedValues);
    }
}
