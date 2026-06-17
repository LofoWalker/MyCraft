package org.example.world;

import org.example.components.Inventory;
import org.example.components.ItemStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InventoriesTest {

    private static final int STONE = WorldConstants.BLOCK_STONE;
    private static final int DIRT  = WorldConstants.BLOCK_DIRT;

    @Test
    void emptyInventoryHasAllSlotsEmptyAndNoNulls() {
        Inventory inv = Inventories.empty();
        assertEquals(WorldConstants.INVENTORY_SLOTS, inv.size());
        for (int i = 0; i < inv.size(); i++) {
            assertSame(ItemStack.EMPTY, Inventories.get(inv, i));
        }
    }

    @Test
    void addFillsFirstEmptySlot() {
        Inventory inv = Inventories.add(Inventories.empty(), new ItemStack(STONE, 10)).inventory();
        assertEquals(new ItemStack(STONE, 10), Inventories.get(inv, 0));
        assertSame(ItemStack.EMPTY, Inventories.get(inv, 1));
    }

    @Test
    void addStacksOntoExistingSameItem() {
        Inventory inv = Inventories.add(Inventories.empty(), new ItemStack(STONE, 10)).inventory();
        inv = Inventories.add(inv, new ItemStack(STONE, 5)).inventory();
        assertEquals(new ItemStack(STONE, 15), Inventories.get(inv, 0));
        assertSame(ItemStack.EMPTY, Inventories.get(inv, 1));
    }

    @Test
    void addCapsAtMaxStackThenOverflowsToNextSlot() {
        Inventory inv = Inventories.add(Inventories.empty(),
                new ItemStack(STONE, WorldConstants.MAX_STACK + 20)).inventory();
        assertEquals(new ItemStack(STONE, WorldConstants.MAX_STACK), Inventories.get(inv, 0));
        assertEquals(new ItemStack(STONE, 20), Inventories.get(inv, 1));
    }

    @Test
    void addReportsNoRemainderWhenItFits() {
        Inventories.AddResult result = Inventories.add(Inventories.empty(), new ItemStack(STONE, 5));
        assertSame(ItemStack.EMPTY, result.remainder());
    }

    @Test
    void addToFullInventoryReturnsRemainder() {
        Inventory full = fullOf(STONE);
        Inventories.AddResult result = Inventories.add(full, new ItemStack(DIRT, 7));
        assertEquals(new ItemStack(DIRT, 7), result.remainder());
        assertTrue(sameContents(full, result.inventory()), "full inventory should be unchanged");
    }

    @Test
    void addDoesNotMutateOriginalInventory() {
        Inventory original = Inventories.empty();
        Inventories.add(original, new ItemStack(STONE, 1));
        assertSame(ItemStack.EMPTY, Inventories.get(original, 0));
    }

    @Test
    void removeOneDecrementsStack() {
        Inventory inv = Inventories.add(Inventories.empty(), new ItemStack(STONE, 3)).inventory();
        inv = Inventories.removeOne(inv, 0);
        assertEquals(new ItemStack(STONE, 2), Inventories.get(inv, 0));
    }

    @Test
    void removeOneEmptiesSlotAtLastItem() {
        Inventory inv = Inventories.add(Inventories.empty(), new ItemStack(STONE, 1)).inventory();
        inv = Inventories.removeOne(inv, 0);
        assertSame(ItemStack.EMPTY, Inventories.get(inv, 0));
    }

    @Test
    void removeOneOnEmptySlotIsNoOp() {
        Inventory inv = Inventories.empty();
        assertSame(inv, Inventories.removeOne(inv, 0));
    }

    @Test
    void addEmptyStackIsNoOp() {
        Inventory inv = Inventories.empty();
        Inventories.AddResult result = Inventories.add(inv, ItemStack.EMPTY);
        assertSame(inv, result.inventory());
        assertSame(ItemStack.EMPTY, result.remainder());
    }

    private static Inventory fullOf(int itemId) {
        Inventory inv = Inventories.empty();
        return Inventories.add(inv,
                new ItemStack(itemId, WorldConstants.INVENTORY_SLOTS * WorldConstants.MAX_STACK)).inventory();
    }

    private static boolean sameContents(Inventory a, Inventory b) {
        for (int i = 0; i < a.size(); i++) {
            if (!Inventories.get(a, i).equals(Inventories.get(b, i))) return false;
        }
        return true;
    }
}
