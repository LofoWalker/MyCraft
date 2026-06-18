package org.example.systems;

import org.example.world.FluidLogic;
import org.example.world.SimpleFluidGrid;
import org.example.world.WorldConstants;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Deque;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for lava/water interaction: lava source + water → obsidian;
 * flowing lava + water → stone.
 */
class FluidInteractionTest {

    private static void propagate(SimpleFluidGrid grid, int maxSteps) {
        Deque<long[]> queue = new ArrayDeque<>();
        for (int x = 0; x < 15; x++) for (int y = 0; y < 15; y++) for (int z = 0; z < 15; z++) {
            if (FluidLogic.isFluid(grid.getBlock(x, y, z))) {
                queue.add(new long[]{x, y, z});
            }
        }
        int steps = 0;
        while (!queue.isEmpty() && steps < maxSteps) {
            long[] pos = queue.poll();
            FluidLogic.evaluateCell(grid, (int)pos[0], (int)pos[1], (int)pos[2],
                    (nx, ny, nz) -> queue.add(new long[]{nx, ny, nz}));
            steps++;
        }
    }

    // -----------------------------------------------------------------------
    // Lava source + adjacent water → obsidian
    // -----------------------------------------------------------------------

    @Test
    void lavaSourceTurnedToObsidianByWater() {
        SimpleFluidGrid grid = new SimpleFluidGrid(15, 15, 15);
        // Floor.
        for (int x = 0; x < 15; x++) for (int z = 0; z < 15; z++) {
            grid.setBlock(x, 4, z, WorldConstants.BLOCK_STONE);
        }
        grid.setBlock(7, 5, 7, WorldConstants.BLOCK_LAVA);    // lava source
        grid.setBlock(8, 5, 7, WorldConstants.BLOCK_WATER);   // water adjacent

        propagate(grid, 200);

        assertEquals(WorldConstants.BLOCK_OBSIDIAN, grid.getBlock(7, 5, 7),
                "Lava source next to water should become obsidian");
    }

    // -----------------------------------------------------------------------
    // Flowing lava + adjacent water → stone
    // -----------------------------------------------------------------------

    @Test
    void flowingLavaTurnedToStoneByWater() {
        SimpleFluidGrid grid = new SimpleFluidGrid(15, 15, 15);
        for (int x = 0; x < 15; x++) for (int z = 0; z < 15; z++) {
            grid.setBlock(x, 4, z, WorldConstants.BLOCK_STONE);
        }
        // Flowing lava level 3 (non-source).
        grid.setBlock(7, 5, 7, FluidLogic.lavaBlockForLevel(3));
        grid.setBlock(8, 5, 7, WorldConstants.BLOCK_WATER);

        propagate(grid, 200);

        assertEquals(WorldConstants.BLOCK_STONE, grid.getBlock(7, 5, 7),
                "Flowing lava next to water should become stone");
    }

    // -----------------------------------------------------------------------
    // Without water, lava source persists
    // -----------------------------------------------------------------------

    @Test
    void lavaSourceWithoutWaterPersists() {
        SimpleFluidGrid grid = new SimpleFluidGrid(10, 10, 10);
        for (int x = 0; x < 10; x++) for (int z = 0; z < 10; z++) {
            grid.setBlock(x, 4, z, WorldConstants.BLOCK_STONE);
        }
        grid.setBlock(5, 5, 5, WorldConstants.BLOCK_LAVA);

        propagate(grid, 200);

        assertEquals(WorldConstants.BLOCK_LAVA, grid.getBlock(5, 5, 5),
                "Lava source without water must remain lava");
    }

    // -----------------------------------------------------------------------
    // Water over lava: cell below lava source has water above → obsidian
    // -----------------------------------------------------------------------

    @Test
    void waterAboveLavaSourceCreatesObsidian() {
        SimpleFluidGrid grid = new SimpleFluidGrid(10, 10, 10);
        grid.setBlock(5, 5, 5, WorldConstants.BLOCK_LAVA);    // lava source
        grid.setBlock(5, 6, 5, WorldConstants.BLOCK_WATER);   // water directly above

        propagate(grid, 200);

        assertEquals(WorldConstants.BLOCK_OBSIDIAN, grid.getBlock(5, 5, 5),
                "Lava source touched by water from above should solidify to obsidian");
    }

    // -----------------------------------------------------------------------
    // Lava level encoding
    // -----------------------------------------------------------------------

    @Test
    void isLavaRecognisesSourceAndFlowing() {
        assertTrue(FluidLogic.isLava(WorldConstants.BLOCK_LAVA), "Should recognise source lava");
        for (int level = 1; level < WorldConstants.FLUID_MAX_FLOW_LEVEL; level++) {
            byte id = FluidLogic.lavaBlockForLevel(level);
            assertTrue(FluidLogic.isLava(id), "Should recognise flowing lava level " + level);
            assertFalse(FluidLogic.isWater(id), "Lava should not be water");
        }
    }

    @Test
    void isWaterRecognisesSourceAndFlowing() {
        assertTrue(FluidLogic.isWater(WorldConstants.BLOCK_WATER), "Should recognise source water");
        for (int level = 1; level <= WorldConstants.FLUID_MAX_FLOW_LEVEL; level++) {
            byte id = FluidLogic.waterBlockForLevel(level);
            assertTrue(FluidLogic.isWater(id), "Should recognise flowing water level " + level);
            assertFalse(FluidLogic.isLava(id), "Water should not be lava");
        }
    }
}
