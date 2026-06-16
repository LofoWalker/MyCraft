package org.example.worldgen.stage;

import org.example.components.VoxelChunkData;
import org.example.world.WorldConstants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OreStageTest {

    private static final int  SX   = WorldConstants.CHUNK_SIZE_XZ;
    private static final long SEED = WorldConstants.WORLD_SEED;

    private final FlatTerrainStage terrain = new FlatTerrainStage(SEED);
    private final OreStage         ores    = new OreStage(SEED);

    private VoxelChunkData withOres(int cx, int cz) {
        VoxelChunkData data = VoxelChunkData.empty();
        terrain.apply(data, cx, cz);
        ores.apply(data, cx, cz);
        return data;
    }

    @Test
    void scattersIronAndDiamondAcrossChunks() {
        boolean foundIron    = false;
        boolean foundDiamond = false;
        for (int cx = 0; cx < 4 && !(foundIron && foundDiamond); cx++) {
            for (int cz = 0; cz < 4; cz++) {
                VoxelChunkData data = withOres(cx, cz);
                for (int b : data.blocks()) {
                    if (b == WorldConstants.BLOCK_IRON)    foundIron    = true;
                    if (b == WorldConstants.BLOCK_DIAMOND) foundDiamond = true;
                }
            }
        }
        assertTrue(foundIron,    "Expected at least one iron block across the scanned chunks");
        assertTrue(foundDiamond, "Expected at least one diamond block across the scanned chunks");
    }

    @Test
    void diamondStaysWithinDeepLayers() {
        for (int cx = -2; cx <= 2; cx++) {
            for (int cz = -2; cz <= 2; cz++) {
                VoxelChunkData data = withOres(cx, cz);
                for (int bx = 0; bx < SX; bx++) {
                    for (int bz = 0; bz < SX; bz++) {
                        for (int by = WorldConstants.DIAMOND_MAX_LEVEL + 1; by < WorldConstants.WORLD_HEIGHT; by++) {
                            assertNotEquals(WorldConstants.BLOCK_DIAMOND, data.get(bx, by, bz),
                                    "Diamond above DIAMOND_MAX_LEVEL at y=" + by);
                        }
                    }
                }
            }
        }
    }

    @Test
    void oreOnlyReplacesStoneAndLeavesSurfaceIntact() {
        VoxelChunkData data = withOres(1, 2);
        for (int bx = 0; bx < SX; bx++) {
            for (int bz = 0; bz < SX; bz++) {
                byte cap = data.get(bx, WorldConstants.FLAT_SURFACE_LEVEL, bz);
                assertNotEquals(WorldConstants.BLOCK_IRON,    cap, "Surface cap should never be ore");
                assertNotEquals(WorldConstants.BLOCK_DIAMOND, cap, "Surface cap should never be ore");
            }
        }
    }

    @Test
    void isDeterministicForSameSeed() {
        VoxelChunkData a = withOres(3, -2);
        VoxelChunkData b = withOres(3, -2);
        assertArrayEquals(a.blocks(), b.blocks());
    }
}
