package org.example.components;

// Marks the player entity when the inventory screen is open. Carries the current mouse-cursor
// position in screen pixels and the optional "held" item stack (grabbed from a slot and following
// the cursor until the player clicks again to place it). heldStack == ItemStack.EMPTY means no
// item is being carried. craftingTableOpen == true when the screen was opened by right-clicking a
// crafting-table block (gives the 3×3 grid); false = personal 2×2 grid.
public record InventoryScreen(
        double cursorX,
        double cursorY,
        ItemStack heldStack,
        boolean craftingTableOpen
) {
    public static InventoryScreen open(boolean craftingTableOpen) {
        return new InventoryScreen(0, 0, ItemStack.EMPTY, craftingTableOpen);
    }
}
