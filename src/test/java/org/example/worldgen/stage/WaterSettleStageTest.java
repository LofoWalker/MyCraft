package org.example.worldgen.stage;

import org.example.components.VoxelChunkData;
import org.example.world.WorldConstants;
import org.example.worldgen.TerrainShape;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WaterSettleStageTest {

    private static final int  SX   = WorldConstants.CHUNK_SIZE_XZ;
    private static final int  H    = WorldConstants.WORLD_HEIGHT;
    private static final long SEED = WorldConstants.WORLD_SEED;

    private final TerrainStage      terrain = new TerrainStage(new TerrainShape(SEED));
    private final CaveStage         caves   = new CaveStage(SEED);
    private final WaterSettleStage  water   = new WaterSettleStage();

    private VoxelChunkData carved(int cx, int cz) {
        VoxelChunkData data = VoxelChunkData.empty();
        terrain.apply(data, cx, cz);
        caves.apply(data, cx, cz);
        return data;
    }

    private VoxelChunkData carvedAndSettled(int cx, int cz) {
        VoxelChunkData data = carved(cx, cz);
        water.apply(data, cx, cz);
        return data;
    }

    @Test
    void settledWaterNeverHasAirDirectlyBelow() {
        for (int cx = -3; cx <= 3; cx++) {
            for (int cz = -3; cz <= 3; cz++) {
                VoxelChunkData data = carvedAndSettled(cx, cz);
                for (int x = 0; x < SX; x++) {
                    for (int z = 0; z < SX; z++) {
                        for (int y = 1; y < H; y++) {
                            if (data.get(x, y, z) == WorldConstants.BLOCK_WATER) {
                                assertNotEquals(WorldConstants.BLOCK_AIR, data.get(x, y - 1, z),
                                        "Water at (" + x + "," + y + "," + z + ") still has air below");
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    void settlingOnlyTurnsAirIntoWater() {
        VoxelChunkData carvedData = carved(0, 0);
        byte[] before = carvedData.blocks().clone();
        water.apply(carvedData, 0, 0);
        for (int i = 0; i < before.length; i++) {
            if (before[i] != carvedData.blocks()[i]) {
                assertEquals(WorldConstants.BLOCK_AIR, before[i],
                        "Settling changed a non-air block at index " + i);
                assertEquals(WorldConstants.BLOCK_WATER, carvedData.blocks()[i],
                        "Settling produced something other than water at index " + i);
            }
        }
    }

    @Test
    void waterCascadesDownIntoCarvedAir() {
        VoxelChunkData data = VoxelChunkData.empty();
        int x = 5, z = 5;
        data.set(x, 50, z, WorldConstants.BLOCK_WATER);
        data.set(x, 47, z, WorldConstants.BLOCK_STONE);
        water.apply(data, 0, 0);
        assertEquals(WorldConstants.BLOCK_WATER, data.get(x, 49, z), "Water should fall one block");
        assertEquals(WorldConstants.BLOCK_WATER, data.get(x, 48, z), "Water should keep cascading");
        assertEquals(WorldConstants.BLOCK_STONE, data.get(x, 47, z), "Cascade stops on the solid floor");
        assertEquals(WorldConstants.BLOCK_AIR, data.get(x, 46, z), "Water must not pass through solid");
    }

    @Test
    void settlingIsDeterministicForSameSeed() {
        VoxelChunkData a = carvedAndSettled(1, 2);
        VoxelChunkData b = carvedAndSettled(1, 2);
        assertArrayEquals(a.blocks(), b.blocks(), "Settling must be deterministic");
    }
}
