package org.example.worldgen.stage;

import org.example.components.VoxelChunkData;
import org.example.world.WorldConstants;
import org.example.worldgen.TerrainShape;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TerrainStageTest {

    private static final int  SX   = WorldConstants.CHUNK_SIZE_XZ;
    private static final int  H    = WorldConstants.WORLD_HEIGHT;
    private static final long SEED = WorldConstants.WORLD_SEED;

    private final TerrainStage terrain = new TerrainStage(new TerrainShape(SEED));

    private VoxelChunkData generated(int cx, int cz) {
        VoxelChunkData data = VoxelChunkData.empty();
        terrain.apply(data, cx, cz);
        return data;
    }

    @Test
    void topBlockDependsOnAltitude() {
        assertEquals(WorldConstants.BLOCK_GRASS,
                TerrainStage.topBlock(WorldConstants.WATER_LEVEL + 3), "Dry land is grass");
        assertEquals(WorldConstants.BLOCK_DIRT,
                TerrainStage.topBlock(WorldConstants.WATER_LEVEL), "Submerged surface is dirt");
        assertEquals(WorldConstants.BLOCK_DIRT,
                TerrainStage.topBlock(WorldConstants.WATER_LEVEL - 4), "Lake bed is dirt");
        assertEquals(WorldConstants.BLOCK_STONE,
                TerrainStage.topBlock(WorldConstants.ROCK_LEVEL), "Mountain peak is bare stone");
    }

    @Test
    void columnSurfaceMatchesAltitudeRule() {
        VoxelChunkData data = generated(0, 0);
        for (int bx = 0; bx < SX; bx++) {
            for (int bz = 0; bz < SX; bz++) {
                int surface = terrainSurfaceY(data, bx, bz);
                assertTrue(surface >= 0, "No terrain in column (" + bx + "," + bz + ")");
                assertEquals(TerrainStage.topBlock(surface), data.get(bx, surface, bz),
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
        TerrainStage other = new TerrainStage(new TerrainShape(SEED + 1));
        VoxelChunkData b = VoxelChunkData.empty();
        other.apply(b, 0, 0);
        assertFalse(java.util.Arrays.equals(a.blocks(), b.blocks()),
                "Distinct seeds should yield distinct terrain");
    }

    @Test
    void sameSeedProducesSameChunk() {
        TerrainStage other = new TerrainStage(new TerrainShape(SEED));
        VoxelChunkData a = generated(2, -1);
        VoxelChunkData b = VoxelChunkData.empty();
        other.apply(b, 2, -1);
        assertArrayEquals(a.blocks(), b.blocks());
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

    /** Returns the y of the highest solid terrain block (ignoring air and water), or -1 if none. */
    private static int terrainSurfaceY(VoxelChunkData data, int bx, int bz) {
        for (int by = H - 1; by >= 0; by--) {
            byte b = data.get(bx, by, bz);
            if (b != WorldConstants.BLOCK_AIR && b != WorldConstants.BLOCK_WATER) return by;
        }
        return -1;
    }
}
