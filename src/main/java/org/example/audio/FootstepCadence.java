package org.example.audio;

/**
 * Pure-logic helper that decides when to fire a footstep sound.
 *
 * <p>A footstep fires when the accumulated distance travelled on the ground
 * exceeds {@link #STEP_DISTANCE_METRES}. Distance is derived from horizontal
 * speed × dt each tick. This class carries no AL state, making it fully
 * testable without a real audio device.
 */
public final class FootstepCadence {

    /** How far (in world units / metres) the player walks between step sounds. */
    public static final float STEP_DISTANCE_METRES = 2.0f;

    private float distanceAccum = 0f;

    /**
     * Advances the cadence by one simulation tick.
     *
     * @param horizontalSpeed magnitude of horizontal velocity (metres/second)
     * @param dt              elapsed time for this tick (seconds)
     * @param grounded        true when the entity is standing on solid ground
     * @return true if a step sound should play this tick
     */
    public boolean tick(float horizontalSpeed, float dt, boolean grounded) {
        if (!grounded || horizontalSpeed <= 0f) {
            return false;
        }
        distanceAccum += horizontalSpeed * dt;
        if (distanceAccum >= STEP_DISTANCE_METRES) {
            distanceAccum -= STEP_DISTANCE_METRES;
            return true;
        }
        return false;
    }

    /** Resets accumulated distance — call when leaving the ground. */
    public void reset() {
        distanceAccum = 0f;
    }
}
