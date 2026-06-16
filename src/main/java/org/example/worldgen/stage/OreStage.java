package org.example.worldgen.stage;

import org.example.components.VoxelChunkData;
import org.example.world.WorldConstants;
import org.example.worldgen.SpatialHash;

// Decoration stage: scatters ore into underground stone. Each eligible stone block independently
// rolls (deterministically per world position) to become iron throughout, or rare diamond limited
// to the deepest layers. Only stone is replaced, so caves, dirt and water are left untouched.
public final class OreStage implements GenerationStage {

    // Distinct salts give iron and diamond independent random streams off the same world seed.
    private static final long IRON_SALT    = 0x140E0DL;
    private static final long DIAMOND_SALT = 0xD1A0DL;

    private final long seed;

    public OreStage(long seed) {
        this.seed = seed;
    }

    @Override
    public void apply(VoxelChunkData data, int chunkX, int chunkZ) {
        int s = WorldConstants.CHUNK_SIZE_XZ;
        for (int bx = 0; bx < s; bx++) {
            for (int bz = 0; bz < s; bz++) {
                int worldX = chunkX * s + bx;
                int worldZ = chunkZ * s + bz;
                for (int by = WorldConstants.ORE_MIN_LEVEL; by < WorldConstants.FLAT_SURFACE_LEVEL; by++) {
                    if (data.get(bx, by, bz) != WorldConstants.BLOCK_STONE) continue;
                    byte ore = oreAt(worldX, by, worldZ);
                    if (ore != WorldConstants.BLOCK_AIR) data.set(bx, by, bz, ore);
                }
            }
        }
    }

    // BLOCK_AIR is the "no ore here" sentinel; the caller only writes solid ore results.
    private byte oreAt(int worldX, int y, int worldZ) {
        if (y <= WorldConstants.DIAMOND_MAX_LEVEL
                && SpatialHash.hash(worldX, y, worldZ, seed ^ DIAMOND_SALT) % WorldConstants.DIAMOND_RARITY == 0) {
            return WorldConstants.BLOCK_DIAMOND;
        }
        if (SpatialHash.hash(worldX, y, worldZ, seed ^ IRON_SALT) % WorldConstants.IRON_RARITY == 0) {
            return WorldConstants.BLOCK_IRON;
        }
        return WorldConstants.BLOCK_AIR;
    }
}
