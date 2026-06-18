package org.example.audio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FootstepCadence pure logic — no audio device required.
 */
class FootstepCadenceTest {

    private FootstepCadence cadence;

    @BeforeEach
    void setUp() {
        cadence = new FootstepCadence();
    }

    @Test
    void noStepWhenNotGrounded() {
        boolean fired = cadence.tick(5f, 1f, false);
        assertFalse(fired, "Should not fire when not grounded");
    }

    @Test
    void noStepWhenStandingStill() {
        boolean fired = cadence.tick(0f, 1f, true);
        assertFalse(fired, "Zero speed should never trigger a step");
    }

    @Test
    void stepFiresAfterSufficientDistance() {
        // Walk at 2 m/s for exactly 1 second = 2 m = STEP_DISTANCE_METRES
        boolean fired = cadence.tick(FootstepCadence.STEP_DISTANCE_METRES, 1f, true);
        assertTrue(fired, "Step should fire once the full step distance is covered");
    }

    @Test
    void stepDoesNotFireBeforeSufficientDistance() {
        // Walk at 1 m/s for 0.5 s = 0.5 m < STEP_DISTANCE_METRES
        boolean fired = cadence.tick(1f, 0.5f, true);
        assertFalse(fired, "Step must not fire before full step distance is reached");
    }

    @Test
    void stepFiresRepeatedly() {
        // Advance far enough in one tick to cover two steps
        float speed = FootstepCadence.STEP_DISTANCE_METRES * 2f;
        // First step
        boolean first = cadence.tick(speed, 1f, true);
        assertTrue(first, "First step fires after one STEP_DISTANCE_METRES");
        // Remainder covers another STEP_DISTANCE_METRES on the very next tick (zero dt)
        boolean second = cadence.tick(speed, 1f, true);
        assertTrue(second, "Second step fires on the following tick");
    }

    @Test
    void resetClearsAccumulatedDistance() {
        // Accumulate almost a full step
        cadence.tick(FootstepCadence.STEP_DISTANCE_METRES * 0.9f, 1f, true);
        cadence.reset();
        // After reset only the tiny distance from this tick counts
        boolean fired = cadence.tick(0.01f, 1f, true);
        assertFalse(fired, "Reset should clear accumulated distance so no immediate step");
    }

    @Test
    void accumulatorCarriesOverBetweenTicks() {
        float halfStep = FootstepCadence.STEP_DISTANCE_METRES / 2f;
        // Two small ticks, each covering half a step
        assertFalse(cadence.tick(halfStep, 1f, true), "No step after half distance");
        assertTrue(cadence.tick(halfStep, 1f, true),  "Step fires after full distance");
    }

    @Test
    void groundedTransitionResetsAccumulator() {
        // Accumulate half a step while grounded
        cadence.tick(FootstepCadence.STEP_DISTANCE_METRES * 0.4f, 1f, true);
        // Go airborne (reset)
        cadence.reset();
        // Land again and walk — needs a full step from scratch
        assertFalse(cadence.tick(FootstepCadence.STEP_DISTANCE_METRES * 0.4f, 1f, true),
                    "After reset, half a step should not fire");
    }
}
