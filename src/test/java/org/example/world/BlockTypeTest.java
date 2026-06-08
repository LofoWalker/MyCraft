package org.example.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BlockTypeTest {

    @Test
    void idsMapToMatchingBlockType() {
        assertSame(BlockType.AIR,    BlockType.byId(WorldConstants.BLOCK_AIR));
        assertSame(BlockType.STONE,  BlockType.byId(WorldConstants.BLOCK_STONE));
        assertSame(BlockType.DIRT,   BlockType.byId(WorldConstants.BLOCK_DIRT));
        assertSame(BlockType.GRASS,  BlockType.byId(WorldConstants.BLOCK_GRASS));
        assertSame(BlockType.WOOD,   BlockType.byId(WorldConstants.BLOCK_WOOD));
        assertSame(BlockType.LEAVES, BlockType.byId(WorldConstants.BLOCK_LEAVES));
        assertSame(BlockType.WATER,  BlockType.byId(WorldConstants.BLOCK_WATER));
    }

    @Test
    void unmappedIdFallsBackToUnknownMagenta() {
        BlockType unknown = BlockType.byId((byte) 99);
        assertSame(BlockType.UNKNOWN, unknown);
        assertArrayEquals(new float[]{ 1f, 0f, 1f }, unknown.colorTop());
    }

    @Test
    void airAndWaterAreNotSolid() {
        assertFalse(BlockType.AIR.solid());
        assertFalse(BlockType.WATER.solid());
    }

    @Test
    void terrainBlocksAreSolid() {
        assertTrue(BlockType.STONE.solid());
        assertTrue(BlockType.DIRT.solid());
        assertTrue(BlockType.GRASS.solid());
        assertTrue(BlockType.WOOD.solid());
        assertTrue(BlockType.LEAVES.solid());
    }

    @Test
    void grassTopIsGreenerThanItsSides() {
        assertTrue(BlockType.GRASS.colorTop()[1] > BlockType.GRASS.colorSide()[1],
                "Grass top should be greener than its dirt-colored sides");
    }

    @Test
    void uniformBlocksShareTopAndSideColor() {
        assertArrayEquals(BlockType.STONE.colorTop(), BlockType.STONE.colorSide());
    }
}
