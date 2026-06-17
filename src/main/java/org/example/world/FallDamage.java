package org.example.world;

/**
 * Pure fall-damage math, isolated from the ECS/World and OpenGL so it can be unit-tested directly.
 * A landing below {@link WorldConstants#SAFE_FALL_SPEED} is harmless; above it, damage grows linearly
 * with the excess impact speed and is rounded to whole hearts.
 */
public final class FallDamage {

    private FallDamage() {}

    /**
     * @param impactSpeed downward speed at the moment of landing, as a positive magnitude (m/s).
     * @return whole points of damage, never negative; 0 for any landing at or below the safe speed.
     */
    public static int fromImpactSpeed(float impactSpeed) {
        float excess = impactSpeed - WorldConstants.SAFE_FALL_SPEED;
        if (excess <= 0f) return 0;
        return Math.round(excess * WorldConstants.FALL_DAMAGE_PER_SPEED);
    }
}
