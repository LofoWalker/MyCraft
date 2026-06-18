package org.example.worldgen.stage;

import org.example.components.VoxelChunkData;
import org.example.world.WorldConstants;
import org.example.worldgen.Biome;
import org.example.worldgen.BiomeMap;
import org.example.worldgen.TerrainShape;

// Fills each column up to its surface (and up to sea level with water), assigning block types by
// altitude and biome. Shape comes from TerrainShape; block choice from per-biome palette rules.
public final class TerrainStage implements GenerationStage {

    private static final int DIRT_DEPTH = 3;

    private final TerrainShape shape;
    private final BiomeMap     biomeMap;

    public TerrainStage(TerrainShape shape) {
        this.shape    = shape;
        this.biomeMap = shape.biomeMap();
    }

    @Override
    public void apply(VoxelChunkData data, int chunkX, int chunkZ) {
        int s = WorldConstants.CHUNK_SIZE_XZ;
        for (int bx = 0; bx < s; bx++) {
            for (int bz = 0; bz < s; bz++) {
                double worldX  = (double) (chunkX * s + bx);
                double worldZ  = (double) (chunkZ * s + bz);
                int    surface = shape.surfaceY(worldX, worldZ);
                Biome  biome   = biomeMap.biomeAt((int) worldX, (int) worldZ);
                fillColumn(data, bx, bz, surface, biome);
            }
        }
    }

    private static void fillColumn(VoxelChunkData data, int bx, int bz, int surfaceY, Biome biome) {
        int top = Math.min(WorldConstants.WORLD_HEIGHT - 1, Math.max(surfaceY, WorldConstants.WATER_LEVEL));
        for (int by = 0; by <= top; by++) {
            byte block = classifyBlock(by, surfaceY, biome);
            if (block != WorldConstants.BLOCK_AIR) {
                data.set(bx, by, bz, block);
            }
        }
    }

    static byte classifyBlock(int y, int surfaceY, Biome biome) {
        if (y <= surfaceY)                    return terrainBlock(y, surfaceY, biome);
        if (y <= WorldConstants.WATER_LEVEL)  return WorldConstants.BLOCK_WATER;
        return WorldConstants.BLOCK_AIR;
    }

    // Kept for backward compatibility with existing tests that call the old two-arg form.
    static byte classifyBlock(int y, int surfaceY) {
        return classifyBlock(y, surfaceY, Biome.PLAINS);
    }

    private static byte terrainBlock(int y, int surfaceY, Biome biome) {
        if (y == surfaceY)                         return topBlock(surfaceY, biome);
        if (surfaceY >= WorldConstants.ROCK_LEVEL) return WorldConstants.BLOCK_STONE;
        if (biome == Biome.DESERT && y >= surfaceY - WorldConstants.DESERT_SAND_DEPTH)
            return WorldConstants.BLOCK_SAND;
        if (y >= surfaceY - DIRT_DEPTH)            return WorldConstants.BLOCK_DIRT;
        return WorldConstants.BLOCK_STONE;
    }

    static byte topBlock(int surfaceY, Biome biome) {
        if (surfaceY >= WorldConstants.ROCK_LEVEL)  return WorldConstants.BLOCK_STONE;
        if (surfaceY <= WorldConstants.WATER_LEVEL) return WorldConstants.BLOCK_DIRT;
        return switch (biome) {
            case DESERT -> WorldConstants.BLOCK_SAND;
            default     -> WorldConstants.BLOCK_GRASS;
        };
    }

    // Kept for backward compatibility with existing tests that call the old one-arg form.
    static byte topBlock(int surfaceY) {
        return topBlock(surfaceY, Biome.PLAINS);
    }
}
