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

    private static final double NOISE_SCALE      = 0.05;
    private static final double MOUNTAIN_SCALE    = 0.006;
    private static final int    OCTAVES           = 4;
    private static final int    MOUNTAIN_OCTAVES  = 3;
    private static final double PERSISTENCE       = 0.5;
    private static final double LACUNARITY        = 2.0;
    private static final int    DIRT_DEPTH        = 3;

    // Caves: thin "spaghetti" tunnels (where two noise fields both cross zero) plus rarer
    // "cheese" caverns (high-noise blobs). The bottom CAVE_FLOOR layers are never carved.
    private static final int    CAVE_FLOOR        = 4;
    private static final int    CAVE_OCTAVES      = 2;
    private static final double TUNNEL_SCALE      = 0.04;
    private static final double TUNNEL_RADIUS     = 0.13;
    private static final double CHEESE_SCALE      = 0.025;
    private static final double CHEESE_THRESHOLD  = 0.48;

    private final PerlinNoise noise;
    private final PerlinNoise caveNoiseA;
    private final PerlinNoise caveNoiseB;
    private final PerlinNoise riverNoise;
    private final PerlinNoise basinNoise;
    private final long        seed;

    public WorldGenSystem(long seed) {
        this.noise      = new PerlinNoise(seed);
        this.caveNoiseA = new PerlinNoise(seed ^ 0xCA5E1L);
        this.caveNoiseB = new PerlinNoise(seed ^ 0xCA5E2L);
        this.riverNoise = new PerlinNoise(seed ^ 0x817E5L);
        this.basinNoise = new PerlinNoise(seed ^ 0xBA51AL);
        this.seed       = seed;
    }

    @Override
    public void update(World world, float dt) {
        for (int eid : world.query(VoxelChunkData.class, ChunkComponent.class)) {
            Entity entity = new Entity(eid);
            if (world.has(entity, ChunkGenerated.class)) continue;
            ChunkComponent chunk = world.get(entity, ChunkComponent.class).orElseThrow();
            VoxelChunkData data  = world.get(entity, VoxelChunkData.class).orElseThrow();
            generateTerrain(data, chunk.x(), chunk.z());
            carveCaves(data, chunk.x(), chunk.z());
            settleWater(data);
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

    // Package-private: pure carving pass — runs after terrain, before decoration.
    // Cave noise is sampled in world space, so tunnels stay continuous across chunk borders.
    void carveCaves(VoxelChunkData data, int chunkX, int chunkZ) {
        int S = WorldConstants.CHUNK_SIZE_XZ;
        for (int bx = 0; bx < S; bx++) {
            for (int bz = 0; bz < S; bz++) {
                double worldX = chunkX * S + bx;
                double worldZ = chunkZ * S + bz;
                for (int y = CAVE_FLOOR; y < WorldConstants.WORLD_HEIGHT; y++) {
                    byte block = data.get(bx, y, bz);
                    if (block == WorldConstants.BLOCK_AIR || block == WorldConstants.BLOCK_WATER) continue;
                    if (isCave(worldX, y, worldZ)) data.set(bx, y, bz, WorldConstants.BLOCK_AIR);
                }
            }
        }
    }

    // Package-private: pure pass — runs after carving. Any water with air directly below
    // pours straight down until it lands on a solid block, carving vertical waterfalls into
    // the caves the previous pass opened beneath lakes. A single top-down sweep suffices:
    // once a cell becomes water, the next (lower) cell sees it and keeps the column flowing.
    void settleWater(VoxelChunkData data) {
        int S = WorldConstants.CHUNK_SIZE_XZ;
        for (int bx = 0; bx < S; bx++) {
            for (int bz = 0; bz < S; bz++) {
                for (int y = WorldConstants.WORLD_HEIGHT - 1; y > 0; y--) {
                    if (data.get(bx, y, bz) != WorldConstants.BLOCK_WATER) continue;
                    if (data.get(bx, y - 1, bz) == WorldConstants.BLOCK_AIR) {
                        data.set(bx, y - 1, bz, WorldConstants.BLOCK_WATER);
                    }
                }
            }
        }
    }

    private boolean isCave(double worldX, double y, double worldZ) {
        double cheese = caveNoiseA.fractal3D(worldX * CHEESE_SCALE, y * CHEESE_SCALE, worldZ * CHEESE_SCALE,
                CAVE_OCTAVES, PERSISTENCE, LACUNARITY);
        if (cheese > CHEESE_THRESHOLD) return true;
        double t1 = caveNoiseA.noise(worldX * TUNNEL_SCALE, y * TUNNEL_SCALE, worldZ * TUNNEL_SCALE);
        double t2 = caveNoiseB.noise(worldX * TUNNEL_SCALE, y * TUNNEL_SCALE, worldZ * TUNNEL_SCALE);
        return Math.abs(t1) < TUNNEL_RADIUS && Math.abs(t2) < TUNNEL_RADIUS;
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
        double hills   = noise.fractal(worldX * NOISE_SCALE, worldZ * NOISE_SCALE, OCTAVES, PERSISTENCE, LACUNARITY);
        double base    = WorldConstants.TERRAIN_BASE_HEIGHT + hills * WorldConstants.TERRAIN_AMPLITUDE;
        int    surface = (int) Math.round(base + mountainHeight(worldX, worldZ));
        surface = carveBasin(surface, worldX, worldZ);
        return clampToWorld(carveRiver(surface, worldX, worldZ));
    }

    // Sinks broad shallow basins into the lowlands so flat plains still cradle lakes. Lowland
    // only (shares RIVER_MAX_ELEVATION as the lowland ceiling) so basins never dent mountains.
    int carveBasin(int surface, double worldX, double worldZ) {
        if (surface > WorldConstants.RIVER_MAX_ELEVATION) return surface;
        double b = basinNoise.fractal(worldX * WorldConstants.BASIN_SCALE, worldZ * WorldConstants.BASIN_SCALE,
                WorldConstants.BASIN_OCTAVES, PERSISTENCE, LACUNARITY);
        if (b >= -WorldConstants.BASIN_THRESHOLD) return surface;
        double depth = (-WorldConstants.BASIN_THRESHOLD - b) / (1.0 - WorldConstants.BASIN_THRESHOLD)
                * WorldConstants.BASIN_DEPTH;
        return surface - (int) Math.round(depth);
    }

    // Only mask values past MOUNTAIN_THRESHOLD lift terrain, remapped to [0,1] so the rise
    // starts at zero at the threshold: most of the map stays plains/hills, ranges are rare.
    // A low frequency + few octaves keeps massifs wide and smooth rather than spiky.
    private double mountainHeight(double worldX, double worldZ) {
        double mask = noise.fractal(worldX * MOUNTAIN_SCALE, worldZ * MOUNTAIN_SCALE,
                MOUNTAIN_OCTAVES, PERSISTENCE, LACUNARITY);
        if (mask <= WorldConstants.MOUNTAIN_THRESHOLD) return 0.0;
        double lift = (mask - WorldConstants.MOUNTAIN_THRESHOLD) / (1.0 - WorldConstants.MOUNTAIN_THRESHOLD);
        return lift * WorldConstants.MOUNTAIN_AMPLITUDE;
    }

    // Where the river ridge (|noise| near zero) crosses lowland terrain, sink the surface to
    // a bed below sea level so fillColumn floods it into a winding river. Highlands are exempt
    // so rivers never cut canyons through hills or mountains. Sampled in world space → continuous.
    int carveRiver(int surface, double worldX, double worldZ) {
        if (surface > WorldConstants.RIVER_MAX_ELEVATION) return surface;
        double ridge = Math.abs(riverNoise.fractal(worldX * WorldConstants.RIVER_SCALE,
                worldZ * WorldConstants.RIVER_SCALE, WorldConstants.RIVER_OCTAVES, PERSISTENCE, LACUNARITY));
        if (ridge >= WorldConstants.RIVER_HALF_WIDTH) return surface;
        double closeness = 1.0 - ridge / WorldConstants.RIVER_HALF_WIDTH;
        int    bed       = WorldConstants.WATER_LEVEL - (int) Math.round(closeness * WorldConstants.RIVER_BED_DEPTH);
        return Math.min(surface, bed);
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
