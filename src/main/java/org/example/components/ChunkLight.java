package org.example.components;

import org.example.world.WorldConstants;

/**
 * Baked per-cell light for one chunk: one byte per voxel (high nibble skylight, low nibble blocklight),
 * sharing {@link VoxelChunkData}'s flat indexing. Produced by
 * {@link org.example.world.LightEngine#computeLight} on the chunk workers and read by the meshing to
 * shade exposed faces. Data-only; the engine owns all packing/unpacking logic.
 */
public record ChunkLight(byte[] light) {

    public static ChunkLight empty() {
        int sx = WorldConstants.CHUNK_SIZE_XZ;
        return new ChunkLight(new byte[sx * sx * WorldConstants.WORLD_HEIGHT]);
    }
}
