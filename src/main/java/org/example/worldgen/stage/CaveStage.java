package org.example.worldgen.stage;

import org.example.components.VoxelChunkData;
import org.example.world.WorldConstants;
import org.example.worldgen.noise.PerlinNoise;

// Carves caves out of solid terrain: thin "spaghetti" tunnels (where two noise fields both cross
// zero) plus rarer "cheese" caverns (high-noise blobs). The bottom CAVE_FLOOR layers are never
// carved. Cave noise is sampled in world space, so tunnels stay continuous across chunk borders.
public final class CaveStage implements GenerationStage {

    static final int    CAVE_FLOOR       = 4;
    private static final int    CAVE_OCTAVES     = 2;
    private static final double TUNNEL_SCALE     = 0.04;
    private static final double TUNNEL_RADIUS    = 0.13;
    private static final double CHEESE_SCALE     = 0.025;
    private static final double CHEESE_THRESHOLD = 0.48;
    private static final double PERSISTENCE      = 0.5;
    private static final double LACUNARITY       = 2.0;

    private final PerlinNoise caveNoiseA;
    private final PerlinNoise caveNoiseB;

    public CaveStage(long seed) {
        this.caveNoiseA = new PerlinNoise(seed ^ 0xCA5E1L);
        this.caveNoiseB = new PerlinNoise(seed ^ 0xCA5E2L);
    }

    @Override
    public void apply(VoxelChunkData data, int chunkX, int chunkZ) {
        int s = WorldConstants.CHUNK_SIZE_XZ;
        for (int bx = 0; bx < s; bx++) {
            for (int bz = 0; bz < s; bz++) {
                double worldX = chunkX * s + bx;
                double worldZ = chunkZ * s + bz;
                for (int y = CAVE_FLOOR; y < WorldConstants.WORLD_HEIGHT; y++) {
                    byte block = data.get(bx, y, bz);
                    if (block == WorldConstants.BLOCK_AIR || block == WorldConstants.BLOCK_WATER) continue;
                    if (isCave(worldX, y, worldZ)) data.set(bx, y, bz, WorldConstants.BLOCK_AIR);
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
}
