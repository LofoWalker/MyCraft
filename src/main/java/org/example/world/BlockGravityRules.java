package org.example.world;

/**
 * Pure block-gravity rules: given a block at (wx, wy, wz) and access to the voxel grid,
 * determines whether and where it should fall. Operates on a {@link FluidGrid} so tests
 * can run on a tiny in-memory grid without ECS or OpenGL.
 *
 * <p>Algorithm: move the block one cell down per call. The caller (BlockGravitySystem)
 * is responsible for re-queuing until the block comes to rest.
 */
public final class BlockGravityRules {

    private BlockGravityRules() {}

    /**
     * Returns true when the block at (wx, wy, wz) is affected by gravity and the
     * cell directly below is passable (air or fluid). The caller must then move it.
     */
    public static boolean shouldFall(FluidGrid grid, int wx, int wy, int wz) {
        byte block = grid.getBlock(wx, wy, wz);
        if (!BlockType.byId(block).affectedByGravity()) return false;
        if (wy <= 0) return false;
        byte below = grid.getBlock(wx, wy - 1, wz);
        return isPassable(below);
    }

    /**
     * Moves the gravity block at (wx, wy, wz) one cell downward.
     * No-op when {@link #shouldFall} would return false.
     * Returns true when a move was performed (so the caller knows to re-queue).
     */
    public static boolean stepFall(FluidGrid grid, int wx, int wy, int wz) {
        if (!shouldFall(grid, wx, wy, wz)) return false;
        byte block = grid.getBlock(wx, wy, wz);
        grid.setBlock(wx, wy,     wz, WorldConstants.BLOCK_AIR);
        grid.setBlock(wx, wy - 1, wz, block);
        return true;
    }

    /** True when a gravity block can displace this block (air or any fluid). */
    static boolean isPassable(byte id) {
        return id == WorldConstants.BLOCK_AIR || FluidLogic.isFluid(id);
    }
}
