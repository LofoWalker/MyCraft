package org.example.world;

import org.example.components.Hunger;
import org.example.components.HungerTimers;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HungerMathTest {

    private static final float DT = 1f / 60f;

    // --- exhaustion accumulation ---

    @Test
    void movementAddsExhaustionProportionalToDistance() {
        float near = HungerMath.exhaustionFromMovement(1f, 0f);
        float far  = HungerMath.exhaustionFromMovement(4f, 0f);
        assertTrue(far > near);
        assertEquals(WorldConstants.EXHAUSTION_PER_BLOCK_MOVED, near, 1e-6f);
    }

    @Test
    void addExhaustionIgnoresNegativeContributions() {
        assertEquals(2f, HungerMath.addExhaustion(2f, -5f), 1e-6f);
    }

    // --- draining: saturation first, then food ---

    @Test
    void drainSpendsSaturationBeforeFood() {
        Hunger start = new Hunger(20, 3f);
        HungerMath.DrainResult r = HungerMath.drain(start, WorldConstants.EXHAUSTION_THRESHOLD);
        assertEquals(20, r.hunger().food());
        assertEquals(2f, r.hunger().saturation(), 1e-6f);
    }

    @Test
    void drainEatsFoodOnceSaturationIsEmpty() {
        Hunger start = new Hunger(20, 0f);
        HungerMath.DrainResult r = HungerMath.drain(start, WorldConstants.EXHAUSTION_THRESHOLD);
        assertEquals(19, r.hunger().food());
        assertEquals(0f, r.hunger().saturation(), 1e-6f);
    }

    @Test
    void drainKeepsLeftoverBelowThreshold() {
        float exhaustion = WorldConstants.EXHAUSTION_THRESHOLD * 1.5f;
        HungerMath.DrainResult r = HungerMath.drain(new Hunger(20, 10f), exhaustion);
        assertTrue(r.exhaustion() < WorldConstants.EXHAUSTION_THRESHOLD);
        assertEquals(9f, r.hunger().saturation(), 1e-6f);
    }

    @Test
    void drainNeverDrivesFoodNegative() {
        HungerMath.DrainResult r = HungerMath.drain(new Hunger(0, 0f),
                WorldConstants.EXHAUSTION_THRESHOLD * 5f);
        assertEquals(0, r.hunger().food());
        assertEquals(0f, r.hunger().saturation(), 1e-6f);
    }

    // --- eating ---

    @Test
    void eatingRaisesFoodAndSaturation() {
        Hunger after = HungerMath.eat(new Hunger(10, 0f), 5, 6f);
        assertEquals(15, after.food());
        assertEquals(6f, after.saturation(), 1e-6f);
    }

    @Test
    void eatingCapsFoodAtMax() {
        Hunger after = HungerMath.eat(new Hunger(WorldConstants.MAX_FOOD - 1, 0f), 5, 6f);
        assertEquals(WorldConstants.MAX_FOOD, after.food());
    }

    @Test
    void saturationNeverExceedsFood() {
        Hunger after = HungerMath.eat(new Hunger(WorldConstants.MAX_FOOD, 0f), 5, 100f);
        assertEquals(WorldConstants.MAX_FOOD, after.food());
        assertEquals(WorldConstants.MAX_FOOD, after.saturation(), 1e-6f);
    }

    // --- health coupling ---

    @Test
    void wellFedRegeneratesAndSpendsHunger() {
        Hunger fed = new Hunger(WorldConstants.REGEN_FOOD_THRESHOLD, 5f);
        HungerMath.TickResult r = HungerMath.tickHealth(fed, new HungerTimers(0f, 0f, 0f),
                WorldConstants.REGEN_INTERVAL, true, false);
        assertEquals(WorldConstants.REGEN_AMOUNT, r.healthDelta());
        assertTrue(r.hunger().saturation() < 5f);
    }

    @Test
    void lowFoodDoesNotRegenerate() {
        Hunger low = new Hunger(WorldConstants.REGEN_FOOD_THRESHOLD - 1, 5f);
        HungerMath.TickResult r = HungerMath.tickHealth(low, new HungerTimers(0f, 0f, 0f),
                WorldConstants.REGEN_INTERVAL, true, false);
        assertEquals(0, r.healthDelta());
    }

    @Test
    void regenSuppressedInsidePostDamageDelay() {
        Hunger fed = new Hunger(WorldConstants.MAX_FOOD, 5f);
        HungerMath.TickResult r = HungerMath.tickHealth(fed, new HungerTimers(0f, 0f, 0f),
                WorldConstants.REGEN_INTERVAL, false, false);
        assertEquals(0, r.healthDelta());
    }

    @Test
    void noRegenAtFullHealth() {
        Hunger fed = new Hunger(WorldConstants.MAX_FOOD, 5f);
        HungerMath.TickResult r = HungerMath.tickHealth(fed, new HungerTimers(0f, 0f, 0f),
                WorldConstants.REGEN_INTERVAL, true, true);
        assertEquals(0, r.healthDelta());
    }

    @Test
    void emptyFoodStarvesAfterInterval() {
        Hunger empty = new Hunger(0, 0f);
        HungerMath.TickResult r = HungerMath.tickHealth(empty, new HungerTimers(0f, 0f, 0f),
                WorldConstants.STARVE_INTERVAL, true, false);
        assertEquals(-WorldConstants.STARVE_DAMAGE, r.healthDelta());
    }

    @Test
    void starvationAccumulatesBelowInterval() {
        Hunger empty = new Hunger(0, 0f);
        HungerMath.TickResult r = HungerMath.tickHealth(empty, new HungerTimers(0f, 0f, 0f),
                DT, true, false);
        assertEquals(0, r.healthDelta());
        assertTrue(r.timers().starveAccum() > 0f);
    }
}
