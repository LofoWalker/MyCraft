package org.example.systems;

import org.example.components.Furnace;
import org.example.components.Inventory;
import org.example.components.ItemStack;
import org.example.world.WorldConstants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Pure unit tests for the furnace smelting tick logic (no World, no GL). */
class FurnaceSystemTest {

    private static Inventory furnaceInv(ItemStack input, ItemStack fuel, ItemStack output) {
        return new Inventory(new ItemStack[]{ input, fuel, output });
    }

    private static FurnaceSystem.FurnaceState run(Furnace furnace, Inventory inv, int ticks) {
        FurnaceSystem.FurnaceState state = new FurnaceSystem.FurnaceState(furnace, inv);
        for (int i = 0; i < ticks; i++) {
            state = FurnaceSystem.tick(state.furnace(), state.inventory());
        }
        return state;
    }

    @Test
    void oreWithFuelSmeltsToIngotAfterCookTime() {
        Inventory inv = furnaceInv(new ItemStack(WorldConstants.BLOCK_IRON, 1),
                                   new ItemStack(WorldConstants.BLOCK_WOOD, 1),
                                   ItemStack.EMPTY);

        FurnaceSystem.FurnaceState state = run(Furnace.empty(), inv, WorldConstants.FURNACE_COOK_TIME);

        ItemStack output = state.inventory().slots()[2];
        assertEquals(WorldConstants.ITEM_IRON_INGOT, output.itemId(), "output should be an iron ingot");
        assertEquals(1, output.count(), "exactly one ingot smelted");
        assertTrue(state.inventory().slots()[0].isEmpty(), "input ore consumed");
    }

    @Test
    void withoutFuelNothingSmelts() {
        Inventory inv = furnaceInv(new ItemStack(WorldConstants.BLOCK_IRON, 1),
                                   ItemStack.EMPTY, ItemStack.EMPTY);

        FurnaceSystem.FurnaceState state = run(Furnace.empty(), inv, WorldConstants.FURNACE_COOK_TIME * 2);

        assertTrue(state.inventory().slots()[2].isEmpty(), "no output without fuel");
        assertEquals(1, state.inventory().slots()[0].count(), "input not consumed without fuel");
        assertEquals(0, state.furnace().cookTicks(), "cook progress stays at zero");
    }

    @Test
    void lightingFuelConsumesOneFuelItem() {
        Inventory inv = furnaceInv(new ItemStack(WorldConstants.BLOCK_IRON, 1),
                                   new ItemStack(WorldConstants.BLOCK_WOOD, 2),
                                   ItemStack.EMPTY);

        FurnaceSystem.FurnaceState state = run(Furnace.empty(), inv, 1);

        assertTrue(state.furnace().isBurning(), "furnace lit after one tick");
        assertEquals(1, state.inventory().slots()[1].count(), "one fuel item consumed to light");
    }

    @Test
    void fullOutputBlocksCooking() {
        ItemStack fullOutput = new ItemStack(WorldConstants.ITEM_IRON_INGOT, WorldConstants.MAX_STACK);
        Inventory inv = furnaceInv(new ItemStack(WorldConstants.BLOCK_IRON, 1),
                                   new ItemStack(WorldConstants.BLOCK_WOOD, 4),
                                   fullOutput);

        FurnaceSystem.FurnaceState state = run(Furnace.empty(), inv, WorldConstants.FURNACE_COOK_TIME * 2);

        assertEquals(WorldConstants.MAX_STACK, state.inventory().slots()[2].count(), "output unchanged when full");
        assertEquals(1, state.inventory().slots()[0].count(), "input not consumed when output is full");
        assertEquals(0, state.furnace().cookTicks(), "never cooks when output is full");
    }
}
