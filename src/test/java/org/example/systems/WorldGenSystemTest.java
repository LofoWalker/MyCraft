package org.example.systems;

import org.example.components.VoxelChunkData;
import org.example.world.WorldConstants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WorldGenSystemTest {

    private static final int  SX   = WorldConstants.CHUNK_SIZE_XZ;
    private static final int  H    = WorldConstants.WORLD_HEIGHT;
    private static final long SEED = WorldConstants.WORLD_SEED;

    private final WorldGenSystem gen = new WorldGenSystem(SEED);

    private VoxelChunkData generated(int cx, int cz) {
        VoxelChunkData data = VoxelChunkData.empty();
        gen.generateTerrain(data, cx, cz);
        return data;
    }

    private VoxelChunkData withTrees(int cx, int cz) {
        VoxelChunkData data = VoxelChunkData.empty();
        gen.generateTerrain(data, cx, cz);
        gen.plantTrees(data, cx, cz);
        return data;
    }

    private static final int CAVE_FLOOR = 4;

    @Test
    void carvingOnlyRemovesBlocks() {
        VoxelChunkData before = generated(0, 0);
        byte[] terrain = before.blocks().clone();
        VoxelChunkData after = generated(0, 0);
        gen.carveCaves(after, 0, 0);
        for (int i = 0; i < terrain.length; i++) {
            byte original = terrain[i];
            byte carved   = after.blocks()[i];
            assertTrue(carved == original || carved == WorldConstants.BLOCK_AIR,
                    "Carving turned block " + original + " into " + carved + " (only air allowed)");
        }
    }

    @Test
    void carvingNeverRemovesWater() {
        VoxelChunkData before = generated(0, 0);
        byte[] terrain = before.blocks().clone();
        VoxelChunkData after = generated(0, 0);
        gen.carveCaves(after, 0, 0);
        for (int i = 0; i < terrain.length; i++) {
            if (terrain[i] == WorldConstants.BLOCK_WATER) {
                assertEquals(WorldConstants.BLOCK_WATER, after.blocks()[i],
                        "Water at index " + i + " must survive carving");
            }
        }
    }

    @Test
    void caveFloorLayersAreNeverCarved() {
        VoxelChunkData before = generated(0, 0);
        VoxelChunkData after  = generated(0, 0);
        gen.carveCaves(after, 0, 0);
        for (int y = 0; y < CAVE_FLOOR; y++) {
            for (int x = 0; x < SX; x++) {
                for (int z = 0; z < SX; z++) {
                    assertEquals(before.get(x, y, z), after.get(x, y, z),
                            "Block in protected floor layer y=" + y + " was modified");
                }
            }
        }
    }

    @Test
    void cavesCarveSomeAirIntoTheRegion() {
        boolean carvedSomething = false;
        outer:
        for (int cx = -3; cx <= 3; cx++) {
            for (int cz = -3; cz <= 3; cz++) {
                VoxelChunkData before = generated(cx, cz);
                byte[] terrain = before.blocks().clone();
                VoxelChunkData after = generated(cx, cz);
                gen.carveCaves(after, cx, cz);
                for (int i = 0; i < terrain.length; i++) {
                    if (terrain[i] != WorldConstants.BLOCK_AIR
                            && after.blocks()[i] == WorldConstants.BLOCK_AIR) {
                        carvedSomething = true;
                        break outer;
                    }
                }
            }
        }
        assertTrue(carvedSomething, "Expected caves to hollow out some solid blocks in the scanned region");
    }

    @Test
    void cavesAreDeterministicForSameSeed() {
        VoxelChunkData a = generated(2, -1);
        gen.carveCaves(a, 2, -1);
        WorldGenSystem gen2 = new WorldGenSystem(SEED);
        VoxelChunkData b = VoxelChunkData.empty();
        gen2.generateTerrain(b, 2, -1);
        gen2.carveCaves(b, 2, -1);
        assertArrayEquals(a.blocks(), b.blocks(), "Same seed must carve identical caves");
    }

    private VoxelChunkData carvedAndSettled(int cx, int cz) {
        VoxelChunkData data = generated(cx, cz);
        gen.carveCaves(data, cx, cz);
        gen.settleWater(data);
        return data;
    }

    @Test
    void settledWaterNeverHasAirDirectlyBelow() {
        for (int cx = -3; cx <= 3; cx++) {
            for (int cz = -3; cz <= 3; cz++) {
                VoxelChunkData data = carvedAndSettled(cx, cz);
                for (int x = 0; x < SX; x++) {
                    for (int z = 0; z < SX; z++) {
                        for (int y = 1; y < H; y++) {
                            if (data.get(x, y, z) == WorldConstants.BLOCK_WATER) {
                                assertNotEquals(WorldConstants.BLOCK_AIR, data.get(x, y - 1, z),
                                        "Water at (" + x + "," + y + "," + z + ") still has air below");
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    void settlingOnlyTurnsAirIntoWater() {
        VoxelChunkData before = generated(0, 0);
        gen.carveCaves(before, 0, 0);
        byte[] carved = before.blocks().clone();
        VoxelChunkData after = generated(0, 0);
        gen.carveCaves(after, 0, 0);
        gen.settleWater(after);
        for (int i = 0; i < carved.length; i++) {
            if (carved[i] != after.blocks()[i]) {
                assertEquals(WorldConstants.BLOCK_AIR, carved[i],
                        "Settling changed a non-air block at index " + i);
                assertEquals(WorldConstants.BLOCK_WATER, after.blocks()[i],
                        "Settling produced something other than water at index " + i);
            }
        }
    }

    @Test
    void waterCascadesDownIntoCarvedAir() {
        VoxelChunkData data = VoxelChunkData.empty();
        int x = 5, z = 5;
        data.set(x, 50, z, WorldConstants.BLOCK_WATER);
        data.set(x, 47, z, WorldConstants.BLOCK_STONE);
        gen.settleWater(data);
        assertEquals(WorldConstants.BLOCK_WATER, data.get(x, 49, z), "Water should fall one block");
        assertEquals(WorldConstants.BLOCK_WATER, data.get(x, 48, z), "Water should keep cascading");
        assertEquals(WorldConstants.BLOCK_STONE, data.get(x, 47, z), "Cascade stops on the solid floor");
        assertEquals(WorldConstants.BLOCK_AIR, data.get(x, 46, z), "Water must not pass through solid");
    }

    @Test
    void settlingIsDeterministicForSameSeed() {
        VoxelChunkData a = carvedAndSettled(1, 2);
        VoxelChunkData b = carvedAndSettled(1, 2);
        assertArrayEquals(a.blocks(), b.blocks(), "Settling must be deterministic");
    }

    @Test
    void riverNeverRaisesTerrain() {
        for (int wx = -300; wx <= 300; wx += 7) {
            for (int wz = -300; wz <= 300; wz += 7) {
                int surface = 45;
                assertTrue(gen.carveRiver(surface, wx, wz) <= surface,
                        "River carving must never lift terrain at (" + wx + "," + wz + ")");
            }
        }
    }

    @Test
    void riversLeaveHighlandsUntouched() {
        int highland = WorldConstants.RIVER_MAX_ELEVATION + 1;
        for (int wx = -300; wx <= 300; wx += 5) {
            for (int wz = -300; wz <= 300; wz += 5) {
                assertEquals(highland, gen.carveRiver(highland, wx, wz),
                        "Highland surfaces must never be carved into rivers");
            }
        }
    }

    @Test
    void basinsNeverRaiseTerrain() {
        for (int wx = -300; wx <= 300; wx += 7) {
            for (int wz = -300; wz <= 300; wz += 7) {
                int surface = 45;
                assertTrue(gen.carveBasin(surface, wx, wz) <= surface,
                        "Basin carving must never lift terrain at (" + wx + "," + wz + ")");
            }
        }
    }

    @Test
    void basinsLeaveHighlandsUntouched() {
        int highland = WorldConstants.RIVER_MAX_ELEVATION + 1;
        for (int wx = -300; wx <= 300; wx += 5) {
            for (int wz = -300; wz <= 300; wz += 5) {
                assertEquals(highland, gen.carveBasin(highland, wx, wz),
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
                if (gen.carveBasin(WorldConstants.WATER_LEVEL, wx, wz) < WorldConstants.WATER_LEVEL) {
                    foundBasin = true;
                    break outer;
                }
            }
        }
        assertTrue(foundBasin, "Expected at least one basin to pool below sea level");
    }

    @Test
    void riversCarveChannelsBelowSeaLevel() {
        boolean foundChannel = false;
        outer:
        for (int wx = -400; wx <= 400; wx++) {
            for (int wz = -400; wz <= 400; wz += 3) {
                if (gen.carveRiver(WorldConstants.RIVER_MAX_ELEVATION, wx, wz) < WorldConstants.WATER_LEVEL) {
                    foundChannel = true;
                    break outer;
                }
            }
        }
        assertTrue(foundChannel, "Expected at least one river channel cut below sea level");
    }

    @Test
    void topBlockDependsOnAltitude() {
        assertEquals(WorldConstants.BLOCK_GRASS,
                WorldGenSystem.topBlock(WorldConstants.WATER_LEVEL + 3), "Dry land is grass");
        assertEquals(WorldConstants.BLOCK_DIRT,
                WorldGenSystem.topBlock(WorldConstants.WATER_LEVEL), "Submerged surface is dirt");
        assertEquals(WorldConstants.BLOCK_DIRT,
                WorldGenSystem.topBlock(WorldConstants.WATER_LEVEL - 4), "Lake bed is dirt");
        assertEquals(WorldConstants.BLOCK_STONE,
                WorldGenSystem.topBlock(WorldConstants.ROCK_LEVEL), "Mountain peak is bare stone");
    }

    @Test
    void columnSurfaceMatchesAltitudeRule() {
        VoxelChunkData data = generated(0, 0);
        for (int bx = 0; bx < SX; bx++) {
            for (int bz = 0; bz < SX; bz++) {
                int surface = terrainSurfaceY(data, bx, bz);
                assertTrue(surface >= 0, "No terrain in column (" + bx + "," + bz + ")");
                assertEquals(WorldGenSystem.topBlock(surface), data.get(bx, surface, bz),
                        "Surface block at y=" + surface + " disagrees with altitude rule");
            }
        }
    }

    @Test
    void dirtLayerSitsDirectlyBelowGrass() {
        VoxelChunkData data = generated(0, 0);
        for (int bx = 0; bx < SX; bx++) {
            for (int bz = 0; bz < SX; bz++) {
                int surface = terrainSurfaceY(data, bx, bz);
                if (surface > 0 && data.get(bx, surface, bz) == WorldConstants.BLOCK_GRASS) {
                    assertEquals(WorldConstants.BLOCK_DIRT, data.get(bx, surface - 1, bz),
                            "Block below grass should be dirt at y=" + (surface - 1));
                }
            }
        }
    }

    @Test
    void stoneAppearsDeepBelowSurface() {
        VoxelChunkData data = generated(0, 0);
        for (int bx = 0; bx < SX; bx++) {
            for (int bz = 0; bz < SX; bz++) {
                int surface = terrainSurfaceY(data, bx, bz);
                int deepY = surface - 4;
                if (deepY >= 0) {
                    assertEquals(WorldConstants.BLOCK_STONE, data.get(bx, deepY, bz),
                            "Expected stone 4 blocks below surface at y=" + deepY);
                }
            }
        }
    }

    @Test
    void waterFillsValleysUpToSeaLevel() {
        VoxelChunkData data = generated(0, 0);
        for (int bx = 0; bx < SX; bx++) {
            for (int bz = 0; bz < SX; bz++) {
                int surface = terrainSurfaceY(data, bx, bz);
                for (int by = surface + 1; by <= WorldConstants.WATER_LEVEL; by++) {
                    assertEquals(WorldConstants.BLOCK_WATER, data.get(bx, by, bz),
                            "Expected water between surface and sea level at y=" + by);
                }
                int airStart = Math.max(surface, WorldConstants.WATER_LEVEL) + 1;
                for (int by = airStart; by < H; by++) {
                    assertEquals(WorldConstants.BLOCK_AIR, data.get(bx, by, bz),
                            "Expected air above terrain/sea level at y=" + by);
                }
            }
        }
    }

    @Test
    void worldContainsWaterAndStonePeaks() {
        boolean foundWater = false;
        boolean foundPeak  = false;
        outer:
        for (int cx = -4; cx <= 4; cx++) {
            for (int cz = -4; cz <= 4; cz++) {
                VoxelChunkData data = generated(cx, cz);
                for (int bx = 0; bx < SX && !(foundWater && foundPeak); bx++)
                    for (int bz = 0; bz < SX; bz++) {
                        int surface = terrainSurfaceY(data, bx, bz);
                        if (data.get(bx, WorldConstants.WATER_LEVEL, bz) == WorldConstants.BLOCK_WATER) foundWater = true;
                        if (surface >= WorldConstants.ROCK_LEVEL) foundPeak = true;
                    }
                if (foundWater && foundPeak) break outer;
            }
        }
        assertTrue(foundWater, "Expected water somewhere in the scanned region");
        assertTrue(foundPeak,  "Expected stone mountain peaks somewhere in the scanned region");
    }

    @Test
    void differentSeedsProduceDifferentTerrain() {
        VoxelChunkData a = generated(0, 0);
        WorldGenSystem other = new WorldGenSystem(SEED + 1);
        VoxelChunkData b = VoxelChunkData.empty();
        other.generateTerrain(b, 0, 0);
        assertFalse(java.util.Arrays.equals(a.blocks(), b.blocks()),
                "Distinct seeds should yield distinct terrain");
    }

    @Test
    void sameSeadProducesSameChunk() {
        WorldGenSystem gen2 = new WorldGenSystem(SEED);
        VoxelChunkData a = generated(2, -1);
        VoxelChunkData b = VoxelChunkData.empty();
        gen2.generateTerrain(b, 2, -1);

        for (int y = 0; y < H; y++)
            for (int z = 0; z < SX; z++)
                for (int x = 0; x < SX; x++)
                    assertEquals(a.get(x, y, z), b.get(x, y, z),
                            "Mismatch at (" + x + "," + y + "," + z + ")");
    }

    @Test
    void terrainIsNotFlat() {
        VoxelChunkData data = generated(0, 0);
        int firstSurface = terrainSurfaceY(data, 0, 0);
        boolean foundVariation = false;
        outer:
        for (int bx = 0; bx < SX; bx++) {
            for (int bz = 0; bz < SX; bz++) {
                if (terrainSurfaceY(data, bx, bz) != firstSurface) {
                    foundVariation = true;
                    break outer;
                }
            }
        }
        assertTrue(foundVariation, "All columns have the same height — terrain is flat");
    }

    @Test
    void treesProduceWoodAndLeaves() {
        boolean foundWood   = false;
        boolean foundLeaves = false;
        outer:
        for (int cx = 0; cx < 4 && !(foundWood && foundLeaves); cx++) {
            for (int cz = 0; cz < 4; cz++) {
                VoxelChunkData data = withTrees(cx, cz);
                for (int b : data.blocks()) {
                    if (b == WorldConstants.BLOCK_WOOD)   foundWood   = true;
                    if (b == WorldConstants.BLOCK_LEAVES) foundLeaves = true;
                }
                if (foundWood && foundLeaves) break outer;
            }
        }
        assertTrue(foundWood,   "Expected at least one wood block across the scanned chunks");
        assertTrue(foundLeaves, "Expected at least one leaf block across the scanned chunks");
    }

    @Test
    void trunksRiseFromGrass() {
        VoxelChunkData data = firstChunkWithTree();
        for (int bx = 0; bx < SX; bx++) {
            for (int bz = 0; bz < SX; bz++) {
                int lowestWood = lowestWoodY(data, bx, bz);
                if (lowestWood < 0) continue;
                assertEquals(WorldConstants.BLOCK_GRASS, data.get(bx, lowestWood - 1, bz),
                        "Trunk base at (" + bx + "," + bz + ") should stand on grass");
            }
        }
    }

    @Test
    void treesAreDeterministicForSameSeed() {
        VoxelChunkData a = withTrees(1, 2);
        WorldGenSystem gen2 = new WorldGenSystem(SEED);
        VoxelChunkData b = VoxelChunkData.empty();
        gen2.generateTerrain(b, 1, 2);
        gen2.plantTrees(b, 1, 2);
        assertArrayEquals(a.blocks(), b.blocks());
    }

    @Test
    void plantTreesStaysWithinChunkBounds() {
        assertDoesNotThrow(() -> {
            for (int cx = -2; cx <= 2; cx++)
                for (int cz = -2; cz <= 2; cz++)
                    withTrees(cx, cz);
        });
    }

    private VoxelChunkData firstChunkWithTree() {
        for (int cx = 0; cx < 6; cx++) {
            for (int cz = 0; cz < 6; cz++) {
                VoxelChunkData data = withTrees(cx, cz);
                for (int b : data.blocks()) {
                    if (b == WorldConstants.BLOCK_WOOD) return data;
                }
            }
        }
        throw new AssertionError("No tree found in any scanned chunk");
    }

    private static int lowestWoodY(VoxelChunkData data, int bx, int bz) {
        for (int by = 1; by < H; by++) {
            if (data.get(bx, by, bz) == WorldConstants.BLOCK_WOOD) return by;
        }
        return -1;
    }

    /** Returns the y of the highest solid terrain block (ignoring air and water), or -1 if none. */
    private static int terrainSurfaceY(VoxelChunkData data, int bx, int bz) {
        for (int by = H - 1; by >= 0; by--) {
            byte b = data.get(bx, by, bz);
            if (b != WorldConstants.BLOCK_AIR && b != WorldConstants.BLOCK_WATER) return by;
        }
        return -1;
    }
}
