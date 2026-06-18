package org.example.world;

/**
 * Pure eligibility rules for spawning a hostile mob at a candidate cell. No World, no GL —
 * the {@link org.example.systems.HostileSpawnSystem} gathers the inputs and calls these.
 */
public final class HostileSpawnRules {

    private HostileSpawnRules() {}

    /**
     * A hostile mob may spawn when the cell is dark enough, far enough from the player, and the
     * area is below the hostile population cap.
     *
     * @param lightLevel        effective light at the candidate cell (0..MAX_LIGHT_LEVEL)
     * @param distanceToPlayer  distance from the candidate to the player (blocks)
     * @param currentPopulation number of hostile mobs already loaded in the area
     */
    public static boolean canSpawn(int lightLevel, double distanceToPlayer, int currentPopulation) {
        return lightLevel <= WorldConstants.HOSTILE_SPAWN_MAX_LIGHT
            && distanceToPlayer >= WorldConstants.MOB_SPAWN_MIN_RADIUS
            && currentPopulation < WorldConstants.MAX_HOSTILE_PER_AREA;
    }
}
