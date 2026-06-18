package org.example.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Tests the pure hostile-spawn eligibility rules (light, distance, population cap). */
class HostileSpawnSystemTest {

    private static final double FAR = WorldConstants.MOB_SPAWN_MIN_RADIUS + 10;

    @Test
    void spawnsInDarknessFarFromPlayerUnderCap() {
        assertTrue(HostileSpawnRules.canSpawn(0, FAR, 0), "dark, distant, empty area should spawn");
    }

    @Test
    void refusedWhenTooBright() {
        int bright = WorldConstants.HOSTILE_SPAWN_MAX_LIGHT + 1;
        assertFalse(HostileSpawnRules.canSpawn(bright, FAR, 0), "bright cells must not spawn hostiles");
    }

    @Test
    void refusedWithinMinimumRadius() {
        double near = WorldConstants.MOB_SPAWN_MIN_RADIUS - 1;
        assertFalse(HostileSpawnRules.canSpawn(0, near, 0), "must not spawn right next to the player");
    }

    @Test
    void refusedAtPopulationCap() {
        int atCap = WorldConstants.MAX_HOSTILE_PER_AREA;
        assertFalse(HostileSpawnRules.canSpawn(0, FAR, atCap), "must not exceed the population cap");
    }
}
