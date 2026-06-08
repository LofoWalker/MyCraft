package org.example.components;

import org.example.world.WorldConstants;

public record VoxelChunkData(byte[] blocks) {

    public static VoxelChunkData empty() {
        return new VoxelChunkData(new byte[WorldConstants.CHUNK_SIZE * WorldConstants.CHUNK_SIZE * WorldConstants.CHUNK_SIZE]);
    }

    public byte get(int x, int y, int z) {
        return blocks[x + y * WorldConstants.CHUNK_SIZE + z * WorldConstants.CHUNK_SIZE * WorldConstants.CHUNK_SIZE];
    }

    public void set(int x, int y, int z, byte blockId) {
        blocks[x + y * WorldConstants.CHUNK_SIZE + z * WorldConstants.CHUNK_SIZE * WorldConstants.CHUNK_SIZE] = blockId;
    }
}
