package org.example.world;

/**
 * Test-only FluidGrid backed by a fixed-size 3-D byte array.
 * Coordinates map directly: no chunk system, no ECS.
 */
public final class SimpleFluidGrid implements FluidGrid {

    private final byte[][][] blocks;
    private final int width, height, depth;

    public SimpleFluidGrid(int width, int height, int depth) {
        this.width  = width;
        this.height = height;
        this.depth  = depth;
        this.blocks = new byte[width][height][depth];
    }

    @Override
    public byte getBlock(int x, int y, int z) {
        if (outOfBounds(x, y, z)) return WorldConstants.BLOCK_AIR;
        return blocks[x][y][z];
    }

    @Override
    public void setBlock(int x, int y, int z, byte blockId) {
        if (outOfBounds(x, y, z)) return;
        blocks[x][y][z] = blockId;
    }

    private boolean outOfBounds(int x, int y, int z) {
        return x < 0 || x >= width || y < 0 || y >= height || z < 0 || z >= depth;
    }
}
