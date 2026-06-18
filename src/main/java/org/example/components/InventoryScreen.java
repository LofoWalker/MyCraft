package org.example.components;

// Marks the player entity when the inventory screen is open. Carries the current mouse-cursor
// position in screen pixels and the optional "held" item stack (grabbed from a slot and following
// the cursor until the player clicks again to place it). heldStack == ItemStack.EMPTY means no
// item is being carried.
// Mode flags:
//   craftingTableOpen == true  → 3×3 crafting grid (crafting table)
//   furnaceEntityId   >= 0     → furnace UI for the given block-entity id
//   chestEntityId     >= 0     → chest UI for the given block-entity id
// Only one mode is active at a time; the rest use their "inactive" sentinel (-1 / false).
public record InventoryScreen(
        double cursorX,
        double cursorY,
        ItemStack heldStack,
        boolean craftingTableOpen,
        int furnaceEntityId,
        int chestEntityId
) {
    /** Sentinel: no furnace or chest block-entity is open. */
    public static final int NO_ENTITY = -1;

    /** Opens the plain inventory (or 2×2 crafting grid). */
    public static InventoryScreen open(boolean craftingTableOpen) {
        return new InventoryScreen(0, 0, ItemStack.EMPTY, craftingTableOpen, NO_ENTITY, NO_ENTITY);
    }

    /** Opens a furnace UI backed by the given block-entity id. */
    public static InventoryScreen openFurnace(int entityId) {
        return new InventoryScreen(0, 0, ItemStack.EMPTY, false, entityId, NO_ENTITY);
    }

    /** Opens a chest UI backed by the given block-entity id. */
    public static InventoryScreen openChest(int entityId) {
        return new InventoryScreen(0, 0, ItemStack.EMPTY, false, NO_ENTITY, entityId);
    }

    public boolean isFurnaceOpen() { return furnaceEntityId != NO_ENTITY; }
    public boolean isChestOpen()   { return chestEntityId   != NO_ENTITY; }
}
