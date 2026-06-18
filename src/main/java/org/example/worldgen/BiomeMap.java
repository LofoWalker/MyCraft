package org.example.worldgen;

import org.example.world.WorldConstants;
import org.example.worldgen.noise.PerlinNoise;

// Pure, deterministic biome classifier: maps any world (x, z) column to a Biome based on two
// independent low-frequency noise fields (temperature, humidity). No World/GL dependencies — fully
// testable offline. The same seed always produces the same biome at the same coordinate.
// Not declared final so tests can substitute a fixed-biome stub via anonymous subclass.
public class BiomeMap {

    // Seed XOR constants keep temperature and humidity noise independent from each other and from
    // terrain noise even when all share the same root seed.
    private static final long TEMP_SEED_XOR = 0x54454D50L; // "TEMP" in ASCII
    private static final long HUMD_SEED_XOR = 0x48554D44L; // "HUMD" in ASCII

    private final PerlinNoise temperatureNoise;
    private final PerlinNoise humidityNoise;

    public BiomeMap(long seed) {
        this.temperatureNoise = new PerlinNoise(seed ^ TEMP_SEED_XOR);
        this.humidityNoise    = new PerlinNoise(seed ^ HUMD_SEED_XOR);
    }

    // Derive a stable biome at (worldX, worldZ) from temperature and humidity samples.
    public Biome biomeAt(int worldX, int worldZ) {
        double temperature = sample(temperatureNoise, worldX, worldZ, WorldConstants.BIOME_TEMPERATURE_SCALE);
        double humidity    = sample(humidityNoise,    worldX, worldZ, WorldConstants.BIOME_HUMIDITY_SCALE);
        return classify(temperature, humidity);
    }

    // Returns a blended base offset for terrain height, interpolating between neighbouring biomes
    // to avoid cliff seams at biome boundaries.
    public double blendedBaseOffset(double worldX, double worldZ) {
        return blendProperty(worldX, worldZ, true);
    }

    // Returns a blended amplitude scale for terrain height, interpolating between neighbouring biomes.
    public double blendedAmplitudeScale(double worldX, double worldZ) {
        return blendProperty(worldX, worldZ, false);
    }

    // Classify (temperature, humidity) into a Biome using a priority table.
    static Biome classify(double temperature, double humidity) {
        if (temperature < WorldConstants.BIOME_OCEAN_TEMP_THRESHOLD) return Biome.OCEAN;
        if (temperature > WorldConstants.BIOME_DESERT_TEMP_THRESHOLD
                && humidity < WorldConstants.BIOME_DESERT_HUMID_THRESHOLD) return Biome.DESERT;
        if (humidity > WorldConstants.BIOME_FOREST_HUMID_THRESHOLD
                && temperature > WorldConstants.BIOME_MOUNTAIN_TEMP_THRESHOLD) return Biome.FOREST;
        if (temperature > WorldConstants.BIOME_DESERT_TEMP_THRESHOLD
                && humidity >= WorldConstants.BIOME_DESERT_HUMID_THRESHOLD) return Biome.MOUNTAINS;
        return Biome.PLAINS;
    }

    // Sample a noise field at the given world position.
    private static double sample(PerlinNoise noise, double x, double z, double scale) {
        return noise.fractal(x * scale, z * scale,
                WorldConstants.BIOME_NOISE_OCTAVES, 0.5, 2.0);
    }

    // Weighted blend over a 3×3 grid of probe points. Each probe contributes its biome property
    // with a Gaussian-like weight so biome borders fade gradually rather than snapping.
    private double blendProperty(double worldX, double worldZ, boolean useBaseOffset) {
        double step  = WorldConstants.BIOME_BLEND_RADIUS * 12.0;
        double sumW   = 0.0;
        double sumVal = 0.0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                double px = worldX + dx * step;
                double pz = worldZ + dz * step;
                double t  = sample(temperatureNoise, px, pz, WorldConstants.BIOME_TEMPERATURE_SCALE);
                double h  = sample(humidityNoise,    px, pz, WorldConstants.BIOME_HUMIDITY_SCALE);
                Biome  b  = classify(t, h);
                double distSq = dx * dx + dz * dz;
                double w  = Math.exp(-distSq * 0.5);
                sumVal += w * (useBaseOffset ? b.baseOffset() : b.amplitudeScale());
                sumW   += w;
            }
        }
        return sumVal / sumW;
    }
}
