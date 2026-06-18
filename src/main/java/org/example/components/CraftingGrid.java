package org.example.components;

import java.util.Arrays;

// Current contents of the player's active crafting grid (2×2 or 3×3), plus the derived craft result.
// size is WorldConstants.CRAFTING_GRID_SMALL (2) or WorldConstants.CRAFTING_GRID_LARGE (3);
// slots.length == size * size. craftResult is ItemStack.EMPTY when no recipe matches.
// Attached to the player entity while the inventory screen is open; removed on close (after returning
// any remaining ingredients to the inventory). All mutation is done by InventoryScreenSystem, which
// replaces the component functionally (immutable-style) to stay ECS-clean.
public record CraftingGrid(ItemStack[] slots, int size, ItemStack craftResult) {

    public static CraftingGrid empty(int size) {
        ItemStack[] slots = new ItemStack[size * size];
        Arrays.fill(slots, ItemStack.EMPTY);
        return new CraftingGrid(slots, size, ItemStack.EMPTY);
    }
}
