package org.example.worldgen;

import org.example.world.WorldConstants;
import org.example.worldgen.noise.PerlinNoise;

// Pure height field: maps a world (x, z) to a surface Y by combining rolling hills, rare wide
// mountains, lake basins and river channels. Holds no chunk state, so any stage that needs the
// surface height (terrain fill, tree planting) can share one instance.
// When a BiomeMap is supplied, the base terrain amplitude and vertical offset are blended per biome
// so height transitions between zones are smooth rather than cliff-walled.
public final class TerrainShape implements SurfaceHeights {

    private static final double NOISE_SCALE      = 0.05;
    private static final double MOUNTAIN_SCALE   = 0.006;
    private static final int    OCTAVES          = 4;
    private static final int    MOUNTAIN_OCTAVES = 3;
    private static final double PERSISTENCE      = 0.5;
    private static final double LACUNARITY       = 2.0;

    private final PerlinNoise noise;
    private final PerlinNoise riverNoise;
    private final PerlinNoise basinNoise;
    private final BiomeMap    biomeMap;

    public TerrainShape(long seed) {
        this(seed, new BiomeMap(seed));
    }

    public TerrainShape(long seed, BiomeMap biomeMap) {
        this.noise      = new PerlinNoise(seed);
        this.riverNoise = new PerlinNoise(seed ^ 0x817E5L);
        this.basinNoise = new PerlinNoise(seed ^ 0xBA51AL);
        this.biomeMap   = biomeMap;
    }

    public BiomeMap biomeMap() { return biomeMap; }

    @Override
    public int surfaceY(int worldX, int worldZ) {
        return surfaceY((double) worldX, (double) worldZ);
    }

    public int surfaceY(double worldX, double worldZ) {
        double hills          = noise.fractal(worldX * NOISE_SCALE, worldZ * NOISE_SCALE, OCTAVES, PERSISTENCE, LACUNARITY);
        double ampScale       = biomeMap.blendedAmplitudeScale(worldX, worldZ);
        double baseOffset     = biomeMap.blendedBaseOffset(worldX, worldZ);
        double base           = WorldConstants.TERRAIN_BASE_HEIGHT + baseOffset
                + hills * WorldConstants.TERRAIN_AMPLITUDE * ampScale;
        int    surface = (int) Math.round(base + mountainHeight(worldX, worldZ));
        surface = carveBasin(surface, worldX, worldZ);
        return clampToWorld(carveRiver(surface, worldX, worldZ));
    }

    // Only mask values past MOUNTAIN_THRESHOLD lift terrain, remapped to [0,1] so the rise
    // starts at zero at the threshold: most of the map stays plains/hills, ranges are rare.
    // A low frequency + few octaves keeps massifs wide and smooth rather than spiky.
    double mountainHeight(double worldX, double worldZ) {
        double mask = noise.fractal(worldX * MOUNTAIN_SCALE, worldZ * MOUNTAIN_SCALE,
                MOUNTAIN_OCTAVES, PERSISTENCE, LACUNARITY);
        if (mask <= WorldConstants.MOUNTAIN_THRESHOLD) return 0.0;
        double lift = (mask - WorldConstants.MOUNTAIN_THRESHOLD) / (1.0 - WorldConstants.MOUNTAIN_THRESHOLD);
        return lift * WorldConstants.MOUNTAIN_AMPLITUDE;
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

    // Where the river ridge (|noise| near zero) crosses lowland terrain, sink the surface to
    // a bed below sea level so the terrain fill floods it into a winding river. Highlands are
    // exempt so rivers never cut canyons through hills or mountains. Sampled in world space.
    int carveRiver(int surface, double worldX, double worldZ) {
        if (surface > WorldConstants.RIVER_MAX_ELEVATION) return surface;
        double ridge = Math.abs(riverNoise.fractal(worldX * WorldConstants.RIVER_SCALE,
                worldZ * WorldConstants.RIVER_SCALE, WorldConstants.RIVER_OCTAVES, PERSISTENCE, LACUNARITY));
        if (ridge >= WorldConstants.RIVER_HALF_WIDTH) return surface;
        double closeness = 1.0 - ridge / WorldConstants.RIVER_HALF_WIDTH;
        int    bed       = WorldConstants.WATER_LEVEL - (int) Math.round(closeness * WorldConstants.RIVER_BED_DEPTH);
        return Math.min(surface, bed);
    }

    static int clampToWorld(int y) {
        return Math.max(1, Math.min(WorldConstants.WORLD_HEIGHT - 1, y));
    }
}
