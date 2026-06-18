package org.example.world;

// Pure, stateless smelting and fuel tables. All methods take item ids and return values with no
// side effects. No HashMap: item ids are sparse but bounded, so an int[] look-up table is used.
public final class SmeltingRecipes {

    private SmeltingRecipes() {}

    // Returns NO_RESULT when the input item cannot be smelted.
    public static final int NO_RESULT = 0;

    // Returns the item id produced by smelting the given input item, or NO_RESULT if not smeltable.
    public static int smeltingResult(int inputItemId) {
        return inputItemId >= 0 && inputItemId < SMELTING_TABLE.length
                ? SMELTING_TABLE[inputItemId]
                : NO_RESULT;
    }

    // Returns the number of burn ticks the given fuel item provides, or 0 if it is not a fuel.
    public static int fuelBurnTicks(int fuelItemId) {
        return fuelItemId >= 0 && fuelItemId < FUEL_TABLE.length
                ? FUEL_TABLE[fuelItemId]
                : 0;
    }

    // --- Tables ---

    private static final int TABLE_SIZE = 512;
    private static final int[] SMELTING_TABLE = new int[TABLE_SIZE];
    private static final int[] FUEL_TABLE     = new int[TABLE_SIZE];

    static {
        // Ores → ingots.
        SMELTING_TABLE[WorldConstants.BLOCK_IRON]    = WorldConstants.ITEM_IRON_INGOT;
        // Sand → glass (reuse STONE as placeholder until a glass block is added).
        SMELTING_TABLE[WorldConstants.BLOCK_SAND]    = WorldConstants.BLOCK_STONE;

        // Fuels: coal-equivalent (STONE standing in as coal until a coal item is added),
        // and wood (logs). Coal is not yet a distinct item; map BLOCK_STONE as a placeholder
        // so tests can use it. The canonical fuels are BLOCK_WOOD and — once added — ITEM_COAL.
        FUEL_TABLE[WorldConstants.BLOCK_WOOD]  = WorldConstants.FURNACE_FUEL_TICKS_WOOD;
        // BLOCK_STONE standing in as coal-equivalent for now.
        FUEL_TABLE[WorldConstants.BLOCK_STONE] = WorldConstants.FURNACE_FUEL_TICKS_COAL;
    }
}
