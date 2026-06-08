package org.example.systems;

import org.example.components.VoxelChunkData;
import org.example.world.WorldConstants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WorldGenSystemTest {

    private static final int S    = WorldConstants.CHUNK_SIZE;
    private static final long SEED = WorldConstants.WORLD_SEED;

    private final WorldGenSystem gen = new WorldGenSystem(SEED);

    private VoxelChunkData generated(int cx, int cz) {
        VoxelChunkData data = VoxelChunkData.empty();
        gen.generateTerrain(data, cx, cz);
        return data;
    }

    @Test
    void surfaceBlockIsAlwaysGrass() {
        VoxelChunkData data = generated(0, 0);
        for (int bx = 0; bx < S; bx++) {
            for (int bz = 0; bz < S; bz++) {
                int surface = findSurfaceY(data, bx, bz);
                assertTrue(surface >= 0, "No solid block found in column (" + bx + "," + bz + ")");
                assertEquals(WorldConstants.BLOCK_GRASS, data.get(bx, surface, bz),
                        "Surface block at y=" + surface + " should be grass");
            }
        }
    }

    @Test
    void blockAboveSurfaceIsAlwaysAir() {
        VoxelChunkData data = generated(0, 0);
        for (int bx = 0; bx < S; bx++) {
            for (int bz = 0; bz < S; bz++) {
                int surface = findSurfaceY(data, bx, bz);
                for (int by = surface + 1; by < S; by++) {
                    assertEquals(WorldConstants.BLOCK_AIR, data.get(bx, by, bz),
                            "Expected air above surface at y=" + by);
                }
            }
        }
    }

    @Test
    void dirtLayerSitsDirectlyBelowGrass() {
        VoxelChunkData data = generated(0, 0);
        for (int bx = 0; bx < S; bx++) {
            for (int bz = 0; bz < S; bz++) {
                int surface = findSurfaceY(data, bx, bz);
                if (surface > 0) {
                    assertEquals(WorldConstants.BLOCK_DIRT, data.get(bx, surface - 1, bz),
                            "Block below grass should be dirt at y=" + (surface - 1));
                }
            }
        }
    }

    @Test
    void stoneAppearsDeepBelowSurface() {
        VoxelChunkData data = generated(0, 0);
        for (int bx = 0; bx < S; bx++) {
            for (int bz = 0; bz < S; bz++) {
                int surface = findSurfaceY(data, bx, bz);
                int deepY = surface - 4;
                if (deepY >= 0) {
                    assertEquals(WorldConstants.BLOCK_STONE, data.get(bx, deepY, bz),
                            "Expected stone 4 blocks below surface at y=" + deepY);
                }
            }
        }
    }

    @Test
    void sameSeadProducesSameChunk() {
        WorldGenSystem gen2 = new WorldGenSystem(SEED);
        VoxelChunkData a = generated(2, -1);
        VoxelChunkData b = VoxelChunkData.empty();
        gen2.generateTerrain(b, 2, -1);

        for (int y = 0; y < S; y++)
            for (int z = 0; z < S; z++)
                for (int x = 0; x < S; x++)
                    assertEquals(a.get(x, y, z), b.get(x, y, z),
                            "Mismatch at (" + x + "," + y + "," + z + ")");
    }

    @Test
    void terrainIsNotFlat() {
        VoxelChunkData data = generated(0, 0);
        int firstSurface = findSurfaceY(data, 0, 0);
        boolean foundVariation = false;
        outer:
        for (int bx = 0; bx < S; bx++) {
            for (int bz = 0; bz < S; bz++) {
                if (findSurfaceY(data, bx, bz) != firstSurface) {
                    foundVariation = true;
                    break outer;
                }
            }
        }
        assertTrue(foundVariation, "All columns have the same height — terrain is flat");
    }

    /** Returns the y of the highest non-air block in column (bx, bz), or -1 if all air. */
    private static int findSurfaceY(VoxelChunkData data, int bx, int bz) {
        for (int by = S - 1; by >= 0; by--) {
            if (data.get(bx, by, bz) != WorldConstants.BLOCK_AIR) return by;
        }
        return -1;
    }
}
