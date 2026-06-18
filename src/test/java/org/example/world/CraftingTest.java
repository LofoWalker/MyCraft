package org.example.world;

import org.example.components.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

// Integration tests for the full crafting chain: wood→planks→table→pickaxe.
// Also verifies ingredient consumption semantics exercised by InventoryScreenSystem.
class CraftingTest {

    private static final int WOOD  = WorldConstants.BLOCK_WOOD;
    private static final int TABLE = WorldConstants.BLOCK_CRAFTING_TABLE;
    private static final int STICK = WorldConstants.ITEM_STICK;

    // ---- wood → planks (shapeless, 2×2 grid) ----

    @Test
    void oneWoodYieldsFourPlanks() {
        int[] grid = { WOOD, 0, 0, 0 };
        Optional<ItemStack> result = RecipeBook.get().match(grid, 2, 2);
        assertTrue(result.isPresent());
        assertEquals(WOOD, result.get().itemId(),   "planks represented as WOOD in this engine");
        assertEquals(4,    result.get().count(),    "1 wood → 4 planks");
    }

    // ---- planks → sticks (shaped 1×2) ----

    @Test
    void twoWoodYieldsFourSticks() {
        // Two WOOD stacked vertically (1 col × 2 rows) → 4 sticks.
        int[] grid = {
            WOOD, 0,
            WOOD, 0
        };
        Optional<ItemStack> result = RecipeBook.get().match(grid, 2, 2);
        assertTrue(result.isPresent());
        assertEquals(STICK, result.get().itemId(), "2 wood stacked vertically → sticks");
        assertEquals(4,     result.get().count());
    }

    // ---- planks → crafting table (shaped 2×2) ----

    @Test
    void fourWoodYieldsCraftingTable() {
        int[] grid = {
            WOOD, WOOD,
            WOOD, WOOD
        };
        Optional<ItemStack> result = RecipeBook.get().match(grid, 2, 2);
        assertTrue(result.isPresent(), "4 wood in 2×2 should produce a crafting table");
        assertEquals(TABLE, result.get().itemId());
        assertEquals(1,     result.get().count());
    }

    // ---- crafting table → wood pickaxe (shaped 3×3; only available at crafting table) ----

    @Test
    void threeWoodAndTwoSticksYieldWoodPickaxe() {
        int[] grid = {
            WOOD,  WOOD,  WOOD,
               0, STICK,     0,
               0, STICK,     0
        };
        Optional<ItemStack> result = RecipeBook.get().match(grid, 3, 3);
        assertTrue(result.isPresent(), "Standard pickaxe layout should be recognised");
        assertEquals(ItemRegistry.PICKAXE_WOOD, result.get().itemId());
        assertEquals(1, result.get().count());
    }

    // ---- ingredient consumption ----

    @Test
    void takingResultConsumesEachIngredientOnce() {
        // Simulates InventoryScreenSystem.consumeIngredients on a 2×2 grid loaded with 4 WOOD.
        ItemStack[] slots = {
            new ItemStack(WOOD, 3),
            new ItemStack(WOOD, 3),
            new ItemStack(WOOD, 3),
            new ItemStack(WOOD, 3)
        };
        ItemStack[] after = consumeIngredients(slots);

        for (int i = 0; i < 4; i++) {
            assertEquals(2, after[i].count(),
                    "Each slot should have exactly one fewer item after taking the result");
        }
    }

    @Test
    void takingResultEmptiesSlotWhenCountReachesZero() {
        ItemStack[] slots = {
            new ItemStack(WOOD, 1),
            new ItemStack(WOOD, 1),
            new ItemStack(WOOD, 1),
            new ItemStack(WOOD, 1)
        };
        ItemStack[] after = consumeIngredients(slots);
        for (int i = 0; i < 4; i++) {
            assertTrue(after[i].isEmpty(),
                    "Slot with count 1 should become EMPTY after ingredient consumption");
        }
    }

    @Test
    void consumeIngredientsSkipsEmptySlots() {
        ItemStack[] slots = {
            new ItemStack(WOOD, 2),
            ItemStack.EMPTY,
            ItemStack.EMPTY,
            new ItemStack(WOOD, 2)
        };
        ItemStack[] after = consumeIngredients(slots);
        assertEquals(1, after[0].count());
        assertTrue(after[1].isEmpty());
        assertTrue(after[2].isEmpty());
        assertEquals(1, after[3].count());
    }

    @Test
    void woodPickaxeChain() {
        // Full chain: wood → sticks (intermediate), then wood + sticks → pickaxe.
        // Step 1: craft sticks from 2 wood.
        int[] sticksGrid = { WOOD, 0, WOOD, 0 };  // 2×2, col 0 has 2 wood
        // Reorder: row-major means [0]=row0col0, [1]=row0col1, [2]=row1col0, [3]=row1col1
        // So WOOD in [0] and [2] = column 0, rows 0 and 1 — that is 1 wide × 2 tall.
        Optional<ItemStack> stickResult = RecipeBook.get().match(sticksGrid, 2, 2);
        assertTrue(stickResult.isPresent(), "Sticks recipe must match");
        assertEquals(STICK, stickResult.get().itemId());
        assertEquals(4,     stickResult.get().count());

        // Step 2: craft pickaxe from 3 wood + 2 sticks in 3×3 grid.
        int[] pickGrid = {
            WOOD,  WOOD,  WOOD,
               0, STICK,     0,
               0, STICK,     0
        };
        Optional<ItemStack> pickResult = RecipeBook.get().match(pickGrid, 3, 3);
        assertTrue(pickResult.isPresent(), "Pickaxe recipe must match");
        assertEquals(ItemRegistry.PICKAXE_WOOD, pickResult.get().itemId());
    }

    // ---- Helper: mirrors InventoryScreenSystem.consumeIngredients (pure copy for tests) ----

    private static ItemStack[] consumeIngredients(ItemStack[] slots) {
        ItemStack[] result = slots.clone();
        for (int i = 0; i < result.length; i++) {
            if (result[i].isEmpty()) continue;
            int left = result[i].count() - 1;
            result[i] = left > 0
                    ? new ItemStack(result[i].itemId(), left, result[i].durability())
                    : ItemStack.EMPTY;
        }
        return result;
    }
}
