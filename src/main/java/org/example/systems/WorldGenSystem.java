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

    private static final double NOISE_SCALE    = 0.05;
    private static final double MOUNTAIN_SCALE = 0.012;
    private static final int    OCTAVES        = 4;
    private static final double PERSISTENCE    = 0.5;
    private static final double LACUNARITY     = 2.0;
    private static final int    DIRT_DEPTH     = 3;

    private final PerlinNoise noise;
    private final long        seed;

    public WorldGenSystem(long seed) {
        this.noise = new PerlinNoise(seed);
        this.seed  = seed;
    }

    @Override
    public void update(World world, float dt) {
        for (int eid : world.query(VoxelChunkData.class, ChunkComponent.class)) {
            Entity entity = new Entity(eid);
            if (world.has(entity, ChunkGenerated.class)) continue;
            ChunkComponent chunk = world.get(entity, ChunkComponent.class).orElseThrow();
            VoxelChunkData data  = world.get(entity, VoxelChunkData.class).orElseThrow();
            generateTerrain(data, chunk.x(), chunk.z());
            plantTrees(data, chunk.x(), chunk.z());
            world.add(entity, new ChunkGenerated());
        }
    }

    // Package-private: pure data transform — testable without ECS context
    void generateTerrain(VoxelChunkData data, int chunkX, int chunkZ) {
        int S = WorldConstants.CHUNK_SIZE_XZ;
        for (int bx = 0; bx < S; bx++) {
            for (int bz = 0; bz < S; bz++) {
                double worldX  = (double) (chunkX * S + bx);
                double worldZ  = (double) (chunkZ * S + bz);
                int    surface = computeSurfaceY(worldX, worldZ);
                fillColumn(data, bx, bz, surface);
            }
        }
    }

    // Package-private: pure feature pass — runs after terrain, testable without ECS context.
    // Trees stay inside chunk bounds (no cross-chunk writes), leaving a thin treeless seam at borders.
    void plantTrees(VoxelChunkData data, int chunkX, int chunkZ) {
        int S      = WorldConstants.CHUNK_SIZE_XZ;
        int margin = WorldConstants.TREE_CANOPY_RADIUS;
        for (int bx = margin; bx < S - margin; bx++) {
            for (int bz = margin; bz < S - margin; bz++) {
                int worldX = chunkX * S + bx;
                int worldZ = chunkZ * S + bz;
                if (!shouldPlantTree(worldX, worldZ)) continue;
                int surfaceY = computeSurfaceY(worldX, worldZ);
                if (data.get(bx, surfaceY, bz) != WorldConstants.BLOCK_GRASS) continue;
                growTree(data, bx, bz, surfaceY, trunkHeight(worldX, worldZ));
            }
        }
    }

    private boolean shouldPlantTree(int worldX, int worldZ) {
        return hash(worldX, worldZ, seed) % WorldConstants.TREE_RARITY == 0;
    }

    private int trunkHeight(int worldX, int worldZ) {
        int span = WorldConstants.TREE_TRUNK_MAX_HEIGHT - WorldConstants.TREE_TRUNK_MIN_HEIGHT + 1;
        return WorldConstants.TREE_TRUNK_MIN_HEIGHT + (int) (hash(worldX, worldZ, ~seed) % span);
    }

    private static void growTree(VoxelChunkData data, int bx, int bz, int surfaceY, int trunkHeight) {
        int trunkTop = surfaceY + trunkHeight;
        if (trunkTop + 2 >= WorldConstants.WORLD_HEIGHT) return; // canopy would overflow the world top
        for (int y = surfaceY + 1; y <= trunkTop; y++) {
            data.set(bx, y, bz, WorldConstants.BLOCK_WOOD);
        }
        int radius = WorldConstants.TREE_CANOPY_RADIUS;
        addLeafLayer(data, bx, bz, trunkTop - 1, radius);
        addLeafLayer(data, bx, bz, trunkTop,     radius);
        addLeafLayer(data, bx, bz, trunkTop + 1, radius - 1);
        addLeafLayer(data, bx, bz, trunkTop + 2, radius - 1);
    }

    private static void addLeafLayer(VoxelChunkData data, int cx, int cz, int y, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (Math.abs(dx) == radius && Math.abs(dz) == radius) continue; // rounded corners
                if (data.get(cx + dx, y, cz + dz) == WorldConstants.BLOCK_AIR) {
                    data.set(cx + dx, y, cz + dz, WorldConstants.BLOCK_LEAVES);
                }
            }
        }
    }

    // Deterministic spatial hash (SplitMix-style finalizer) — same seed + coords always agree.
    private static long hash(int x, int z, long seed) {
        long h = seed ^ (x * 0x9E3779B97F4A7C15L) ^ (z * 0xC2B2AE3D27D4EB4FL);
        h ^= (h >>> 30);
        h *= 0xBF58476D1CE4E5B9L;
        h ^= (h >>> 27);
        h *= 0x94D049BB133111EBL;
        h ^= (h >>> 31);
        return h & 0x7FFFFFFFFFFFFFFFL;
    }

    private int computeSurfaceY(double worldX, double worldZ) {
        double hills = noise.fractal(worldX * NOISE_SCALE, worldZ * NOISE_SCALE, OCTAVES, PERSISTENCE, LACUNARITY);
        double base  = WorldConstants.TERRAIN_BASE_HEIGHT + hills * WorldConstants.TERRAIN_AMPLITUDE;
        return clampToWorld((int) Math.round(base + mountainHeight(worldX, worldZ)));
    }

    // Only the positive half of the mask lifts terrain: lowlands (mask <= 0) stay flat,
    // while ranges rise proportionally into mountains.
    private double mountainHeight(double worldX, double worldZ) {
        double mask = noise.fractal(worldX * MOUNTAIN_SCALE, worldZ * MOUNTAIN_SCALE, OCTAVES, PERSISTENCE, LACUNARITY);
        return Math.max(0.0, mask) * WorldConstants.MOUNTAIN_AMPLITUDE;
    }

    private static int clampToWorld(int y) {
        return Math.max(1, Math.min(WorldConstants.WORLD_HEIGHT - 1, y));
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
        if (y == surfaceY)                       return topBlock(surfaceY);
        if (surfaceY >= WorldConstants.ROCK_LEVEL) return WorldConstants.BLOCK_STONE;
        if (y >= surfaceY - DIRT_DEPTH)          return WorldConstants.BLOCK_DIRT;
        return WorldConstants.BLOCK_STONE;
    }

    static byte topBlock(int surfaceY) {
        if (surfaceY >= WorldConstants.ROCK_LEVEL) return WorldConstants.BLOCK_STONE;
        if (surfaceY <= WorldConstants.WATER_LEVEL) return WorldConstants.BLOCK_DIRT;
        return WorldConstants.BLOCK_GRASS;
    }
}
