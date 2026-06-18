package org.example.worldgen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BiomeMapTest {

    private static final long SEED = 42L;

    @Test
    void sameSeedAndCoordinateAlwaysReturnSameBiome() {
        BiomeMap map1 = new BiomeMap(SEED);
        BiomeMap map2 = new BiomeMap(SEED);
        for (int x = -200; x <= 200; x += 50) {
            for (int z = -200; z <= 200; z += 50) {
                assertEquals(map1.biomeAt(x, z), map2.biomeAt(x, z),
                        "Biome at (" + x + "," + z + ") must be deterministic");
            }
        }
    }

    @Test
    void differentSeedsProduceDifferentBiomeDistributions() {
        BiomeMap mapA = new BiomeMap(SEED);
        BiomeMap mapB = new BiomeMap(SEED + 999);
        int mismatches = 0;
        for (int x = -500; x <= 500; x += 100) {
            for (int z = -500; z <= 500; z += 100) {
                if (mapA.biomeAt(x, z) != mapB.biomeAt(x, z)) mismatches++;
            }
        }
        assertTrue(mismatches > 0, "Different seeds should yield at least some different biomes");
    }

    @Test
    void classifyOceanWhenTemperatureIsVeryLow() {
        // Temperature well below ocean threshold, any humidity → OCEAN
        Biome result = BiomeMap.classify(-0.9, 0.0);
        assertEquals(Biome.OCEAN, result);
    }

    @Test
    void classifyDesertWhenHotAndDry() {
        // High temperature, very low humidity → DESERT
        Biome result = BiomeMap.classify(0.8, -0.5);
        assertEquals(Biome.DESERT, result);
    }

    @Test
    void classifyForestWhenModerateTemperatureAndHighHumidity() {
        // Moderate-high temperature, high humidity → FOREST
        Biome result = BiomeMap.classify(0.2, 0.6);
        assertEquals(Biome.FOREST, result);
    }

    @Test
    void classifyPlainsWhenModerateTempAndHumidity() {
        // Central T/H values → PLAINS
        Biome result = BiomeMap.classify(0.0, 0.0);
        assertEquals(Biome.PLAINS, result);
    }

    @Test
    void worldSpanContainsMultipleBiomes() {
        BiomeMap map       = new BiomeMap(SEED);
        boolean[] seen     = new boolean[Biome.values().length];
        for (int x = -2000; x <= 2000; x += 64) {
            for (int z = -2000; z <= 2000; z += 64) {
                seen[map.biomeAt(x, z).ordinal()] = true;
            }
        }
        int biomeCount = 0;
        for (boolean s : seen) if (s) biomeCount++;
        assertTrue(biomeCount >= 3,
                "Expected at least 3 distinct biomes across large area, found: " + biomeCount);
    }

    @Test
    void blendedValuesAreWithinBiomePropertyRange() {
        BiomeMap map = new BiomeMap(SEED);
        double minAmp = Double.MAX_VALUE;
        double maxAmp = -Double.MAX_VALUE;
        for (Biome b : Biome.values()) {
            minAmp = Math.min(minAmp, b.amplitudeScale());
            maxAmp = Math.max(maxAmp, b.amplitudeScale());
        }
        for (int x = -500; x <= 500; x += 100) {
            for (int z = -500; z <= 500; z += 100) {
                double blended = map.blendedAmplitudeScale(x, z);
                assertTrue(blended >= minAmp - 0.01 && blended <= maxAmp + 0.01,
                        "Blended amplitude " + blended + " out of biome range at (" + x + "," + z + ")");
            }
        }
    }
}
