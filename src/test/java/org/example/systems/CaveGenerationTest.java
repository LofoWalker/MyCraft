package org.example.systems;

import org.example.components.VoxelChunkData;
import org.example.world.WorldConstants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CaveGenerationTest {

    private static final int  SX   = WorldConstants.CHUNK_SIZE_XZ;
    private static final int  H    = WorldConstants.WORLD_HEIGHT;
    private static final long SEED = WorldConstants.WORLD_SEED;

    // Mirrors WorldGenSystem.CAVE_FLOOR: the bottom layers must never be carved.
    private static final int CAVE_FLOOR = 4;

    private final WorldGenSystem gen = new WorldGenSystem(SEED);

    private VoxelChunkData terrainOnly(int cx, int cz) {
        VoxelChunkData data = VoxelChunkData.empty();
        gen.generateTerrain(data, cx, cz);
        return data;
    }

    private VoxelChunkData carved(int cx, int cz) {
        VoxelChunkData data = VoxelChunkData.empty();
        gen.generateTerrain(data, cx, cz);
        gen.carveCaves(data, cx, cz);
        return data;
    }

    @Test
    void carvingRemovesSomeSolidBlocks() {
        int carvedCount = 0;
        for (int cx = 0; cx < 2 && carvedCount == 0; cx++) {
            for (int cz = 0; cz < 2; cz++) {
                VoxelChunkData before = terrainOnly(cx, cz);
                VoxelChunkData after  = carved(cx, cz);
                carvedCount += countNewlyEmptied(before, after);
            }
        }
        assertTrue(carvedCount > 0, "Expected caves to empty some solid blocks");
    }

    @Test
    void carvingLeavesSolidTerrainBehind() {
        VoxelChunkData after = carved(0, 0);
        int solid = 0;
        for (byte b : after.blocks()) {
            if (b != WorldConstants.BLOCK_AIR && b != WorldConstants.BLOCK_WATER) solid++;
        }
        assertTrue(solid > 0, "Caves must not dissolve the entire chunk");
    }

    @Test
    void bedrockFloorIsNeverCarved() {
        VoxelChunkData before = terrainOnly(0, 0);
        VoxelChunkData after  = carved(0, 0);
        for (int y = 0; y < CAVE_FLOOR; y++)
            for (int z = 0; z < SX; z++)
                for (int x = 0; x < SX; x++)
                    assertEquals(before.get(x, y, z), after.get(x, y, z),
                            "Floor block at y=" + y + " was carved");
    }

    @Test
    void cavesAreDeterministicForSameSeed() {
        VoxelChunkData a = carved(3, -2);
        WorldGenSystem gen2 = new WorldGenSystem(SEED);
        VoxelChunkData b = VoxelChunkData.empty();
        gen2.generateTerrain(b, 3, -2);
        gen2.carveCaves(b, 3, -2);
        assertArrayEquals(a.blocks(), b.blocks());
    }

    @Test
    void carvingNeverTurnsAirIntoSolid() {
        VoxelChunkData before = terrainOnly(0, 0);
        VoxelChunkData after  = carved(0, 0);
        for (int y = 0; y < H; y++)
            for (int z = 0; z < SX; z++)
                for (int x = 0; x < SX; x++) {
                    if (before.get(x, y, z) == WorldConstants.BLOCK_AIR) {
                        assertEquals(WorldConstants.BLOCK_AIR, after.get(x, y, z),
                                "Carving must only remove blocks, never add them");
                    }
                }
    }

    private static int countNewlyEmptied(VoxelChunkData before, VoxelChunkData after) {
        int count = 0;
        byte[] b = before.blocks();
        byte[] a = after.blocks();
        for (int i = 0; i < b.length; i++) {
            boolean wasSolid = b[i] != WorldConstants.BLOCK_AIR && b[i] != WorldConstants.BLOCK_WATER;
            if (wasSolid && a[i] == WorldConstants.BLOCK_AIR) count++;
        }
        return count;
    }
}
