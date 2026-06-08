package org.example;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class GameLoopTest {

    private static final double FIXED_DT = 1.0 / 60.0;

    @Test
    void zeroAccumulatorRunsNoTicks() {
        AtomicInteger count = new AtomicInteger();
        double remainder = GameLoop.runFixedSteps(0.0, FIXED_DT, count::incrementAndGet);
        assertEquals(0, count.get());
        assertEquals(0.0, remainder, 1e-12);
    }

    @Test
    void accumulatorBelowFixedDtRunsNoTicks() {
        AtomicInteger count = new AtomicInteger();
        double accumulator = FIXED_DT * 0.99;
        double remainder = GameLoop.runFixedSteps(accumulator, FIXED_DT, count::incrementAndGet);
        assertEquals(0, count.get());
        assertEquals(accumulator, remainder, 1e-12);
    }

    @Test
    void exactlyOneFixedDtRunsOneTick() {
        AtomicInteger count = new AtomicInteger();
        double remainder = GameLoop.runFixedSteps(FIXED_DT, FIXED_DT, count::incrementAndGet);
        assertEquals(1, count.get());
        assertEquals(0.0, remainder, 1e-12);
    }

    @Test
    void multipleStepsConsumedCorrectly() {
        AtomicInteger count = new AtomicInteger();
        double accumulator = FIXED_DT * 3.5;
        double remainder = GameLoop.runFixedSteps(accumulator, FIXED_DT, count::incrementAndGet);
        assertEquals(3, count.get());
        assertEquals(FIXED_DT * 0.5, remainder, 1e-10);
    }

    @Test
    void remainderIsAlwaysLessThanFixedDt() {
        double[] inputs = {0.001, 0.016, 0.033, 0.100, 0.250};
        for (double input : inputs) {
            double remainder = GameLoop.runFixedSteps(input, FIXED_DT, () -> {});
            assertTrue(remainder < FIXED_DT,
                    "remainder " + remainder + " >= fixedDt for input " + input);
        }
    }

    @Test
    void ticksRunInOrderWithCorrectCount() {
        int[] callOrder = new int[5];
        AtomicInteger idx = new AtomicInteger();
        double accumulator = FIXED_DT * 5;
        GameLoop.runFixedSteps(accumulator, FIXED_DT, () -> callOrder[idx.getAndIncrement()] = idx.get());
        for (int i = 0; i < 5; i++) {
            assertEquals(i + 1, callOrder[i], "Tick " + i + " received wrong call index");
        }
    }
}
