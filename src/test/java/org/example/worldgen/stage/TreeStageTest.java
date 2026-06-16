package org.example.worldgen.stage;

import org.example.components.VoxelChunkData;
import org.example.world.WorldConstants;
import org.example.worldgen.TerrainShape;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TreeStageTest {

    private static final int  SX   = WorldConstants.CHUNK_SIZE_XZ;
    private static final int  H    = WorldConstants.WORLD_HEIGHT;
    private static final long SEED = WorldConstants.WORLD_SEED;

    private final TerrainShape shape   = new TerrainShape(SEED);
    private final TerrainStage terrain = new TerrainStage(shape);
    private final TreeStage    trees   = new TreeStage(shape, SEED, WorldConstants.BLOCK_GRASS);

    private VoxelChunkData withTrees(int cx, int cz) {
        VoxelChunkData data = VoxelChunkData.empty();
        terrain.apply(data, cx, cz);
        trees.apply(data, cx, cz);
        return data;
    }

    @Test
    void treesProduceWoodAndLeaves() {
        boolean foundWood   = false;
        boolean foundLeaves = false;
        outer:
        for (int cx = 0; cx < 4 && !(foundWood && foundLeaves); cx++) {
            for (int cz = 0; cz < 4; cz++) {
                VoxelChunkData data = withTrees(cx, cz);
                for (int b : data.blocks()) {
                    if (b == WorldConstants.BLOCK_WOOD)   foundWood   = true;
                    if (b == WorldConstants.BLOCK_LEAVES) foundLeaves = true;
                }
                if (foundWood && foundLeaves) break outer;
            }
        }
        assertTrue(foundWood,   "Expected at least one wood block across the scanned chunks");
        assertTrue(foundLeaves, "Expected at least one leaf block across the scanned chunks");
    }

    @Test
    void trunksRiseFromGrass() {
        VoxelChunkData data = firstChunkWithTree();
        for (int bx = 0; bx < SX; bx++) {
            for (int bz = 0; bz < SX; bz++) {
                int lowestWood = lowestWoodY(data, bx, bz);
                if (lowestWood < 0) continue;
                assertEquals(WorldConstants.BLOCK_GRASS, data.get(bx, lowestWood - 1, bz),
                        "Trunk base at (" + bx + "," + bz + ") should stand on grass");
            }
        }
    }

    @Test
    void treesAreDeterministicForSameSeed() {
        VoxelChunkData a = withTrees(1, 2);
        TerrainShape shape2 = new TerrainShape(SEED);
        TerrainStage terrain2 = new TerrainStage(shape2);
        TreeStage trees2 = new TreeStage(shape2, SEED, WorldConstants.BLOCK_GRASS);
        VoxelChunkData b = VoxelChunkData.empty();
        terrain2.apply(b, 1, 2);
        trees2.apply(b, 1, 2);
        assertArrayEquals(a.blocks(), b.blocks());
    }

    @Test
    void plantTreesStaysWithinChunkBounds() {
        assertDoesNotThrow(() -> {
            for (int cx = -2; cx <= 2; cx++)
                for (int cz = -2; cz <= 2; cz++)
                    withTrees(cx, cz);
        });
    }

    private VoxelChunkData firstChunkWithTree() {
        for (int cx = 0; cx < 6; cx++) {
            for (int cz = 0; cz < 6; cz++) {
                VoxelChunkData data = withTrees(cx, cz);
                for (int b : data.blocks()) {
                    if (b == WorldConstants.BLOCK_WOOD) return data;
                }
            }
        }
        throw new AssertionError("No tree found in any scanned chunk");
    }

    private static int lowestWoodY(VoxelChunkData data, int bx, int bz) {
        for (int by = 1; by < H; by++) {
            if (data.get(bx, by, bz) == WorldConstants.BLOCK_WOOD) return by;
        }
        return -1;
    }
}
