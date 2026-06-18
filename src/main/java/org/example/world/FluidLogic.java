package org.example.world;

/**
 * Pure fluid propagation algorithm. Operates on a {@link FluidGrid} so it can be tested
 * on a small in-memory grid without any ECS, OpenGL, or chunk-streaming dependency.
 *
 * <h3>Level encoding</h3>
 * Water source  = BLOCK_WATER (6)            → logical level FLUID_SOURCE_LEVEL (8)
 * Flowing water = BLOCK_WATER_FLOW_BASE + level, i.e. ids 10..16 → levels 7..1
 * Lava source   = BLOCK_LAVA (17)            → logical level FLUID_SOURCE_LEVEL (8)
 * Flowing lava  = BLOCK_LAVA_FLOW_BASE + level, i.e. ids 18..23 → levels 6..1
 * Obsidian      = BLOCK_OBSIDIAN (24)        → lava-source solidified by water
 * Stone is produced when flowing lava meets water.
 *
 * <h3>Source vs flowing</h3>
 * Source blocks (level 8) are infinite: they never drain, even when isolated.
 * Flowing blocks without a neighbouring source of equal-or-higher level dry to AIR.
 */
public final class FluidLogic {

    private FluidLogic() {}

    // -----------------------------------------------------------------------
    // Query helpers
    // -----------------------------------------------------------------------

    /** Returns true when the block id represents any water (source or flowing). */
    public static boolean isWater(byte id) {
        int i = id & 0xFF;
        return i == WorldConstants.BLOCK_WATER
            || (i > WorldConstants.BLOCK_WATER_FLOW_BASE && i <= WorldConstants.BLOCK_WATER_FLOW_BASE + WorldConstants.FLUID_MAX_FLOW_LEVEL);
    }

    /** Returns true when the block id represents any lava (source or flowing). */
    public static boolean isLava(byte id) {
        int i = id & 0xFF;
        return i == WorldConstants.BLOCK_LAVA
            || (i > WorldConstants.BLOCK_LAVA_FLOW_BASE && i <= WorldConstants.BLOCK_LAVA_FLOW_BASE + WorldConstants.FLUID_MAX_FLOW_LEVEL - 1);
    }

    /** Returns true for any fluid block (water or lava). */
    public static boolean isFluid(byte id) {
        return isWater(id) || isLava(id);
    }

    /**
     * Returns the logical level of a fluid block (1..FLUID_SOURCE_LEVEL),
     * or 0 if the block is not a fluid.
     */
    public static int fluidLevel(byte id) {
        int i = id & 0xFF;
        if (i == WorldConstants.BLOCK_WATER) return WorldConstants.FLUID_SOURCE_LEVEL;
        if (i > WorldConstants.BLOCK_WATER_FLOW_BASE
                && i <= WorldConstants.BLOCK_WATER_FLOW_BASE + WorldConstants.FLUID_MAX_FLOW_LEVEL) {
            return i - WorldConstants.BLOCK_WATER_FLOW_BASE;
        }
        if (i == WorldConstants.BLOCK_LAVA) return WorldConstants.FLUID_SOURCE_LEVEL;
        if (i > WorldConstants.BLOCK_LAVA_FLOW_BASE
                && i <= WorldConstants.BLOCK_LAVA_FLOW_BASE + WorldConstants.FLUID_MAX_FLOW_LEVEL - 1) {
            return i - WorldConstants.BLOCK_LAVA_FLOW_BASE;
        }
        return 0;
    }

    /** Returns true when the block id is a fluid source (infinite, never drains). */
    public static boolean isSource(byte id) {
        int i = id & 0xFF;
        return i == WorldConstants.BLOCK_WATER || i == WorldConstants.BLOCK_LAVA;
    }

    /**
     * Returns the block id for water at the given logical level, or BLOCK_AIR for level 0.
     * Level FLUID_SOURCE_LEVEL returns BLOCK_WATER (source).
     */
    public static byte waterBlockForLevel(int level) {
        if (level <= 0) return WorldConstants.BLOCK_AIR;
        if (level >= WorldConstants.FLUID_SOURCE_LEVEL) return WorldConstants.BLOCK_WATER;
        return (byte) (WorldConstants.BLOCK_WATER_FLOW_BASE + level);
    }

    /**
     * Returns the block id for lava at the given logical level, or BLOCK_AIR for level 0.
     * Level FLUID_SOURCE_LEVEL returns BLOCK_LAVA (source).
     */
    public static byte lavaBlockForLevel(int level) {
        if (level <= 0) return WorldConstants.BLOCK_AIR;
        if (level >= WorldConstants.FLUID_SOURCE_LEVEL) return WorldConstants.BLOCK_LAVA;
        return (byte) (WorldConstants.BLOCK_LAVA_FLOW_BASE + level);
    }

    // -----------------------------------------------------------------------
    // Cell evaluation
    // -----------------------------------------------------------------------

    /**
     * Evaluates one fluid cell. Reads the current block, determines the correct level
     * from neighbouring cells, writes any change back to the grid, and enqueues the
     * affected neighbours via {@code onNeighbourChanged}.
     *
     * @param grid                 read/write accessor for block data
     * @param wx wy wz             world-space coordinates of the cell being evaluated
     * @param onNeighbourChanged   callback invoked for each neighbour whose position
     *                             should be re-enqueued (never null)
     */
    public static void evaluateCell(FluidGrid grid, int wx, int wy, int wz,
                                    NeighbourCallback onNeighbourChanged) {
        byte current = grid.getBlock(wx, wy, wz);
        if (!isFluid(current)) return;

        if (isWater(current)) {
            evaluateWaterCell(grid, wx, wy, wz, current, onNeighbourChanged);
        } else {
            evaluateLavaCell(grid, wx, wy, wz, current, onNeighbourChanged);
        }
    }

    private static void evaluateWaterCell(FluidGrid grid, int wx, int wy, int wz,
                                          byte current, NeighbourCallback cb) {
        // Source blocks never drain or move; they may still spread to empty neighbours.
        if (isSource(current)) {
            spreadDown(grid, wx, wy, wz, true, cb);
            spreadHorizontal(grid, wx, wy, wz, WorldConstants.FLUID_SOURCE_LEVEL, true, cb);
            return;
        }

        int bestLevel = bestIncomingWaterLevel(grid, wx, wy, wz);
        int targetLevel = targetFlowingLevel(bestLevel);

        if (targetLevel < WorldConstants.FLUID_MIN_LEVEL) {
            // No valid source nearby: dry up.
            writeAndNotify(grid, wx, wy, wz, WorldConstants.BLOCK_AIR, cb);
            return;
        }

        byte desired = waterBlockForLevel(targetLevel);
        if (desired != current) {
            writeAndNotify(grid, wx, wy, wz, desired, cb);
        }
        // If this cell has level, try spreading to air below / horizontal neighbours.
        spreadDown(grid, wx, wy, wz, false, cb);
        spreadHorizontal(grid, wx, wy, wz, targetLevel, false, cb);
    }

    private static void evaluateLavaCell(FluidGrid grid, int wx, int wy, int wz,
                                         byte current, NeighbourCallback cb) {
        if (isSource(current)) {
            // Lava source: check for water contact first.
            if (hasAdjacentWater(grid, wx, wy, wz)) {
                writeAndNotify(grid, wx, wy, wz, WorldConstants.BLOCK_OBSIDIAN, cb);
                return;
            }
            spreadDown(grid, wx, wy, wz, true, cb);
            spreadHorizontal(grid, wx, wy, wz, WorldConstants.FLUID_SOURCE_LEVEL, true, cb);
            return;
        }

        // Flowing lava: solidify to stone if water is adjacent.
        if (hasAdjacentWater(grid, wx, wy, wz)) {
            writeAndNotify(grid, wx, wy, wz, WorldConstants.BLOCK_STONE, cb);
            return;
        }

        int bestLevel = bestIncomingLavaLevel(grid, wx, wy, wz);
        int targetLevel = targetFlowingLevel(bestLevel);

        if (targetLevel < WorldConstants.FLUID_MIN_LEVEL) {
            writeAndNotify(grid, wx, wy, wz, WorldConstants.BLOCK_AIR, cb);
            return;
        }

        byte desired = lavaBlockForLevel(targetLevel);
        if (desired != current) {
            writeAndNotify(grid, wx, wy, wz, desired, cb);
        }
        spreadDown(grid, wx, wy, wz, false, cb);
        spreadHorizontal(grid, wx, wy, wz, targetLevel, false, cb);
    }

    // -----------------------------------------------------------------------
    // Spread helpers
    // -----------------------------------------------------------------------

    private static void spreadDown(FluidGrid grid, int wx, int wy, int wz,
                                   boolean isSource, NeighbourCallback cb) {
        byte current = grid.getBlock(wx, wy, wz);
        // Falling fluid drops fast: fill the whole air column below in one pass, down to the
        // first obstacle. Each cell is filled at source level (full column) per the ticket spec.
        byte fill = isWater(current) ? WorldConstants.BLOCK_WATER : WorldConstants.BLOCK_LAVA;
        for (int y = wy - 1; y >= 0; y--) {
            if (!canFlowInto(grid.getBlock(wx, y, wz))) break;
            writeAndNotify(grid, wx, y, wz, fill, cb);
        }
    }

    private static void spreadHorizontal(FluidGrid grid, int wx, int wy, int wz,
                                         int currentLevel, boolean isSource, NeighbourCallback cb) {
        // Cannot spread horizontally if already at minimum level.
        if (currentLevel <= WorldConstants.FLUID_MIN_LEVEL && !isSource) return;

        byte current = grid.getBlock(wx, wy, wz);
        int spreadLevel = isSource ? WorldConstants.FLUID_MAX_FLOW_LEVEL : currentLevel - 1;

        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
        for (int[] d : dirs) {
            int nx = wx + d[0], nz = wz + d[1];
            byte neighbour = grid.getBlock(nx, wy, nz);
            if (!canFlowInto(neighbour)) continue;
            // Only spread if the neighbour would get a higher level than it currently has.
            int neighbourLevel = fluidLevel(neighbour);
            if (spreadLevel <= neighbourLevel) continue;
            byte fill = isWater(current) ? waterBlockForLevel(spreadLevel) : lavaBlockForLevel(spreadLevel);
            writeAndNotify(grid, nx, wy, nz, fill, cb);
        }
    }

    // -----------------------------------------------------------------------
    // Level computation helpers
    // -----------------------------------------------------------------------

    /**
     * Finds the highest water level among the 4 horizontal neighbours and the cell above
     * (a cell above at any level acts like a source-level supply: it fills the whole column below).
     */
    private static int bestIncomingWaterLevel(FluidGrid grid, int wx, int wy, int wz) {
        int best = 0;
        byte above = grid.getBlock(wx, wy + 1, wz);
        if (isWater(above)) return WorldConstants.FLUID_SOURCE_LEVEL; // flowing down from above

        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
        for (int[] d : dirs) {
            byte nb = grid.getBlock(wx + d[0], wy, wz + d[1]);
            if (isWater(nb)) best = Math.max(best, fluidLevel(nb));
        }
        return best;
    }

    private static int bestIncomingLavaLevel(FluidGrid grid, int wx, int wy, int wz) {
        int best = 0;
        byte above = grid.getBlock(wx, wy + 1, wz);
        if (isLava(above)) return WorldConstants.FLUID_SOURCE_LEVEL;

        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
        for (int[] d : dirs) {
            byte nb = grid.getBlock(wx + d[0], wy, wz + d[1]);
            if (isLava(nb)) best = Math.max(best, fluidLevel(nb));
        }
        return best;
    }

    /** Level a flowing cell should settle at given the best incoming supply level. */
    private static int targetFlowingLevel(int incomingLevel) {
        if (incomingLevel >= WorldConstants.FLUID_SOURCE_LEVEL) {
            return WorldConstants.FLUID_MAX_FLOW_LEVEL;
        }
        return incomingLevel - 1;
    }

    // -----------------------------------------------------------------------
    // Misc helpers
    // -----------------------------------------------------------------------

    /** True when a fluid can flow into this block (air or lower-level same fluid). */
    private static boolean canFlowInto(byte id) {
        return id == WorldConstants.BLOCK_AIR;
    }

    private static boolean hasAdjacentWater(FluidGrid grid, int wx, int wy, int wz) {
        int[][] dirs = {{1,0,0},{-1,0,0},{0,0,1},{0,0,-1},{0,1,0},{0,-1,0}};
        for (int[] d : dirs) {
            if (isWater(grid.getBlock(wx + d[0], wy + d[1], wz + d[2]))) return true;
        }
        return false;
    }

    private static void writeAndNotify(FluidGrid grid, int wx, int wy, int wz,
                                       byte blockId, NeighbourCallback cb) {
        grid.setBlock(wx, wy, wz, blockId);
        cb.enqueue(wx, wy, wz);
        // Enqueue 4 horizontal neighbours and the cell above so they re-evaluate.
        int[][] dirs = {{1,0,0},{-1,0,0},{0,0,1},{0,0,-1},{0,1,0}};
        for (int[] d : dirs) cb.enqueue(wx + d[0], wy + d[1], wz + d[2]);
    }

    // -----------------------------------------------------------------------
    // Callback type
    // -----------------------------------------------------------------------

    /** Called for each cell position that should be added to the update queue. */
    @FunctionalInterface
    public interface NeighbourCallback {
        void enqueue(int wx, int wy, int wz);
    }
}
