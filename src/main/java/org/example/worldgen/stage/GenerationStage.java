package org.example.worldgen.stage;

import org.example.components.VoxelChunkData;

// One ordered step of chunk generation (mirrors a Minecraft generation stage: noise, surface,
// carvers, features...). Each stage is a pure transform over the chunk's voxel data and its
// world position; stages hold only their own noise/config, never per-chunk state.
public interface GenerationStage {
    void apply(VoxelChunkData data, int chunkX, int chunkZ);
}
