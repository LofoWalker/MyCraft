package org.example.worldgen;

import org.example.world.WorldConstants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TerrainShapeTest {

    private static final long SEED = WorldConstants.WORLD_SEED;

    private final TerrainShape shape = new TerrainShape(SEED);

    @Test
    void riverNeverRaisesTerrain() {
        for (int wx = -300; wx <= 300; wx += 7) {
            for (int wz = -300; wz <= 300; wz += 7) {
                int surface = 45;
                assertTrue(shape.carveRiver(surface, wx, wz) <= surface,
                        "River carving must never lift terrain at (" + wx + "," + wz + ")");
            }
        }
    }

    @Test
    void riversLeaveHighlandsUntouched() {
        int highland = WorldConstants.RIVER_MAX_ELEVATION + 1;
        for (int wx = -300; wx <= 300; wx += 5) {
            for (int wz = -300; wz <= 300; wz += 5) {
                assertEquals(highland, shape.carveRiver(highland, wx, wz),
                        "Highland surfaces must never be carved into rivers");
            }
        }
    }

    @Test
    void riversCarveChannelsBelowSeaLevel() {
        boolean foundChannel = false;
        outer:
        for (int wx = -400; wx <= 400; wx++) {
            for (int wz = -400; wz <= 400; wz += 3) {
                if (shape.carveRiver(WorldConstants.RIVER_MAX_ELEVATION, wx, wz) < WorldConstants.WATER_LEVEL) {
                    foundChannel = true;
                    break outer;
                }
            }
        }
        assertTrue(foundChannel, "Expected at least one river channel cut below sea level");
    }

    @Test
    void basinsNeverRaiseTerrain() {
        for (int wx = -300; wx <= 300; wx += 7) {
            for (int wz = -300; wz <= 300; wz += 7) {
                int surface = 45;
                assertTrue(shape.carveBasin(surface, wx, wz) <= surface,
                        "Basin carving must never lift terrain at (" + wx + "," + wz + ")");
            }
        }
    }

    @Test
    void basinsLeaveHighlandsUntouched() {
        int highland = WorldConstants.RIVER_MAX_ELEVATION + 1;
        for (int wx = -300; wx <= 300; wx += 5) {
            for (int wz = -300; wz <= 300; wz += 5) {
                assertEquals(highland, shape.carveBasin(highland, wx, wz),
                        "Highland surfaces must never be dented into basins");
            }
        }
    }

    @Test
    void basinsSinkSomeLowlandBelowSeaLevel() {
        boolean foundBasin = false;
        outer:
        for (int wx = -400; wx <= 400; wx += 2) {
            for (int wz = -400; wz <= 400; wz += 2) {
                if (shape.carveBasin(WorldConstants.WATER_LEVEL, wx, wz) < WorldConstants.WATER_LEVEL) {
                    foundBasin = true;
                    break outer;
                }
            }
        }
        assertTrue(foundBasin, "Expected at least one basin to pool below sea level");
    }
}
