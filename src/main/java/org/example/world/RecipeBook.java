package org.example.world;

import org.example.components.ItemStack;

import java.util.Optional;

// Pure, stateless recipe registry. Holds all shaped and shapeless recipes and exposes a single
// match() entry point that evaluates a crafting grid. No GL, no World, no mutable state.
// Lookup is linear over the recipe list (recipe counts are tiny) — no HashMap on any path.
public final class RecipeBook {

    // A shaped recipe has a fixed spatial pattern (width × height) of ingredient item-ids plus
    // zeros for empty cells. The pattern is offset-tested against all valid top-left positions in
    // the grid so the same recipe works wherever the player places the ingredients.
    public record ShapedRecipe(int width, int height, int[] pattern, ItemStack result) {}

    // A shapeless recipe is satisfied by any arrangement of the listed ingredient ids (one per
    // slot, count == 1 each). Ingredient order is irrelevant.
    public record ShapelessRecipe(int[] ingredients, ItemStack result) {}

    // --- Built-in recipe book (singleton) ---

    private static final RecipeBook INSTANCE = new RecipeBook();

    public static RecipeBook get() { return INSTANCE; }

    private final ShapedRecipe[]    shapedRecipes;
    private final ShapelessRecipe[] shapelessRecipes;

    // Clients that need a custom recipe set (unit tests) call this constructor directly.
    public RecipeBook(ShapedRecipe[] shapedRecipes, ShapelessRecipe[] shapelessRecipes) {
        this.shapedRecipes    = shapedRecipes.clone();
        this.shapelessRecipes = shapelessRecipes.clone();
    }

    // Builds the global recipe table.
    private RecipeBook() {
        this(buildShaped(), buildShapeless());
    }

    // --- Public API ---

    // Evaluates a crafting grid (flat row-major array, gridW × gridH cells) against all recipes.
    // Returns the first matching result, or Optional.empty() when no recipe matches.
    // grid[row * gridW + col] holds the item-id of that cell (0 == empty).
    public Optional<ItemStack> match(int[] grid, int gridW, int gridH) {
        Optional<ItemStack> shaped = matchShaped(grid, gridW, gridH);
        if (shaped.isPresent()) return shaped;
        return matchShapeless(grid);
    }

    // --- Shaped matching ---

    private Optional<ItemStack> matchShaped(int[] grid, int gridW, int gridH) {
        for (ShapedRecipe recipe : shapedRecipes) {
            if (recipe.width() > gridW || recipe.height() > gridH) continue;
            Optional<ItemStack> result = tryAllOffsets(grid, gridW, gridH, recipe);
            if (result.isPresent()) return result;
        }
        return Optional.empty();
    }

    // Tries every valid top-left offset for the pattern in the grid.
    private static Optional<ItemStack> tryAllOffsets(
            int[] grid, int gridW, int gridH, ShapedRecipe recipe) {
        int maxOffsetX = gridW - recipe.width();
        int maxOffsetY = gridH - recipe.height();
        for (int offsetY = 0; offsetY <= maxOffsetY; offsetY++) {
            for (int offsetX = 0; offsetX <= maxOffsetX; offsetX++) {
                if (patternMatchesAt(grid, gridW, gridH, recipe, offsetX, offsetY)) {
                    return Optional.of(recipe.result());
                }
            }
        }
        return Optional.empty();
    }

    // Returns true if the recipe pattern exactly matches the grid at the given offset, and all cells
    // outside the pattern footprint are empty (so a 1×2 recipe does not match a 2×2 grid with extras).
    private static boolean patternMatchesAt(
            int[] grid, int gridW, int gridH,
            ShapedRecipe recipe, int offsetX, int offsetY) {
        // Verify pattern cells match.
        for (int row = 0; row < recipe.height(); row++) {
            for (int col = 0; col < recipe.width(); col++) {
                int expected = recipe.pattern()[row * recipe.width() + col];
                int actual   = grid[(row + offsetY) * gridW + (col + offsetX)];
                if (expected != actual) return false;
            }
        }
        // Verify all grid cells outside the pattern footprint are empty.
        for (int row = 0; row < gridH; row++) {
            for (int col = 0; col < gridW; col++) {
                boolean insidePattern =
                        col >= offsetX && col < offsetX + recipe.width()
                        && row >= offsetY && row < offsetY + recipe.height();
                if (!insidePattern && grid[row * gridW + col] != 0) return false;
            }
        }
        return true;
    }

    // --- Shapeless matching ---

    private Optional<ItemStack> matchShapeless(int[] grid) {
        for (ShapelessRecipe recipe : shapelessRecipes) {
            if (shapelessMatches(grid, recipe.ingredients())) return Optional.of(recipe.result());
        }
        return Optional.empty();
    }

    // Returns true when grid contains exactly the ingredients (each with count >= 1) in any order,
    // with no extra non-empty cells beyond the ingredient count.
    private static boolean shapelessMatches(int[] grid, int[] ingredients) {
        // Count non-empty cells.
        int nonEmpty = 0;
        for (int cell : grid) { if (cell != 0) nonEmpty++; }
        if (nonEmpty != ingredients.length) return false;

        // Try to match each ingredient to a distinct non-empty cell.
        boolean[] used = new boolean[grid.length];
        for (int ingredient : ingredients) {
            boolean found = false;
            for (int i = 0; i < grid.length; i++) {
                if (!used[i] && grid[i] == ingredient) {
                    used[i] = true;
                    found   = true;
                    break;
                }
            }
            if (!found) return false;
        }
        return true;
    }

    // --- Recipe definitions ---

    private static ShapedRecipe[] buildShaped() {
        // "Planks" do not have a separate block id in this engine; BLOCK_WOOD (the log) doubles as
        // the plank ingredient. The shapeless recipe wood→4 wood models the log-to-planks step.
        int WOOD  = WorldConstants.BLOCK_WOOD;
        int STONE = WorldConstants.BLOCK_STONE;
        int STICK = WorldConstants.ITEM_STICK;
        int TABLE = WorldConstants.BLOCK_CRAFTING_TABLE;

        return new ShapedRecipe[] {
            // Crafting table: 2×2 of wood logs → 1 crafting table
            new ShapedRecipe(2, 2,
                new int[]{ WOOD, WOOD,
                           WOOD, WOOD },
                new ItemStack(TABLE, 1)),

            // Sticks: 2×1 column of wood → 4 sticks
            new ShapedRecipe(1, 2,
                new int[]{ WOOD,
                           WOOD },
                new ItemStack(STICK, 4)),

            // Wood pickaxe: 3 wood on top, 2 sticks below (3×3 grid needed — available only at table)
            new ShapedRecipe(3, 3,
                new int[]{ WOOD,  WOOD,  WOOD,
                             0, STICK,    0,
                             0, STICK,    0 },
                new ItemStack(ItemRegistry.PICKAXE_WOOD, 1)),

            // Stone pickaxe
            new ShapedRecipe(3, 3,
                new int[]{ STONE, STONE, STONE,
                               0, STICK,     0,
                               0, STICK,     0 },
                new ItemStack(ItemRegistry.PICKAXE_STONE, 1)),

            // Wood axe (2×3 pattern, right-aligned; placed in 3×3 grid)
            new ShapedRecipe(2, 3,
                new int[]{ WOOD,  WOOD,
                           WOOD, STICK,
                              0, STICK },
                new ItemStack(ItemRegistry.AXE_WOOD, 1)),

            // Stone axe
            new ShapedRecipe(2, 3,
                new int[]{ STONE, STONE,
                           STONE, STICK,
                               0, STICK },
                new ItemStack(ItemRegistry.AXE_STONE, 1)),

            // Wood shovel: 1 wood + 2 sticks below (1×3)
            new ShapedRecipe(1, 3,
                new int[]{ WOOD,
                           STICK,
                           STICK },
                new ItemStack(ItemRegistry.SHOVEL_WOOD, 1)),

            // Stone shovel
            new ShapedRecipe(1, 3,
                new int[]{ STONE,
                           STICK,
                           STICK },
                new ItemStack(ItemRegistry.SHOVEL_STONE, 1)),

            // Wood sword: 2 wood + 1 stick below (1×3)
            new ShapedRecipe(1, 3,
                new int[]{ WOOD,
                           WOOD,
                           STICK },
                new ItemStack(ItemRegistry.SWORD_WOOD, 1)),

            // Stone sword
            new ShapedRecipe(1, 3,
                new int[]{ STONE,
                           STONE,
                           STICK },
                new ItemStack(ItemRegistry.SWORD_STONE, 1)),

            // Torch: 1 stone on top + 1 stick below (1×2; available in 2×2)
            new ShapedRecipe(1, 2,
                new int[]{ STONE,
                           STICK },
                new ItemStack(WorldConstants.BLOCK_TORCH, 4)),
        };
    }

    private static ShapelessRecipe[] buildShapeless() {
        return new ShapelessRecipe[] {
            // Planks (shapeless: 1 wood log → 4 planks). Since there is no separate plank block in
            // this engine, a wood log produces 4 more wood logs (symbolic — keeps the chain coherent).
            // In practice the test chain uses BLOCK_WOOD as "planks". If a future step adds a real
            // plank block this entry can be updated without touching RecipeBook's structure.
            new ShapelessRecipe(
                new int[]{ WorldConstants.BLOCK_WOOD },
                new ItemStack(WorldConstants.BLOCK_WOOD, 4)),
        };
    }
}
