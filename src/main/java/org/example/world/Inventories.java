package org.example.world;

import org.example.components.Inventory;
import org.example.components.ItemStack;

import java.util.Arrays;

// Pure functional operations on Inventory/ItemStack. Records stay data-only, so every mutation here
// returns a fresh Inventory (with a fresh slot array) and never edits the input in place. Not a hot
// path (driven by edge-triggered player actions), so clarity is favoured over allocation count.
public final class Inventories {

    private Inventories() {}

    // Result of an add(): the updated inventory plus whatever could not fit (ItemStack.EMPTY when it
    // all fit). The caller decides what to do with the remainder (drop it, keep it, etc.).
    public record AddResult(Inventory inventory, ItemStack remainder) {}

    public static Inventory empty() {
        ItemStack[] slots = new ItemStack[WorldConstants.INVENTORY_SLOTS];
        Arrays.fill(slots, ItemStack.EMPTY);
        return new Inventory(slots);
    }

    public static ItemStack get(Inventory inventory, int slot) {
        return inventory.slots()[slot];
    }

    // Stacks onto existing slots of the same item first, then spills into empty slots. Returns the
    // resulting inventory and the leftover that did not fit.
    public static AddResult add(Inventory inventory, ItemStack incoming) {
        if (incoming.isEmpty()) return new AddResult(inventory, ItemStack.EMPTY);

        ItemStack[] slots = inventory.slots().clone();
        int remaining = mergeIntoMatching(slots, incoming.itemId(), incoming.count());
        remaining = fillEmptySlots(slots, incoming.itemId(), remaining);

        ItemStack remainder = remaining > 0 ? new ItemStack(incoming.itemId(), remaining) : ItemStack.EMPTY;
        return new AddResult(new Inventory(slots), remainder);
    }

    public static Inventory removeOne(Inventory inventory, int slot) {
        ItemStack current = inventory.slots()[slot];
        if (current.isEmpty()) return inventory;

        ItemStack[] slots = inventory.slots().clone();
        int left = current.count() - 1;
        slots[slot] = left > 0 ? new ItemStack(current.itemId(), left) : ItemStack.EMPTY;
        return new Inventory(slots);
    }

    private static int mergeIntoMatching(ItemStack[] slots, int itemId, int remaining) {
        for (int i = 0; i < slots.length && remaining > 0; i++) {
            ItemStack slot = slots[i];
            if (slot.isEmpty() || slot.itemId() != itemId) continue;
            int space = WorldConstants.MAX_STACK - slot.count();
            if (space <= 0) continue;
            int moved = Math.min(space, remaining);
            slots[i] = new ItemStack(itemId, slot.count() + moved);
            remaining -= moved;
        }
        return remaining;
    }

    private static int fillEmptySlots(ItemStack[] slots, int itemId, int remaining) {
        for (int i = 0; i < slots.length && remaining > 0; i++) {
            if (!slots[i].isEmpty()) continue;
            int moved = Math.min(WorldConstants.MAX_STACK, remaining);
            slots[i] = new ItemStack(itemId, moved);
            remaining -= moved;
        }
        return remaining;
    }
}
