package org.example.worldgen.stage;

import org.example.components.VoxelChunkData;
import org.example.world.WorldConstants;

// Any water with air directly below pours straight down until it lands on a solid block, carving
// vertical waterfalls into the caves the carve stage opened beneath lakes. A single top-down sweep
// suffices: once a cell becomes water, the next (lower) cell sees it and keeps the column flowing.
public final class WaterSettleStage implements GenerationStage {

    @Override
    public void apply(VoxelChunkData data, int chunkX, int chunkZ) {
        int s = WorldConstants.CHUNK_SIZE_XZ;
        for (int bx = 0; bx < s; bx++) {
            for (int bz = 0; bz < s; bz++) {
                for (int y = WorldConstants.WORLD_HEIGHT - 1; y > 0; y--) {
                    if (data.get(bx, y, bz) != WorldConstants.BLOCK_WATER) continue;
                    if (data.get(bx, y - 1, bz) == WorldConstants.BLOCK_AIR) {
                        data.set(bx, y - 1, bz, WorldConstants.BLOCK_WATER);
                    }
                }
            }
        }
    }
}
