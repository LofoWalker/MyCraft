package org.example.worldgen.stage;

import org.example.components.VoxelChunkData;
import org.example.world.WorldConstants;
import org.example.worldgen.SpatialHash;

// Placeholder world while real terrain generation is on hold: a flat plain at FLAT_SURFACE_LEVEL,
// solid stone underneath, with each surface column randomly capped by dirt, stone or water.
public final class FlatTerrainStage implements GenerationStage {

    // Relative weights of the random surface caps; their sum is the draw range. Dirt dominates so
    // trees (which only root on dirt) have room, with sparser stone outcrops and water pools.
    private static final int DIRT_WEIGHT  = 6;
    private static final int STONE_WEIGHT = 2;
    private static final int WATER_WEIGHT = 2;
    private static final int WEIGHT_TOTAL = DIRT_WEIGHT + STONE_WEIGHT + WATER_WEIGHT;

    private final long seed;

    public FlatTerrainStage(long seed) {
        this.seed = seed;
    }

    @Override
    public void apply(VoxelChunkData data, int chunkX, int chunkZ) {
        int s = WorldConstants.CHUNK_SIZE_XZ;
        for (int bx = 0; bx < s; bx++) {
            for (int bz = 0; bz < s; bz++) {
                fillColumn(data, bx, bz, surfaceCap(chunkX * s + bx, chunkZ * s + bz));
            }
        }
    }

    private static void fillColumn(VoxelChunkData data, int bx, int bz, byte cap) {
        for (int by = 0; by < WorldConstants.FLAT_SURFACE_LEVEL; by++) {
            data.set(bx, by, bz, WorldConstants.BLOCK_STONE);
        }
        data.set(bx, WorldConstants.FLAT_SURFACE_LEVEL, bz, cap);
    }

    private byte surfaceCap(int worldX, int worldZ) {
        int roll = (int) (SpatialHash.hash(worldX, worldZ, seed) % WEIGHT_TOTAL);
        if (roll < DIRT_WEIGHT)                return WorldConstants.BLOCK_DIRT;
        if (roll < DIRT_WEIGHT + STONE_WEIGHT) return WorldConstants.BLOCK_STONE;
        return WorldConstants.BLOCK_WATER;
    }
}
