package org.example.components;

// Fixed-size grid of item stacks (hotbar slots first, then the backpack). The array reference is
// immutable from the record's point of view; mutation is done functionally in world.Inventories,
// which produces a fresh array rather than editing in place. Empty slots hold ItemStack.EMPTY, never
// null.
public record Inventory(ItemStack[] slots) {

    public int size() {
        return slots.length;
    }
}
