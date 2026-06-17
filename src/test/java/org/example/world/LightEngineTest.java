package org.example.world;

import org.example.components.VoxelChunkData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LightEngineTest {

    private static final int SX  = WorldConstants.CHUNK_SIZE_XZ;
    private static final int MAX = WorldConstants.MAX_LIGHT_LEVEL;

    private static int index(int x, int y, int z) {
        return x + z * SX + y * SX * SX;
    }

    private static int skylight(byte[] light, int x, int y, int z) {
        return LightEngine.skylight(light[index(x, y, z)]);
    }

    private static int blocklight(byte[] light, int x, int y, int z) {
        return LightEngine.blocklight(light[index(x, y, z)]);
    }

    @Test
    void openAirColumnIsFullSkylightFromTopDown() {
        byte[] light = LightEngine.computeLight(VoxelChunkData.empty());
        int top = WorldConstants.WORLD_HEIGHT - 1;
        assertEquals(MAX, skylight(light, 0, top, 0), "Sky cell must be full skylight");
        assertEquals(MAX, skylight(light, 0, 0, 0),   "Air all the way down stays full skylight");
    }

    @Test
    void skylightStopsAtOpaqueBlock() {
        VoxelChunkData data = VoxelChunkData.empty();
        int groundY = 10;
        for (int x = 0; x < SX; x++)
            for (int z = 0; z < SX; z++)
                data.set(x, groundY, z, WorldConstants.BLOCK_STONE);

        byte[] light = LightEngine.computeLight(data);

        assertEquals(MAX, skylight(light, 0, groundY + 1, 0), "Air just above ground is full skylight");
        assertEquals(0,   skylight(light, 0, groundY - 1, 0), "Sealed cell below solid floor is dark");
    }

    @Test
    void skylightDecrementsHorizontallyUnderOverhang() {
        VoxelChunkData data = VoxelChunkData.empty();
        // A solid roof at y=20 over the whole chunk except one open shaft at x=0, leaving a sheltered
        // cavity at y=19 that only receives skylight that creeps in sideways from the shaft.
        int roofY = 20, cavityY = 19;
        for (int x = 1; x < SX; x++)
            for (int z = 0; z < SX; z++)
                data.set(x, roofY, z, WorldConstants.BLOCK_STONE);

        byte[] light = LightEngine.computeLight(data);

        assertEquals(MAX,     skylight(light, 0, cavityY, 0), "Open shaft column is full skylight");
        assertEquals(MAX - 1, skylight(light, 1, cavityY, 0), "One block under the overhang drops by one");
        assertEquals(MAX - 2, skylight(light, 2, cavityY, 0), "Two blocks in drops by two");
    }

    @Test
    void torchEmitsDecreasingHalo() {
        VoxelChunkData data = VoxelChunkData.empty();
        int cx = 16, cy = 100, cz = 16; // deep enough that skylight does not reach
        data.set(cx, cy, cz, WorldConstants.BLOCK_TORCH);

        byte[] light = LightEngine.computeLight(data);

        assertEquals(WorldConstants.TORCH_EMISSION,     blocklight(light, cx, cy, cz),     "Torch cell");
        assertEquals(WorldConstants.TORCH_EMISSION - 1, blocklight(light, cx + 1, cy, cz), "One block away");
        assertEquals(WorldConstants.TORCH_EMISSION - 2, blocklight(light, cx + 2, cy, cz), "Two blocks away");
        assertEquals(WorldConstants.TORCH_EMISSION - 3, blocklight(light, cx + 3, cy, cz), "Three blocks away");
    }

    @Test
    void opaqueWallBlocksBlocklightPropagation() {
        VoxelChunkData data = VoxelChunkData.empty();
        int cx = 16, cy = 100, cz = 16;
        data.set(cx, cy, cz, WorldConstants.BLOCK_TORCH);
        data.set(cx + 1, cy, cz, WorldConstants.BLOCK_STONE); // wall right beside the torch

        byte[] light = LightEngine.computeLight(data);

        assertEquals(0, blocklight(light, cx + 1, cy, cz), "Opaque wall cell carries no blocklight");
        // The cell behind the wall must not get the straight-line value; light has to detour around it.
        assertTrue(blocklight(light, cx + 2, cy, cz) < WorldConstants.TORCH_EMISSION - 1,
                "Wall forces light to detour, so the cell behind it is dimmer than a straight line");
    }

    @Test
    void torchHaloIsBoundedAndNonNegative() {
        VoxelChunkData data = VoxelChunkData.empty();
        int cx = 16, cy = 100, cz = 16;
        data.set(cx, cy, cz, WorldConstants.BLOCK_TORCH);

        byte[] light = LightEngine.computeLight(data);

        // Far enough that the torch can no longer reach: blocklight has decayed to zero.
        assertEquals(0, blocklight(light, cx + WorldConstants.TORCH_EMISSION + 1, cy, cz));
    }

    @Test
    void computationIsDeterministic() {
        VoxelChunkData data = VoxelChunkData.empty();
        data.set(5, 100, 5, WorldConstants.BLOCK_TORCH);
        for (int x = 0; x < SX; x++)
            for (int z = 0; z < SX; z++)
                data.set(x, 40, z, WorldConstants.BLOCK_STONE);

        byte[] a = LightEngine.computeLight(data);
        byte[] b = LightEngine.computeLight(data);

        assertArrayEquals(a, b, "Same chunk must produce the same light field");
    }

    @Test
    void effectiveLevelIsMaxOfSkyAndBlock() {
        byte packed = (byte) ((3 << 4) | 11); // skylight 3, blocklight 11
        assertEquals(3,  LightEngine.skylight(packed));
        assertEquals(11, LightEngine.blocklight(packed));
        assertEquals(11, LightEngine.effectiveLevel(packed));
    }
}
