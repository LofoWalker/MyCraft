package org.example.worldgen;

import org.example.components.VoxelChunkData;
import org.example.world.WorldConstants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GenerationPipelineTest {

    private static final long SEED = WorldConstants.WORLD_SEED;

    private static VoxelChunkData generated(GenerationPipeline pipeline, int cx, int cz) {
        VoxelChunkData data = VoxelChunkData.empty();
        pipeline.generate(data, cx, cz);
        return data;
    }

    // FNV-1a checksum captured from the pre-refactor generator (terrain -> caves -> settle ->
    // trees). Pins the full pipeline output so the subsystem extraction stays behavior-identical.
    private static long checksum(byte[] blocks) {
        long h = 1469598103934665603L;
        for (byte b : blocks) {
            h ^= (b & 0xFF);
            h *= 1099511628211L;
        }
        return h;
    }

    @Test
    void pipelineMatchesGoldenOutput() {
        GenerationPipeline pipeline = GenerationPipeline.overworld(SEED);
        assertEquals(-3006615168672931192L, checksum(generated(pipeline, 0, 0).blocks()));
        assertEquals(3404619738122486451L,  checksum(generated(pipeline, 2, -1).blocks()));
        assertEquals(-4434057090908753661L, checksum(generated(pipeline, -3, 4).blocks()));
        assertEquals(-6021431298548747370L, checksum(generated(pipeline, 5, 5).blocks()));
        assertEquals(8268375569596622951L,  checksum(generated(pipeline, -2, -2).blocks()));
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
}
