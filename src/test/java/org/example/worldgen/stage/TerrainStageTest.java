package org.example.worldgen.stage;

import org.example.components.VoxelChunkData;
import org.example.world.WorldConstants;
import org.example.worldgen.Biome;
import org.example.worldgen.BiomeMap;
import org.example.worldgen.TerrainShape;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TerrainStageTest {

    private static final int  SX   = WorldConstants.CHUNK_SIZE_XZ;
    private static final int  H    = WorldConstants.WORLD_HEIGHT;
    private static final long SEED = WorldConstants.WORLD_SEED;

    private final TerrainStage terrain = new TerrainStage(new TerrainShape(SEED));

    private VoxelChunkData generated(int cx, int cz) {
        VoxelChunkData data = VoxelChunkData.empty();
        terrain.apply(data, cx, cz);
        return data;
    }

    @Test
    void topBlockDependsOnAltitude() {
        assertEquals(WorldConstants.BLOCK_GRASS,
                TerrainStage.topBlock(WorldConstants.WATER_LEVEL + 3), "Dry land is grass");
        assertEquals(WorldConstants.BLOCK_DIRT,
                TerrainStage.topBlock(WorldConstants.WATER_LEVEL), "Submerged surface is dirt");
        assertEquals(WorldConstants.BLOCK_DIRT,
                TerrainStage.topBlock(WorldConstants.WATER_LEVEL - 4), "Lake bed is dirt");
        assertEquals(WorldConstants.BLOCK_STONE,
                TerrainStage.topBlock(WorldConstants.ROCK_LEVEL), "Mountain peak is bare stone");
    }

    @Test
    void columnSurfaceMatchesAltitudeRule() {
        VoxelChunkData data = generated(0, 0);
        for (int bx = 0; bx < SX; bx++) {
            for (int bz = 0; bz < SX; bz++) {
                int surface = terrainSurfaceY(data, bx, bz);
                assertTrue(surface >= 0, "No terrain in column (" + bx + "," + bz + ")");
                assertEquals(TerrainStage.topBlock(surface), data.get(bx, surface, bz),
                        "Surface block at y=" + surface + " disagrees with altitude rule");
            }
        }
    }

    @Test
    void dirtLayerSitsDirectlyBelowGrass() {
        VoxelChunkData data = generated(0, 0);
        for (int bx = 0; bx < SX; bx++) {
            for (int bz = 0; bz < SX; bz++) {
                int surface = terrainSurfaceY(data, bx, bz);
                if (surface > 0 && data.get(bx, surface, bz) == WorldConstants.BLOCK_GRASS) {
                    assertEquals(WorldConstants.BLOCK_DIRT, data.get(bx, surface - 1, bz),
                            "Block below grass should be dirt at y=" + (surface - 1));
                }
            }
        }
    }

    @Test
    void stoneAppearsDeepBelowSurface() {
        VoxelChunkData data = generated(0, 0);
        for (int bx = 0; bx < SX; bx++) {
            for (int bz = 0; bz < SX; bz++) {
                int surface = terrainSurfaceY(data, bx, bz);
                int deepY = surface - 4;
                if (deepY >= 0) {
                    assertEquals(WorldConstants.BLOCK_STONE, data.get(bx, deepY, bz),
                            "Expected stone 4 blocks below surface at y=" + deepY);
                }
            }
        }
    }

    @Test
    void waterFillsValleysUpToSeaLevel() {
        VoxelChunkData data = generated(0, 0);
        for (int bx = 0; bx < SX; bx++) {
            for (int bz = 0; bz < SX; bz++) {
                int surface = terrainSurfaceY(data, bx, bz);
                for (int by = surface + 1; by <= WorldConstants.WATER_LEVEL; by++) {
                    assertEquals(WorldConstants.BLOCK_WATER, data.get(bx, by, bz),
                            "Expected water between surface and sea level at y=" + by);
                }
                int airStart = Math.max(surface, WorldConstants.WATER_LEVEL) + 1;
                for (int by = airStart; by < H; by++) {
                    assertEquals(WorldConstants.BLOCK_AIR, data.get(bx, by, bz),
                            "Expected air above terrain/sea level at y=" + by);
                }
            }
        }
    }

    @Test
    void worldContainsWaterAndStonePeaks() {
        boolean foundWater = false;
        boolean foundPeak  = false;
        outer:
        for (int cx = -4; cx <= 4; cx++) {
            for (int cz = -4; cz <= 4; cz++) {
                VoxelChunkData data = generated(cx, cz);
                for (int bx = 0; bx < SX && !(foundWater && foundPeak); bx++)
                    for (int bz = 0; bz < SX; bz++) {
                        int surface = terrainSurfaceY(data, bx, bz);
                        if (data.get(bx, WorldConstants.WATER_LEVEL, bz) == WorldConstants.BLOCK_WATER) foundWater = true;
                        if (surface >= WorldConstants.ROCK_LEVEL) foundPeak = true;
                    }
                if (foundWater && foundPeak) break outer;
            }
        }
        assertTrue(foundWater, "Expected water somewhere in the scanned region");
        assertTrue(foundPeak,  "Expected stone mountain peaks somewhere in the scanned region");
    }

    @Test
    void differentSeedsProduceDifferentTerrain() {
        VoxelChunkData a = generated(0, 0);
        TerrainStage other = new TerrainStage(new TerrainShape(SEED + 1));
        VoxelChunkData b = VoxelChunkData.empty();
        other.apply(b, 0, 0);
        assertFalse(java.util.Arrays.equals(a.blocks(), b.blocks()),
                "Distinct seeds should yield distinct terrain");
    }

    @Test
    void sameSeedProducesSameChunk() {
        TerrainStage other = new TerrainStage(new TerrainShape(SEED));
        VoxelChunkData a = generated(2, -1);
        VoxelChunkData b = VoxelChunkData.empty();
        other.apply(b, 2, -1);
        assertArrayEquals(a.blocks(), b.blocks());
    }

    @Test
    void terrainIsNotFlat() {
        VoxelChunkData data = generated(0, 0);
        int firstSurface = terrainSurfaceY(data, 0, 0);
        boolean foundVariation = false;
        outer:
        for (int bx = 0; bx < SX; bx++) {
            for (int bz = 0; bz < SX; bz++) {
                if (terrainSurfaceY(data, bx, bz) != firstSurface) {
                    foundVariation = true;
                    break outer;
                }
            }
        }
        assertTrue(foundVariation, "All columns have the same height — terrain is flat");
    }

    // --- Biome-palette tests (STEP-34) ---

    @Test
    void desertTopBlockIsSand() {
        // topBlock for DESERT biome at dry land elevation should be SAND, not GRASS.
        byte top = TerrainStage.topBlock(WorldConstants.WATER_LEVEL + 5, Biome.DESERT);
        assertEquals(WorldConstants.BLOCK_SAND, top, "Desert surface should be sand");
    }

    @Test
    void desertColumnHasNoGrass() {
        // Force desert biome everywhere by using a fixed biome map stub.
        VoxelChunkData data = generateWithFixedBiome(Biome.DESERT, 0, 0);
        for (int bx = 0; bx < SX; bx++) {
            for (int bz = 0; bz < SX; bz++) {
                for (int by = 0; by < H; by++) {
                    assertNotEquals(WorldConstants.BLOCK_GRASS, data.get(bx, by, bz),
                            "Grass should not appear in a pure desert chunk");
                }
            }
        }
    }

    @Test
    void desertSurfaceIsSandAndSubSurfaceIsSandThenStone() {
        // In a desert column, the surface block is sand, the layer below is sand, and
        // 4 blocks below the surface is stone.
        int dryElevation = WorldConstants.WATER_LEVEL + 5;
        byte surface = TerrainStage.topBlock(dryElevation, Biome.DESERT);
        byte subSurface1 = TerrainStage.classifyBlock(dryElevation - 1, dryElevation, Biome.DESERT);
        byte deep = TerrainStage.classifyBlock(dryElevation - 4, dryElevation, Biome.DESERT);
        assertEquals(WorldConstants.BLOCK_SAND, surface,  "Desert surface is sand");
        assertEquals(WorldConstants.BLOCK_SAND, subSurface1, "Desert sub-surface layer is sand");
        assertEquals(WorldConstants.BLOCK_STONE, deep,   "Desert 4 blocks deep is stone");
    }

    @Test
    void oceanColumnFillsWithWaterAboveTerrain() {
        // Force ocean biome everywhere; the biome just affects terrain amplitude/offset, but
        // water fill is handled by classifyBlock. A depressed surface should be flooded.
        VoxelChunkData data = generateWithFixedBiome(Biome.OCEAN, 0, 0);
        boolean foundWater = false;
        for (int bx = 0; bx < SX && !foundWater; bx++) {
            for (int bz = 0; bz < SX && !foundWater; bz++) {
                if (data.get(bx, WorldConstants.WATER_LEVEL, bz) == WorldConstants.BLOCK_WATER) {
                    foundWater = true;
                }
            }
        }
        assertTrue(foundWater, "Ocean biome should produce water at sea level");
    }

    @Test
    void plainsTopBlockIsGrass() {
        byte top = TerrainStage.topBlock(WorldConstants.WATER_LEVEL + 3, Biome.PLAINS);
        assertEquals(WorldConstants.BLOCK_GRASS, top, "Plains surface should be grass");
    }

    @Test
    void forestTopBlockIsGrass() {
        byte top = TerrainStage.topBlock(WorldConstants.WATER_LEVEL + 3, Biome.FOREST);
        assertEquals(WorldConstants.BLOCK_GRASS, top, "Forest surface should be grass");
    }

    @Test
    void mountainPeakIsBareStone() {
        byte top = TerrainStage.topBlock(WorldConstants.ROCK_LEVEL + 5, Biome.MOUNTAINS);
        assertEquals(WorldConstants.BLOCK_STONE, top, "Mountain peak above ROCK_LEVEL should be bare stone");
    }

    /** Generates a chunk where all columns are forced to the given biome. */
    private static VoxelChunkData generateWithFixedBiome(Biome fixedBiome, int cx, int cz) {
        // Use a fixed-biome TerrainShape via anonymous BiomeMap override.
        long seed = WorldConstants.WORLD_SEED;
        BiomeMap fixedMap = new BiomeMap(seed) {
            @Override public Biome biomeAt(int x, int z) { return fixedBiome; }
            @Override public double blendedBaseOffset(double x, double z)     { return fixedBiome.baseOffset(); }
            @Override public double blendedAmplitudeScale(double x, double z) { return fixedBiome.amplitudeScale(); }
        };
        TerrainShape shape = new TerrainShape(seed, fixedMap);
        TerrainStage stage = new TerrainStage(shape);
        VoxelChunkData data = VoxelChunkData.empty();
        stage.apply(data, cx, cz);
        return data;
    }

    /** Returns the y of the highest solid terrain block (ignoring air and water), or -1 if none. */
    private static int terrainSurfaceY(VoxelChunkData data, int bx, int bz) {
        for (int by = H - 1; by >= 0; by--) {
            byte b = data.get(bx, by, bz);
            if (b != WorldConstants.BLOCK_AIR && b != WorldConstants.BLOCK_WATER) return by;
        }
        return -1;
    }
}
