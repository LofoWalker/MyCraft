package org.example.worldgen.stage;

import org.example.components.VoxelChunkData;
import org.example.world.WorldConstants;
import org.example.worldgen.TerrainShape;

// Fills each column up to its surface (and up to sea level with water), assigning block types by
// altitude. This is the "noise + surface" stage: shape comes from TerrainShape, block choice from
// the local rules below.
public final class TerrainStage implements GenerationStage {

    private static final int DIRT_DEPTH = 3;

    private final TerrainShape shape;

    public TerrainStage(TerrainShape shape) {
        this.shape = shape;
    }

    @Override
    public void apply(VoxelChunkData data, int chunkX, int chunkZ) {
        int s = WorldConstants.CHUNK_SIZE_XZ;
        for (int bx = 0; bx < s; bx++) {
            for (int bz = 0; bz < s; bz++) {
                double worldX  = (double) (chunkX * s + bx);
                double worldZ  = (double) (chunkZ * s + bz);
                int    surface = shape.surfaceY(worldX, worldZ);
                fillColumn(data, bx, bz, surface);
            }
        }
    }

    private static void fillColumn(VoxelChunkData data, int bx, int bz, int surfaceY) {
        int top = Math.min(WorldConstants.WORLD_HEIGHT - 1, Math.max(surfaceY, WorldConstants.WATER_LEVEL));
        for (int by = 0; by <= top; by++) {
            byte block = classifyBlock(by, surfaceY);
            if (block != WorldConstants.BLOCK_AIR) {
                data.set(bx, by, bz, block);
            }
        }
    }

    static byte classifyBlock(int y, int surfaceY) {
        if (y <= surfaceY)                    return terrainBlock(y, surfaceY);
        if (y <= WorldConstants.WATER_LEVEL)  return WorldConstants.BLOCK_WATER;
        return WorldConstants.BLOCK_AIR;
    }

    private static byte terrainBlock(int y, int surfaceY) {
        if (y == surfaceY)                         return topBlock(surfaceY);
        if (surfaceY >= WorldConstants.ROCK_LEVEL) return WorldConstants.BLOCK_STONE;
        if (y >= surfaceY - DIRT_DEPTH)            return WorldConstants.BLOCK_DIRT;
        return WorldConstants.BLOCK_STONE;
    }

    static byte topBlock(int surfaceY) {
        if (surfaceY >= WorldConstants.ROCK_LEVEL)  return WorldConstants.BLOCK_STONE;
        if (surfaceY <= WorldConstants.WATER_LEVEL) return WorldConstants.BLOCK_DIRT;
        return WorldConstants.BLOCK_GRASS;
    }
}
