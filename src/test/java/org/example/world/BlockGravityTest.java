package org.example.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for block-gravity rules: sand and gravel fall when unsupported.
 * Uses SimpleFluidGrid as a lightweight in-memory voxel grid — no ECS, no OpenGL.
 */
class BlockGravityTest {

    private static final int W = 5, H = 16, D = 5;

    // -----------------------------------------------------------------------
    // Helper: run gravity steps until nothing moves (or step limit reached)
    // -----------------------------------------------------------------------

    private static void propagate(SimpleFluidGrid grid, int maxSteps) {
        boolean moved = true;
        int steps = 0;
        while (moved && steps < maxSteps) {
            moved = false;
            for (int x = 0; x < W; x++) {
                for (int y = 1; y < H; y++) {
                    for (int z = 0; z < D; z++) {
                        if (BlockGravityRules.stepFall(grid, x, y, z)) {
                            moved = true;
                        }
                    }
                }
            }
            steps++;
        }
    }

    // -----------------------------------------------------------------------
    // Sand falls to first solid support
    // -----------------------------------------------------------------------

    @Test
    void suspendedSandFallsToFirstSupport() {
        SimpleFluidGrid grid = new SimpleFluidGrid(W, H, D);
        // Solid floor at y=0 (stone).
        for (int x = 0; x < W; x++) {
            for (int z = 0; z < D; z++) {
                grid.setBlock(x, 0, z, WorldConstants.BLOCK_STONE);
            }
        }
        // Sand suspended at y=5 with air below (y=1..4 is air).
        grid.setBlock(2, 5, 2, WorldConstants.BLOCK_SAND);

        propagate(grid, 100);

        // Sand should have fallen to y=1 (resting on the stone floor at y=0).
        assertEquals(WorldConstants.BLOCK_AIR, grid.getBlock(2, 5, 2),
                "Sand must no longer be at y=5 after falling");
        assertEquals(WorldConstants.BLOCK_SAND, grid.getBlock(2, 1, 2),
                "Sand must rest at y=1, directly above the stone floor");
    }

    // -----------------------------------------------------------------------
    // Sand rests when directly on a solid block
    // -----------------------------------------------------------------------

    @Test
    void supportedSandDoesNotFall() {
        SimpleFluidGrid grid = new SimpleFluidGrid(W, H, D);
        // Stone at y=3; sand sitting on top at y=4.
        grid.setBlock(2, 3, 2, WorldConstants.BLOCK_STONE);
        grid.setBlock(2, 4, 2, WorldConstants.BLOCK_SAND);

        propagate(grid, 20);

        assertEquals(WorldConstants.BLOCK_SAND, grid.getBlock(2, 4, 2),
                "Supported sand must stay in place");
    }

    // -----------------------------------------------------------------------
    // Gravel also falls (same rule as sand)
    // -----------------------------------------------------------------------

    @Test
    void suspendedGravelFallsToFirstSupport() {
        SimpleFluidGrid grid = new SimpleFluidGrid(W, H, D);
        for (int x = 0; x < W; x++) {
            for (int z = 0; z < D; z++) {
                grid.setBlock(x, 0, z, WorldConstants.BLOCK_STONE);
            }
        }
        grid.setBlock(2, 6, 2, WorldConstants.BLOCK_GRAVEL);

        propagate(grid, 100);

        assertEquals(WorldConstants.BLOCK_AIR,   grid.getBlock(2, 6, 2),
                "Gravel must have left y=6");
        assertEquals(WorldConstants.BLOCK_GRAVEL, grid.getBlock(2, 1, 2),
                "Gravel must rest at y=1");
    }

    // -----------------------------------------------------------------------
    // A column of sand collapses
    // -----------------------------------------------------------------------

    @Test
    void columnOfSandCollapsesToFloor() {
        SimpleFluidGrid grid = new SimpleFluidGrid(W, H, D);
        for (int x = 0; x < W; x++) {
            for (int z = 0; z < D; z++) {
                grid.setBlock(x, 0, z, WorldConstants.BLOCK_STONE);
            }
        }
        // Column of 3 sand blocks at y=4,5,6 suspended above air.
        grid.setBlock(2, 4, 2, WorldConstants.BLOCK_SAND);
        grid.setBlock(2, 5, 2, WorldConstants.BLOCK_SAND);
        grid.setBlock(2, 6, 2, WorldConstants.BLOCK_SAND);

        propagate(grid, 200);

        // All three should pack down to y=1,2,3.
        assertEquals(WorldConstants.BLOCK_SAND, grid.getBlock(2, 1, 2), "y=1 should be sand");
        assertEquals(WorldConstants.BLOCK_SAND, grid.getBlock(2, 2, 2), "y=2 should be sand");
        assertEquals(WorldConstants.BLOCK_SAND, grid.getBlock(2, 3, 2), "y=3 should be sand");
        assertEquals(WorldConstants.BLOCK_AIR,  grid.getBlock(2, 4, 2), "y=4 should be air");
    }

    // -----------------------------------------------------------------------
    // Non-gravity block is unaffected
    // -----------------------------------------------------------------------

    @Test
    void stoneDoesNotFall() {
        SimpleFluidGrid grid = new SimpleFluidGrid(W, H, D);
        grid.setBlock(2, 5, 2, WorldConstants.BLOCK_STONE);

        propagate(grid, 20);

        assertEquals(WorldConstants.BLOCK_STONE, grid.getBlock(2, 5, 2),
                "Stone must never be moved by gravity");
    }

    // -----------------------------------------------------------------------
    // affectedByGravity flag
    // -----------------------------------------------------------------------

    @Test
    void sandAndGravelAreAffectedByGravity() {
        assertTrue(BlockType.SAND.affectedByGravity(),   "SAND must be gravity-affected");
        assertTrue(BlockType.GRAVEL.affectedByGravity(), "GRAVEL must be gravity-affected");
    }

    @Test
    void stoneAndDirtAreNotAffectedByGravity() {
        assertFalse(BlockType.STONE.affectedByGravity(), "STONE must not be gravity-affected");
        assertFalse(BlockType.DIRT.affectedByGravity(),  "DIRT must not be gravity-affected");
    }
}
