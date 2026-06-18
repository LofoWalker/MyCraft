package org.example.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BlockDropsTest {

    @Test
    void stoneDropsWithAnyPickaxe() {
        assertTrue(BlockDrops.yieldsDrop(BlockType.STONE, ItemRegistry.PICKAXE_WOOD));
        assertTrue(BlockDrops.yieldsDrop(BlockType.STONE, ItemRegistry.PICKAXE_STONE));
        assertTrue(BlockDrops.yieldsDrop(BlockType.STONE, ItemRegistry.PICKAXE_IRON));
        assertTrue(BlockDrops.yieldsDrop(BlockType.STONE, ItemRegistry.PICKAXE_DIAMOND));
    }

    @Test
    void stoneDropsNothingWithBareHands() {
        // STONE requires miningLevel >= 1; bare hands have no mining level.
        assertEquals(BlockDrops.NO_DROP, BlockDrops.dropItemId(BlockType.STONE, WorldConstants.BLOCK_AIR));
    }

    @Test
    void diamondDropsWithIronPickaxeOrBetter() {
        assertTrue(BlockDrops.yieldsDrop(BlockType.DIAMOND, ItemRegistry.PICKAXE_IRON));
        assertTrue(BlockDrops.yieldsDrop(BlockType.DIAMOND, ItemRegistry.PICKAXE_DIAMOND));
    }

    @Test
    void diamondDropsNothingWithStonePickaxe() {
        // DIAMOND requires miningLevel >= 3; stone pickaxe has level 2.
        assertEquals(BlockDrops.NO_DROP, BlockDrops.dropItemId(BlockType.DIAMOND, ItemRegistry.PICKAXE_STONE));
    }

    @Test
    void diamondDropsNothingWithWoodPickaxe() {
        assertEquals(BlockDrops.NO_DROP, BlockDrops.dropItemId(BlockType.DIAMOND, ItemRegistry.PICKAXE_WOOD));
    }

    @Test
    void diamondDropsNothingWithBareHands() {
        assertEquals(BlockDrops.NO_DROP, BlockDrops.dropItemId(BlockType.DIAMOND, WorldConstants.BLOCK_AIR));
    }

    @Test
    void diamondDropsNothingWithWrongToolFamily() {
        // A pickaxe kind is required for diamond; shovel is wrong family even if iron.
        assertEquals(BlockDrops.NO_DROP, BlockDrops.dropItemId(BlockType.DIAMOND, ItemRegistry.SHOVEL_IRON));
    }

    @Test
    void ironOreRequiresStonePlusPickaxe() {
        // IRON requires miningLevel >= 2; stone pickaxe has level 2.
        assertTrue(BlockDrops.yieldsDrop(BlockType.IRON, ItemRegistry.PICKAXE_STONE));
        assertTrue(BlockDrops.yieldsDrop(BlockType.IRON, ItemRegistry.PICKAXE_IRON));
        // Wood pickaxe has level 1 — not enough.
        assertEquals(BlockDrops.NO_DROP, BlockDrops.dropItemId(BlockType.IRON, ItemRegistry.PICKAXE_WOOD));
    }

    @Test
    void dirtDropsWithBareHandsOrShovel() {
        // DIRT has requiredMiningLevel == 0 → any means yields a drop.
        assertTrue(BlockDrops.yieldsDrop(BlockType.DIRT, WorldConstants.BLOCK_AIR));
        assertTrue(BlockDrops.yieldsDrop(BlockType.DIRT, ItemRegistry.SHOVEL_WOOD));
    }

    @Test
    void woodDropsWithBareHandsOrAxe() {
        assertTrue(BlockDrops.yieldsDrop(BlockType.WOOD, WorldConstants.BLOCK_AIR));
        assertTrue(BlockDrops.yieldsDrop(BlockType.WOOD, ItemRegistry.AXE_STONE));
    }

    @Test
    void airAndWaterNeverDrop() {
        assertEquals(BlockDrops.NO_DROP, BlockDrops.dropItemId(BlockType.AIR, WorldConstants.BLOCK_AIR));
        assertEquals(BlockDrops.NO_DROP, BlockDrops.dropItemId(BlockType.WATER, WorldConstants.BLOCK_AIR));
    }

    @Test
    void dropItemIdMatchesBlockOrdinal() {
        // When a drop is produced, the item id equals the block's ordinal (block id).
        int dropId = BlockDrops.dropItemId(BlockType.DIRT, WorldConstants.BLOCK_AIR);
        assertEquals(BlockType.DIRT.ordinal(), dropId);
    }
}
