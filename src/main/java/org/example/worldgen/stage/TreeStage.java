package org.example.worldgen.stage;

import org.example.components.VoxelChunkData;
import org.example.world.WorldConstants;
import org.example.worldgen.Biome;
import org.example.worldgen.BiomeMap;
import org.example.worldgen.SpatialHash;
import org.example.worldgen.SurfaceHeights;

// Decoration stage: plants deterministic trees on columns whose surface is the configured ground
// block. Tree density varies by biome via BiomeMap: forests are dense, deserts and oceans have none.
// Trees stay inside chunk bounds (no cross-chunk writes), leaving a thin treeless seam at borders.
public final class TreeStage implements GenerationStage {

    private final SurfaceHeights heights;
    private final long           seed;
    private final byte           groundBlock;
    private final BiomeMap       biomeMap;

    public TreeStage(SurfaceHeights heights, long seed, byte groundBlock, BiomeMap biomeMap) {
        this.heights     = heights;
        this.seed        = seed;
        this.groundBlock = groundBlock;
        this.biomeMap    = biomeMap;
    }

    // Legacy constructor for tests and flat pipeline that don't need biome-aware tree density.
    public TreeStage(SurfaceHeights heights, long seed, byte groundBlock) {
        this(heights, seed, groundBlock, null);
    }

    @Override
    public void apply(VoxelChunkData data, int chunkX, int chunkZ) {
        int s      = WorldConstants.CHUNK_SIZE_XZ;
        int margin = WorldConstants.TREE_CANOPY_RADIUS;
        for (int bx = margin; bx < s - margin; bx++) {
            for (int bz = margin; bz < s - margin; bz++) {
                int worldX = chunkX * s + bx;
                int worldZ = chunkZ * s + bz;
                int rarity = treeRarityAt(worldX, worldZ);
                if (!shouldPlantTree(worldX, worldZ, rarity)) continue;
                int surfaceY = heights.surfaceY(worldX, worldZ);
                if (data.get(bx, surfaceY, bz) != groundBlock) continue;
                growTree(data, bx, bz, surfaceY, trunkHeight(worldX, worldZ));
            }
        }
    }

    private int treeRarityAt(int worldX, int worldZ) {
        if (biomeMap == null) return WorldConstants.TREE_RARITY;
        return biomeMap.biomeAt(worldX, worldZ).treeRarity();
    }

    private boolean shouldPlantTree(int worldX, int worldZ, int rarity) {
        if (rarity == WorldConstants.TREE_RARITY_NONE) return false;
        return SpatialHash.hash(worldX, worldZ, seed) % rarity == 0;
    }

    private int trunkHeight(int worldX, int worldZ) {
        int span = WorldConstants.TREE_TRUNK_MAX_HEIGHT - WorldConstants.TREE_TRUNK_MIN_HEIGHT + 1;
        return WorldConstants.TREE_TRUNK_MIN_HEIGHT + (int) (SpatialHash.hash(worldX, worldZ, ~seed) % span);
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
}
