package org.example;

import org.example.components.VoxelChunkData;
import org.example.world.WorldConstants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MainTest {

    private static final int S = WorldConstants.CHUNK_SIZE;

    @Test
    void stoneBlocksOccupyLayers0To5() {
        VoxelChunkData data = Main.fillTerrain();
        for (int bx = 0; bx < S; bx++)
            for (int bz = 0; bz < S; bz++)
                for (int by = 0; by <= 5; by++)
                    assertEquals(WorldConstants.BLOCK_STONE, data.get(bx, by, bz),
                            "Expected stone at y=" + by);
    }

    @Test
    void dirtBlocksOccupyLayers6And7() {
        VoxelChunkData data = Main.fillTerrain();
        for (int bx = 0; bx < S; bx++)
            for (int bz = 0; bz < S; bz++)
                for (int by = 6; by <= 7; by++)
                    assertEquals(WorldConstants.BLOCK_DIRT, data.get(bx, by, bz),
                            "Expected dirt at y=" + by);
    }

    @Test
    void grassBlocksOccupyLayer8() {
        VoxelChunkData data = Main.fillTerrain();
        for (int bx = 0; bx < S; bx++)
            for (int bz = 0; bz < S; bz++)
                assertEquals(WorldConstants.BLOCK_GRASS, data.get(bx, 8, bz), "Expected grass at y=8");
    }

    @Test
    void noSolidBlocksAboveLayer8() {
        VoxelChunkData data = Main.fillTerrain();
        for (int bx = 0; bx < S; bx++)
            for (int bz = 0; bz < S; bz++)
                for (int by = 9; by < S; by++)
                    assertEquals(WorldConstants.BLOCK_AIR, data.get(bx, by, bz),
                            "Expected air at y=" + by);
    }
}
