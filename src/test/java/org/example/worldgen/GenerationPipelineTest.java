package org.example.worldgen;

import org.example.components.VoxelChunkData;
import org.example.world.WorldConstants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GenerationPipelineTest {

    private static final int  SX   = WorldConstants.CHUNK_SIZE_XZ;
    private static final long SEED = WorldConstants.WORLD_SEED;

    private static VoxelChunkData generated(GenerationPipeline pipeline, int cx, int cz) {
        VoxelChunkData data = VoxelChunkData.empty();
        pipeline.generate(data, cx, cz);
        return data;
    }

    // The surface of a grass column is its highest non-air block; air sits directly above it and
    // stone lies somewhere beneath. Finding it bottom-up keeps the test independent of the exact
    // surface altitude, which the noise field is free to change.
    @Test
    void overworldColumnHasStoneDepthGrassSurfaceAndAirAbove() {
        VoxelChunkData data = generated(GenerationPipeline.overworld(SEED), 0, 0);
        boolean checkedGrassColumn = false;
        for (int bx = 0; bx < SX && !checkedGrassColumn; bx++) {
            for (int bz = 0; bz < SX && !checkedGrassColumn; bz++) {
                int surfaceY = topSolidY(data, bx, bz);
                if (data.get(bx, surfaceY, bz) != WorldConstants.BLOCK_GRASS) continue;
                assertEquals(WorldConstants.BLOCK_AIR, data.get(bx, surfaceY + 1, bz),
                        "Expected air directly above the grass surface");
                assertTrue(hasStoneBelow(data, bx, bz, surfaceY),
                        "Expected stone somewhere below the grass surface");
                checkedGrassColumn = true;
            }
        }
        assertTrue(checkedGrassColumn, "Expected at least one grass-topped column in the chunk");
    }

    @Test
    void sameSeedProducesIdenticalChunks() {
        VoxelChunkData a = generated(GenerationPipeline.overworld(SEED), 3, -2);
        VoxelChunkData b = generated(GenerationPipeline.overworld(SEED), 3, -2);
        assertArrayEquals(a.blocks(), b.blocks());
    }

    @Test
    void differentSeedsProduceDifferentChunks() {
        VoxelChunkData a = generated(GenerationPipeline.overworld(SEED), 0, 0);
        VoxelChunkData b = generated(GenerationPipeline.overworld(SEED + 1), 0, 0);
        assertFalse(java.util.Arrays.equals(a.blocks(), b.blocks()));
    }

    private static int topSolidY(VoxelChunkData data, int bx, int bz) {
        for (int by = WorldConstants.WORLD_HEIGHT - 1; by >= 0; by--) {
            if (data.get(bx, by, bz) != WorldConstants.BLOCK_AIR) return by;
        }
        return 0;
    }

    private static boolean hasStoneBelow(VoxelChunkData data, int bx, int bz, int surfaceY) {
        for (int by = surfaceY - 1; by >= 0; by--) {
            if (data.get(bx, by, bz) == WorldConstants.BLOCK_STONE) return true;
        }
        return false;
    }
}
