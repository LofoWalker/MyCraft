package org.example.world;

/**
 * Minimal voxel accessor used by FluidLogic so the propagation algorithm
 * can be tested on an in-memory grid without any ECS or OpenGL dependency.
 *
 * Implementors: SimpleFluidGrid (tests), WorldFluidGrid (live game).
 */
public interface FluidGrid {

    /** Returns the block id at world-space (wx, wy, wz), or BLOCK_AIR when out of bounds. */
    byte getBlock(int wx, int wy, int wz);

    /** Writes blockId at (wx, wy, wz). No-op when out of bounds. */
    void setBlock(int wx, int wy, int wz, byte blockId);
}
