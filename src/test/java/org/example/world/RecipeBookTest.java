package org.example.world;

import org.example.components.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

// Pure unit tests for RecipeBook — no GL context required.
class RecipeBookTest {

    private static final int WOOD  = WorldConstants.BLOCK_WOOD;
    private static final int STONE = WorldConstants.BLOCK_STONE;
    private static final int TABLE = WorldConstants.BLOCK_CRAFTING_TABLE;
    private static final int STICK = WorldConstants.ITEM_STICK;
    private static final int TORCH = WorldConstants.BLOCK_TORCH;

    // ---- Shaped: offset independence ----

    @Test
    void craftingTableRecognisedTopLeft() {
        // 2×2 of WOOD placed in top-left of a 3×3 grid.
        int[] grid = {
            WOOD, WOOD,    0,
            WOOD, WOOD,    0,
               0,    0,    0
        };
        Optional<ItemStack> result = RecipeBook.get().match(grid, 3, 3);
        assertTrue(result.isPresent(), "2×2 wood at top-left should match crafting table recipe");
        assertEquals(TABLE, result.get().itemId());
        assertEquals(1, result.get().count());
    }

    @Test
    void craftingTableRecognisedBottomRight() {
        // Same 2×2 pattern but placed in the bottom-right corner.
        int[] grid = {
               0,    0,    0,
               0, WOOD, WOOD,
               0, WOOD, WOOD
        };
        Optional<ItemStack> result = RecipeBook.get().match(grid, 3, 3);
        assertTrue(result.isPresent(), "2×2 wood at bottom-right should match crafting table recipe");
        assertEquals(TABLE, result.get().itemId());
    }

    @Test
    void craftingTableRecognisedCentre() {
        // 2×2 pattern of WOOD in the exact centre of a 3×3 is not possible (offsets would need
        // to be (0 or 1) — centre means offset (0,0) top-left which is covered by first test).
        // Instead verify offset (1,1): column 1..2, row 1..2.
        // Row-major 3×3: indices [3]=row1col0 ... [8]=row2col2
        int[] grid = {
               0,    0,    0,
               0, WOOD, WOOD,
               0, WOOD, WOOD
        };
        assertTrue(RecipeBook.get().match(grid, 3, 3).isPresent());
    }

    @Test
    void sticksRecognisedAtOffsetOneRow() {
        // 1×2 stick recipe (WOOD over WOOD) placed in column 1, rows 1-2 of a 3×3 grid.
        int[] grid = {
               0,    0, 0,
               0, WOOD, 0,
               0, WOOD, 0
        };
        Optional<ItemStack> result = RecipeBook.get().match(grid, 3, 3);
        assertTrue(result.isPresent(), "Stick recipe at offset should match");
        assertEquals(STICK, result.get().itemId());
        assertEquals(4, result.get().count());
    }

    @Test
    void shapedRecipeRejectedWhenPatternWrong() {
        // A 2×2 grid with only 3 wood should NOT match the crafting-table recipe.
        int[] grid2 = {
            WOOD, WOOD,
            WOOD,    0
        };
        Optional<ItemStack> result = RecipeBook.get().match(grid2, 2, 2);
        // Might match something else, but must NOT be the crafting table recipe.
        result.ifPresent(r -> assertNotEquals(TABLE, r.itemId(),
                "Incomplete 2×2 must not produce a crafting table"));
    }

    @Test
    void shapedRecipeRejectedWhenExtraItemOutsidePattern() {
        // Pattern expects exactly 2×2 WOOD; placing a STONE outside it must break the match.
        int[] grid = {
            WOOD, WOOD, STONE,
            WOOD, WOOD,     0,
               0,    0,     0
        };
        Optional<ItemStack> result = RecipeBook.get().match(grid, 3, 3);
        // No recipe should match because there is an extra STONE outside the 2×2 footprint.
        result.ifPresent(r -> assertNotEquals(TABLE, r.itemId()));
    }

    // ---- Shapeless: order independence ----

    @Test
    void shapelessWoodToPlanksAnyPosition() {
        // 1 WOOD in top-left cell of a 2×2 grid.
        int[] grid = { WOOD, 0, 0, 0 };
        Optional<ItemStack> result = RecipeBook.get().match(grid, 2, 2);
        assertTrue(result.isPresent(), "1 wood in any cell should match planks recipe");
        assertEquals(WOOD, result.get().itemId());
        assertEquals(4, result.get().count());
    }

    @Test
    void shapelessWoodToPlanksBottomRight() {
        int[] grid = { 0, 0, 0, WOOD };
        Optional<ItemStack> result = RecipeBook.get().match(grid, 2, 2);
        assertTrue(result.isPresent());
        assertEquals(WOOD, result.get().itemId());
        assertEquals(4, result.get().count());
    }

    @Test
    void shapelessRecipeRequiresExactIngredientCount() {
        // Two WOOD cells should NOT match the single-wood shapeless recipe.
        int[] grid = { WOOD, WOOD, 0, 0 };
        Optional<ItemStack> result = RecipeBook.get().match(grid, 2, 2);
        // If any result is present it cannot be the planks result (count 4 from 1 wood).
        result.ifPresent(r -> {
            if (r.itemId() == WOOD && r.count() == 4) {
                fail("Two-wood grid must not match single-wood recipe");
            }
        });
    }

    // ---- Ingredient consumption via InventoryScreenSystem (logic tested separately) ----

    @Test
    void emptyGridMatchesNothing() {
        int[] grid = { 0, 0, 0, 0 };
        assertTrue(RecipeBook.get().match(grid, 2, 2).isEmpty());
    }

    @Test
    void torchRecipeMatchesOnSticksAndStone() {
        // Torch: STONE on top, STICK below (1×2 — fits in 2×2 with offset freedom).
        int[] grid = {
            STONE, 0,
            STICK, 0
        };
        Optional<ItemStack> result = RecipeBook.get().match(grid, 2, 2);
        assertTrue(result.isPresent(), "Torch recipe should match STONE over STICK");
        assertEquals(TORCH, result.get().itemId());
        assertEquals(4, result.get().count());
    }

    @Test
    void pickaxeRecipeRequires3x3Grid() {
        // Wood pickaxe is a 3-wide recipe; it cannot fit in a 2×2 grid.
        int[] grid2 = {
            WOOD, WOOD,
               0, STICK
        };
        // Should not produce a pickaxe (too wide).
        Optional<ItemStack> result = RecipeBook.get().match(grid2, 2, 2);
        result.ifPresent(r -> assertNotEquals(ItemRegistry.PICKAXE_WOOD, r.itemId()));
    }

    @Test
    void pickaxeRecipeMatchesIn3x3() {
        int[] grid3 = {
            WOOD, WOOD, WOOD,
               0, STICK,   0,
               0, STICK,   0
        };
        Optional<ItemStack> result = RecipeBook.get().match(grid3, 3, 3);
        assertTrue(result.isPresent(), "Wood pickaxe recipe should match");
        assertEquals(ItemRegistry.PICKAXE_WOOD, result.get().itemId());
    }

    // ---- Custom recipe book (isolated, no global state) ----

    @Test
    void customShapedRecipeFoundAtAnyOffset() {
        int IRON = WorldConstants.BLOCK_IRON;
        RecipeBook.ShapedRecipe recipe = new RecipeBook.ShapedRecipe(
                1, 1, new int[]{ IRON }, new ItemStack(IRON, 9));
        RecipeBook book = new RecipeBook(
                new RecipeBook.ShapedRecipe[]{ recipe },
                new RecipeBook.ShapelessRecipe[0]);

        // Single IRON in each of the four cells of a 2×2 grid.
        assertTrue(book.match(new int[]{ IRON,    0,    0,    0 }, 2, 2).isPresent());
        assertTrue(book.match(new int[]{    0, IRON,    0,    0 }, 2, 2).isPresent());
        assertTrue(book.match(new int[]{    0,    0, IRON,    0 }, 2, 2).isPresent());
        assertTrue(book.match(new int[]{    0,    0,    0, IRON }, 2, 2).isPresent());
    }

    @Test
    void customShapelessRecipeOrderIndependent() {
        int A = WorldConstants.BLOCK_STONE;
        int B = WorldConstants.BLOCK_DIRT;
        RecipeBook.ShapelessRecipe recipe = new RecipeBook.ShapelessRecipe(
                new int[]{ A, B }, new ItemStack(TORCH, 2));
        RecipeBook book = new RecipeBook(
                new RecipeBook.ShapedRecipe[0],
                new RecipeBook.ShapelessRecipe[]{ recipe });

        // Both orderings should match.
        assertTrue(book.match(new int[]{ A, B, 0, 0 }, 2, 2).isPresent());
        assertTrue(book.match(new int[]{ B, A, 0, 0 }, 2, 2).isPresent());
        assertTrue(book.match(new int[]{ 0, A, B, 0 }, 2, 2).isPresent());
        assertTrue(book.match(new int[]{ B, 0, A, 0 }, 2, 2).isPresent());
    }
}
