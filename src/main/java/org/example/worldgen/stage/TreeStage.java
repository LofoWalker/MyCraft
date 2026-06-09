package org.example.worldgen.stage;

import org.example.components.VoxelChunkData;
import org.example.world.WorldConstants;
import org.example.worldgen.TerrainShape;

// Decoration stage: plants deterministic trees on grass surfaces. Trees stay inside chunk bounds
// (no cross-chunk writes), leaving a thin treeless seam at borders.
public final class TreeStage implements GenerationStage {

    private final TerrainShape shape;
    private final long         seed;

    public TreeStage(TerrainShape shape, long seed) {
        this.shape = shape;
        this.seed  = seed;
    }

    @Override
    public void apply(VoxelChunkData data, int chunkX, int chunkZ) {
        int s      = WorldConstants.CHUNK_SIZE_XZ;
        int margin = WorldConstants.TREE_CANOPY_RADIUS;
        for (int bx = margin; bx < s - margin; bx++) {
            for (int bz = margin; bz < s - margin; bz++) {
                int worldX = chunkX * s + bx;
                int worldZ = chunkZ * s + bz;
                if (!shouldPlantTree(worldX, worldZ)) continue;
                int surfaceY = shape.surfaceY(worldX, worldZ);
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
}
