package org.example.components;

// A stack of identical items. itemId reuses the BLOCK_* byte ids (one item == one placeable block),
// widened to int for convenience. count is bounded by WorldConstants.MAX_STACK. Immutable data only:
// all stacking/splitting logic lives in world.Inventories. EMPTY is the no-null sentinel for an empty
// slot (itemId AIR, count 0).
public record ItemStack(int itemId, int count) {

    public static final ItemStack EMPTY = new ItemStack(0, 0);

    public boolean isEmpty() {
        return count <= 0;
    }
}
