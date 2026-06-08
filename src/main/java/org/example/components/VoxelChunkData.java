package org.example.components;

import org.example.world.WorldConstants;

public record VoxelChunkData(byte[] blocks) {

    public static VoxelChunkData empty() {
        int sx = WorldConstants.CHUNK_SIZE_XZ;
        return new VoxelChunkData(new byte[sx * sx * WorldConstants.WORLD_HEIGHT]);
    }

    public byte get(int x, int y, int z) {
        return blocks[index(x, y, z)];
    }

    public void set(int x, int y, int z, byte blockId) {
        blocks[index(x, y, z)] = blockId;
    }

    // Y is the outermost dimension: each horizontal layer is contiguous, which keeps
    // vertical passes (cave carving, ore placement) cache-friendly.
    private static int index(int x, int y, int z) {
        int sx = WorldConstants.CHUNK_SIZE_XZ;
        return x + z * sx + y * sx * sx;
    }
}
