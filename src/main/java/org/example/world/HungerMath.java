package org.example.world;

import org.example.components.Hunger;
import org.example.components.HungerTimers;

/**
 * Pure hunger arithmetic — no ECS/World access, so every rule is unit-testable in isolation.
 *
 * <p>Activity feeds {@link HungerTimers#exhaustion()}. Each whole {@code EXHAUSTION_THRESHOLD} reached
 * spends one point: saturation first, then food once saturation is empty. A well-fed player slowly
 * converts hunger into healing; an empty player takes periodic starvation damage. Eating a food raises
 * food (capped at {@code MAX_FOOD}) and saturation (then re-clamped to the new food value).
 */
public final class HungerMath {

    /** Result of draining accumulated exhaustion: the new hunger plus the leftover exhaustion. */
    public record DrainResult(Hunger hunger, float exhaustion) {}

    /** Result of a heal/starve step: how many health points to apply (+heal / -damage) and the new state. */
    public record TickResult(int healthDelta, Hunger hunger, HungerTimers timers) {}

    private HungerMath() {}

    // --- Activity exhaustion ---

    public static float exhaustionFromMovement(float dx, float dz) {
        float distance = (float) Math.sqrt(dx * dx + dz * dz);
        return distance * WorldConstants.EXHAUSTION_PER_BLOCK_MOVED;
    }

    public static float addExhaustion(float current, float added) {
        return current + Math.max(added, 0f);
    }

    // --- Draining: spend whole thresholds of exhaustion into saturation then food ---

    public static DrainResult drain(Hunger hunger, float exhaustion) {
        int points = (int) (exhaustion / WorldConstants.EXHAUSTION_THRESHOLD);
        float leftover = exhaustion - points * WorldConstants.EXHAUSTION_THRESHOLD;
        Hunger drained = hunger;
        for (int i = 0; i < points; i++) drained = spendOne(drained);
        return new DrainResult(drained, leftover);
    }

    private static Hunger spendOne(Hunger hunger) {
        if (hunger.saturation() > 0f) {
            return new Hunger(hunger.food(), Math.max(hunger.saturation() - 1f, 0f));
        }
        return new Hunger(Math.max(hunger.food() - 1, 0), 0f);
    }

    // --- Eating ---

    public static Hunger eat(Hunger hunger, int foodRestore, float saturationRestore) {
        int food = Math.min(hunger.food() + foodRestore, WorldConstants.MAX_FOOD);
        float saturation = Math.min(hunger.saturation() + saturationRestore, food);
        return new Hunger(food, saturation);
    }

    // --- Health coupling: regen when well fed, starve when empty ---

    public static boolean canRegen(Hunger hunger) {
        return hunger.food() >= WorldConstants.REGEN_FOOD_THRESHOLD;
    }

    public static boolean starving(Hunger hunger) {
        return hunger.food() <= 0;
    }

    /**
     * Advances the regen timer while well fed and pays for each heal point by spending hunger; advances
     * the starve timer while empty and emits damage. Returns no health change in the neutral band.
     * Healing is suppressed when {@code regenAllowed} is false (e.g. inside the post-damage delay or at
     * full health) but the timer is still cleared so it does not bank up.
     */
    public static TickResult tickHealth(Hunger hunger, HungerTimers timers, float dt,
                                        boolean regenAllowed, boolean atFullHealth) {
        if (starving(hunger)) return tickStarve(hunger, timers, dt);
        if (canRegen(hunger) && regenAllowed && !atFullHealth) return tickRegen(hunger, timers, dt);
        return new TickResult(0, hunger, withResetHealthTimers(timers));
    }

    private static TickResult tickRegen(Hunger hunger, HungerTimers timers, float dt) {
        float accum = timers.regenAccum() + dt;
        int heals = (int) (accum / WorldConstants.REGEN_INTERVAL);
        float leftover = accum - heals * WorldConstants.REGEN_INTERVAL;
        Hunger spent = spendForHeals(hunger, heals);
        return new TickResult(heals * WorldConstants.REGEN_AMOUNT, spent,
                new HungerTimers(timers.exhaustion(), leftover, 0f));
    }

    private static TickResult tickStarve(Hunger hunger, HungerTimers timers, float dt) {
        float accum = timers.starveAccum() + dt;
        int hits = (int) (accum / WorldConstants.STARVE_INTERVAL);
        float leftover = accum - hits * WorldConstants.STARVE_INTERVAL;
        return new TickResult(-hits * WorldConstants.STARVE_DAMAGE, hunger,
                new HungerTimers(timers.exhaustion(), 0f, leftover));
    }

    private static Hunger spendForHeals(Hunger hunger, int heals) {
        Hunger spent = hunger;
        for (int i = 0; i < heals; i++) spent = spendHungerCost(spent);
        return spent;
    }

    private static Hunger spendHungerCost(Hunger hunger) {
        float cost = WorldConstants.REGEN_HUNGER_COST;
        if (hunger.saturation() >= cost) {
            return new Hunger(hunger.food(), hunger.saturation() - cost);
        }
        float fromFood = cost - hunger.saturation();
        return new Hunger(Math.max(hunger.food() - (int) Math.ceil(fromFood), 0), 0f);
    }

    private static HungerTimers withResetHealthTimers(HungerTimers timers) {
        return new HungerTimers(timers.exhaustion(), 0f, 0f);
    }
}
