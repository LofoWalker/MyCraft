package org.example.systems;

import org.example.components.ChunkComponent;
import org.example.components.ChunkGenerated;
import org.example.components.VoxelChunkData;
import org.example.ecs.Entity;
import org.example.ecs.GameSystem;
import org.example.ecs.World;
import org.example.world.PerlinNoise;
import org.example.world.WorldConstants;

public final class WorldGenSystem implements GameSystem {

    private static final double NOISE_SCALE  = 0.05;
    private static final int    OCTAVES      = 4;
    private static final double PERSISTENCE  = 0.5;
    private static final double LACUNARITY   = 2.0;
    private static final int    DIRT_DEPTH   = 3;

    private final PerlinNoise noise;

    public WorldGenSystem(long seed) {
        this.noise = new PerlinNoise(seed);
    }

    @Override
    public void update(World world, float dt) {
        for (int eid : world.query(VoxelChunkData.class, ChunkComponent.class)) {
            Entity entity = new Entity(eid);
            if (world.has(entity, ChunkGenerated.class)) continue;
            ChunkComponent chunk = world.get(entity, ChunkComponent.class).orElseThrow();
            VoxelChunkData data  = world.get(entity, VoxelChunkData.class).orElseThrow();
            generateTerrain(data, chunk.x(), chunk.z());
            world.add(entity, new ChunkGenerated());
        }
    }

    // Package-private: pure data transform — testable without ECS context
    void generateTerrain(VoxelChunkData data, int chunkX, int chunkZ) {
        int S = WorldConstants.CHUNK_SIZE;
        for (int bx = 0; bx < S; bx++) {
            for (int bz = 0; bz < S; bz++) {
                double worldX  = (double) (chunkX * S + bx);
                double worldZ  = (double) (chunkZ * S + bz);
                int    surface = computeSurfaceY(worldX, worldZ);
                fillColumn(data, bx, bz, surface);
            }
        }
    }

    private int computeSurfaceY(double worldX, double worldZ) {
        double n = noise.fractal(worldX * NOISE_SCALE, worldZ * NOISE_SCALE, OCTAVES, PERSISTENCE, LACUNARITY);
        return (int) Math.round(WorldConstants.TERRAIN_BASE_HEIGHT + n * WorldConstants.TERRAIN_AMPLITUDE);
    }

    private static void fillColumn(VoxelChunkData data, int bx, int bz, int surfaceY) {
        int S = WorldConstants.CHUNK_SIZE;
        for (int by = 0; by < S; by++) {
            byte block = classifyBlock(by, surfaceY);
            if (block != WorldConstants.BLOCK_AIR) {
                data.set(bx, by, bz, block);
            }
        }
    }

    private static byte classifyBlock(int y, int surfaceY) {
        if (y > surfaceY)              return WorldConstants.BLOCK_AIR;
        if (y == surfaceY)             return WorldConstants.BLOCK_GRASS;
        if (y >= surfaceY - DIRT_DEPTH) return WorldConstants.BLOCK_DIRT;
        return WorldConstants.BLOCK_STONE;
    }
}
