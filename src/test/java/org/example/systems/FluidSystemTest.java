package org.example.systems;

import org.example.world.FluidLogic;
import org.example.world.SimpleFluidGrid;
import org.example.world.WorldConstants;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Deque;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure unit tests for the fluid propagation algorithm.
 * No ECS, no OpenGL, no chunk streaming — just FluidLogic + SimpleFluidGrid.
 */
class FluidSystemTest {

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** Run fluid evaluation until the queue is empty or the step limit is reached. */
    private static void propagate(SimpleFluidGrid grid, int maxSteps) {
        Deque<long[]> queue = new ArrayDeque<>();

        // Seed with every fluid cell in the grid.
        for (int x = 0; x < 20; x++) {
            for (int y = 0; y < 20; y++) {
                for (int z = 0; z < 20; z++) {
                    if (FluidLogic.isFluid(grid.getBlock(x, y, z))) {
                        queue.add(new long[]{x, y, z});
                    }
                }
            }
        }

        int steps = 0;
        while (!queue.isEmpty() && steps < maxSteps) {
            long[] pos = queue.poll();
            int wx = (int) pos[0], wy = (int) pos[1], wz = (int) pos[2];
            FluidLogic.evaluateCell(grid, wx, wy, wz,
                    (nx, ny, nz) -> queue.add(new long[]{nx, ny, nz}));
            steps++;
        }
    }

    // -----------------------------------------------------------------------
    // Tests: falling
    // -----------------------------------------------------------------------

    @Test
    void waterFallsDownIntoAir() {
        // Grid 5×10×5; water source at (2,8,2); solid floor at y=0.
        SimpleFluidGrid grid = new SimpleFluidGrid(5, 10, 5);
        grid.setBlock(2, 8, 2, WorldConstants.BLOCK_WATER);
        // Solid floor so water stops.
        for (int x = 0; x < 5; x++) for (int z = 0; z < 5; z++) {
            grid.setBlock(x, 0, z, WorldConstants.BLOCK_STONE);
        }

        propagate(grid, 500);

        // All cells below the source down to y=1 should be water.
        for (int y = 1; y <= 8; y++) {
            assertTrue(FluidLogic.isWater(grid.getBlock(2, y, 2)),
                    "Expected water at y=" + y);
        }
    }

    // -----------------------------------------------------------------------
    // Tests: horizontal spreading
    // -----------------------------------------------------------------------

    @Test
    void waterSpreadsHorizontallyWithDecreasingLevel() {
        // Water source at (10,5,10) on a flat platform; nothing below (falls not of interest here).
        // Place a floor so water can spread.
        SimpleFluidGrid grid = new SimpleFluidGrid(20, 10, 20);
        for (int x = 0; x < 20; x++) for (int z = 0; z < 20; z++) {
            grid.setBlock(x, 4, z, WorldConstants.BLOCK_STONE);
        }
        grid.setBlock(10, 5, 10, WorldConstants.BLOCK_WATER);

        propagate(grid, 2000);

        // At distance 1 horizontally, level should be FLUID_MAX_FLOW_LEVEL (7).
        byte at1 = grid.getBlock(11, 5, 10);
        assertTrue(FluidLogic.isWater(at1), "No water at distance 1");
        assertEquals(WorldConstants.FLUID_MAX_FLOW_LEVEL, FluidLogic.fluidLevel(at1),
                "Level at distance 1 should be " + WorldConstants.FLUID_MAX_FLOW_LEVEL);

        // At distance 2, level should be 6.
        byte at2 = grid.getBlock(12, 5, 10);
        assertTrue(FluidLogic.isWater(at2), "No water at distance 2");
        assertEquals(WorldConstants.FLUID_MAX_FLOW_LEVEL - 1, FluidLogic.fluidLevel(at2),
                "Level at distance 2 should be " + (WorldConstants.FLUID_MAX_FLOW_LEVEL - 1));
    }

    // -----------------------------------------------------------------------
    // Tests: isolated cell dries up
    // -----------------------------------------------------------------------

    @Test
    void isolatedFlowingCellDriesToAir() {
        SimpleFluidGrid grid = new SimpleFluidGrid(10, 10, 10);
        // Floor so it doesn't fall.
        grid.setBlock(5, 4, 5, WorldConstants.BLOCK_STONE);
        // Isolated flowing water cell with level 3 (no source nearby).
        grid.setBlock(5, 5, 5, FluidLogic.waterBlockForLevel(3));

        propagate(grid, 100);

        assertEquals(WorldConstants.BLOCK_AIR, grid.getBlock(5, 5, 5),
                "Isolated flowing cell should dry to AIR");
    }

    // -----------------------------------------------------------------------
    // Tests: wall stops flow
    // -----------------------------------------------------------------------

    @Test
    void wallBlocksHorizontalFlow() {
        SimpleFluidGrid grid = new SimpleFluidGrid(15, 10, 5);
        // Floor.
        for (int x = 0; x < 15; x++) for (int z = 0; z < 5; z++) {
            grid.setBlock(x, 4, z, WorldConstants.BLOCK_STONE);
        }
        // Wall of stone between x=5 and x=6.
        for (int y = 5; y < 10; y++) for (int z = 0; z < 5; z++) {
            grid.setBlock(5, y, z, WorldConstants.BLOCK_STONE);
        }
        // Water source at x=1.
        grid.setBlock(1, 5, 2, WorldConstants.BLOCK_WATER);

        propagate(grid, 2000);

        // Right of the wall (x>=6) should be dry.
        for (int x = 6; x < 15; x++) {
            assertFalse(FluidLogic.isWater(grid.getBlock(x, 5, 2)),
                    "Water should not pass the wall at x=" + x);
        }
        // Left of the wall (x=2..4) should be wet.
        assertTrue(FluidLogic.isWater(grid.getBlock(2, 5, 2)),
                "Water should spread left of the wall");
    }

    // -----------------------------------------------------------------------
    // Tests: level encoding round-trip
    // -----------------------------------------------------------------------

    @Test
    void waterBlockForLevelRoundTrip() {
        for (int level = 1; level <= WorldConstants.FLUID_MAX_FLOW_LEVEL; level++) {
            byte id = FluidLogic.waterBlockForLevel(level);
            assertEquals(level, FluidLogic.fluidLevel(id),
                    "Round-trip failed for water level " + level);
        }
        // Source level.
        byte src = FluidLogic.waterBlockForLevel(WorldConstants.FLUID_SOURCE_LEVEL);
        assertEquals(WorldConstants.FLUID_SOURCE_LEVEL, FluidLogic.fluidLevel(src));
    }

    @Test
    void lavaBlockForLevelRoundTrip() {
        // Lava flowing levels 1..6 (one less than water due to id layout).
        for (int level = 1; level < WorldConstants.FLUID_MAX_FLOW_LEVEL; level++) {
            byte id = FluidLogic.lavaBlockForLevel(level);
            assertEquals(level, FluidLogic.fluidLevel(id),
                    "Round-trip failed for lava level " + level);
        }
        byte src = FluidLogic.lavaBlockForLevel(WorldConstants.FLUID_SOURCE_LEVEL);
        assertEquals(WorldConstants.FLUID_SOURCE_LEVEL, FluidLogic.fluidLevel(src));
    }

    // -----------------------------------------------------------------------
    // Tests: source stays persistent
    // -----------------------------------------------------------------------

    @Test
    void sourceWaterNeverDrains() {
        SimpleFluidGrid grid = new SimpleFluidGrid(5, 5, 5);
        // Source water isolated in mid-air — still must remain a source.
        grid.setBlock(2, 2, 2, WorldConstants.BLOCK_WATER);

        propagate(grid, 200);

        assertEquals(WorldConstants.BLOCK_WATER, grid.getBlock(2, 2, 2),
                "Source water must never drain");
    }

    // -----------------------------------------------------------------------
    // Tests: position encoding helpers
    // -----------------------------------------------------------------------

    @Test
    void positionEncodingRoundTrip() {
        int[][] cases = {{0,0,0},{-100,200,50},{1000,128,-999},{Integer.MIN_VALUE>>9, 511, 0}};
        for (int[] c : cases) {
            long encoded = FluidSystem.encodePos(c[0], c[1], c[2]);
            assertEquals(c[0], FluidSystem.decodeX(encoded), "X mismatch");
            // Y is 9-bit; clamp to range.
            assertEquals(c[1] & 0x1FF, FluidSystem.decodeY(encoded), "Y mismatch");
        }
    }
}
