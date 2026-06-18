package org.example.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ItemRegistryTest {

    @Test
    void blockItemsHaveMaxStack64AndAreNotTools() {
        for (int id = 1; id <= WorldConstants.MAX_BLOCK_ID; id++) {
            ItemRegistry.ItemData data = ItemRegistry.byId(id);
            assertEquals(WorldConstants.MAX_STACK, data.maxStack(),
                    "Block id " + id + " should have maxStack 64");
            assertFalse(data.isTool(), "Block id " + id + " should not be a tool");
            assertTrue(data.isBlock(), "Block id " + id + " should be a block");
        }
    }

    @Test
    void airItemIsNotABlock() {
        ItemRegistry.ItemData air = ItemRegistry.byId(WorldConstants.BLOCK_AIR);
        assertFalse(air.isBlock());
        assertFalse(air.isTool());
    }

    @Test
    void woodPickaxeHasCorrectProperties() {
        ItemRegistry.ItemData data = ItemRegistry.byId(ItemRegistry.PICKAXE_WOOD);
        assertTrue(data.isTool());
        assertFalse(data.isBlock());
        assertEquals(1, data.maxStack());
        assertEquals(ToolKind.PICKAXE, data.toolKind());
        assertEquals(ToolMaterial.WOOD, data.toolMaterial());
        assertEquals(59, data.durability());
    }

    @Test
    void ironPickaxeHasCorrectMaterialAndDurability() {
        ItemRegistry.ItemData data = ItemRegistry.byId(ItemRegistry.PICKAXE_IRON);
        assertEquals(ToolMaterial.IRON, data.toolMaterial());
        assertEquals(250, data.durability());
        assertEquals(1, data.maxStack());
    }

    @Test
    void diamondPickaxeHasHighestDurabilityAmongPickaxes() {
        int wood    = ItemRegistry.byId(ItemRegistry.PICKAXE_WOOD).durability();
        int stone   = ItemRegistry.byId(ItemRegistry.PICKAXE_STONE).durability();
        int iron    = ItemRegistry.byId(ItemRegistry.PICKAXE_IRON).durability();
        int diamond = ItemRegistry.byId(ItemRegistry.PICKAXE_DIAMOND).durability();
        assertTrue(diamond > iron);
        assertTrue(iron > stone);
        assertTrue(stone > wood);
    }

    @Test
    void toolsStackToOne() {
        assertEquals(1, ItemRegistry.maxStack(ItemRegistry.PICKAXE_IRON));
        assertEquals(1, ItemRegistry.maxStack(ItemRegistry.AXE_DIAMOND));
        assertEquals(1, ItemRegistry.maxStack(ItemRegistry.SHOVEL_STONE));
        assertEquals(1, ItemRegistry.maxStack(ItemRegistry.SWORD_WOOD));
    }

    @Test
    void foodsAreNeitherBlocksNorTools() {
        ItemRegistry.ItemData apple = ItemRegistry.byId(WorldConstants.ITEM_APPLE);
        assertFalse(apple.isBlock());
        assertFalse(apple.isTool());
        assertEquals(WorldConstants.MAX_STACK, apple.maxStack());
    }

    @Test
    void axeIronHasCorrectKind() {
        assertEquals(ToolKind.AXE, ItemRegistry.toolKind(ItemRegistry.AXE_IRON));
    }

    @Test
    void shovelDiamondHasCorrectMaterial() {
        assertEquals(ToolMaterial.DIAMOND, ItemRegistry.toolMaterial(ItemRegistry.SHOVEL_DIAMOND));
    }

    @Test
    void outOfRangeIdReturnsDefaultEntry() {
        ItemRegistry.ItemData data = ItemRegistry.byId(9999);
        assertFalse(data.isBlock());
        assertFalse(data.isTool());
    }

    @Test
    void newToolHasFullDurability() {
        org.example.components.ItemStack pickaxe = Inventories.newTool(ItemRegistry.PICKAXE_IRON);
        assertEquals(ItemRegistry.PICKAXE_IRON, pickaxe.itemId());
        assertEquals(1, pickaxe.count());
        assertEquals(250, pickaxe.durability());
    }
}
