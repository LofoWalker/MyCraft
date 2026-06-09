package org.example.worldgen.stage;

import org.example.components.VoxelChunkData;
import org.example.world.WorldConstants;
import org.example.worldgen.TerrainShape;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CaveStageTest {

    private static final int  SX   = WorldConstants.CHUNK_SIZE_XZ;
    private static final int  H    = WorldConstants.WORLD_HEIGHT;
    private static final long SEED = WorldConstants.WORLD_SEED;

    private final TerrainStage terrain = new TerrainStage(new TerrainShape(SEED));
    private final CaveStage    caves   = new CaveStage(SEED);

    private VoxelChunkData terrainOnly(int cx, int cz) {
        VoxelChunkData data = VoxelChunkData.empty();
        terrain.apply(data, cx, cz);
        return data;
    }

    private VoxelChunkData carved(int cx, int cz) {
        VoxelChunkData data = terrainOnly(cx, cz);
        caves.apply(data, cx, cz);
        return data;
    }

    @Test
    void carvingOnlyRemovesBlocks() {
        VoxelChunkData before = terrainOnly(0, 0);
        byte[] original = before.blocks().clone();
        VoxelChunkData after = carved(0, 0);
        for (int i = 0; i < original.length; i++) {
            byte carvedBlock = after.blocks()[i];
            assertTrue(carvedBlock == original[i] || carvedBlock == WorldConstants.BLOCK_AIR,
                    "Carving turned block " + original[i] + " into " + carvedBlock + " (only air allowed)");
        }
    }

    @Test
    void carvingNeverRemovesWater() {
        VoxelChunkData before = terrainOnly(0, 0);
        byte[] original = before.blocks().clone();
        VoxelChunkData after = carved(0, 0);
        for (int i = 0; i < original.length; i++) {
            if (original[i] == WorldConstants.BLOCK_WATER) {
                assertEquals(WorldConstants.BLOCK_WATER, after.blocks()[i],
                        "Water at index " + i + " must survive carving");
            }
        }
    }

    @Test
    void carvingNeverTurnsAirIntoSolid() {
        VoxelChunkData before = terrainOnly(0, 0);
        VoxelChunkData after  = carved(0, 0);
        for (int i = 0; i < before.blocks().length; i++) {
            if (before.blocks()[i] == WorldConstants.BLOCK_AIR) {
                assertEquals(WorldConstants.BLOCK_AIR, after.blocks()[i],
                        "Carving must only remove blocks, never add them");
            }
        }
    }

    @Test
    void caveFloorLayersAreNeverCarved() {
        VoxelChunkData before = terrainOnly(0, 0);
        VoxelChunkData after  = carved(0, 0);
        for (int y = 0; y < CaveStage.CAVE_FLOOR; y++) {
            for (int x = 0; x < SX; x++) {
                for (int z = 0; z < SX; z++) {
                    assertEquals(before.get(x, y, z), after.get(x, y, z),
                            "Block in protected floor layer y=" + y + " was modified");
                }
            }
        }
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
    void cavesCarveSomeAirIntoTheRegion() {
        boolean carvedSomething = false;
        outer:
        for (int cx = -3; cx <= 3; cx++) {
            for (int cz = -3; cz <= 3; cz++) {
                VoxelChunkData before = terrainOnly(cx, cz);
                byte[] original = before.blocks().clone();
                VoxelChunkData after = carved(cx, cz);
                for (int i = 0; i < original.length; i++) {
                    if (original[i] != WorldConstants.BLOCK_AIR
                            && after.blocks()[i] == WorldConstants.BLOCK_AIR) {
                        carvedSomething = true;
                        break outer;
                    }
                }
            }
        }
        assertTrue(carvedSomething, "Expected caves to hollow out some solid blocks in the scanned region");
    }

    @Test
    void cavesAreDeterministicForSameSeed() {
        VoxelChunkData a = carved(2, -1);
        CaveStage caves2 = new CaveStage(SEED);
        VoxelChunkData b = terrainOnly(2, -1);
        caves2.apply(b, 2, -1);
        assertArrayEquals(a.blocks(), b.blocks(), "Same seed must carve identical caves");
    }
}
