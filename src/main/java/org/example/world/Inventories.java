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

    // Stacks onto existing slots of the same item first (only for stackable items), then spills into
    // empty slots. Tools (maxStack == 1) are placed directly into empty slots without merging.
    // Returns the resulting inventory and the leftover that did not fit.
    public static AddResult add(Inventory inventory, ItemStack incoming) {
        if (incoming.isEmpty()) return new AddResult(inventory, ItemStack.EMPTY);

        int maxStack = ItemRegistry.maxStack(incoming.itemId());
        ItemStack[] slots = inventory.slots().clone();
        int remaining = incoming.count();

        if (maxStack > 1) {
            remaining = mergeIntoMatching(slots, incoming.itemId(), remaining, maxStack);
        }
        remaining = fillEmptySlots(slots, incoming.itemId(), remaining, maxStack, incoming.durability());

        ItemStack remainder = remaining > 0
                ? new ItemStack(incoming.itemId(), remaining, incoming.durability())
                : ItemStack.EMPTY;
        return new AddResult(new Inventory(slots), remainder);
    }

    public static Inventory removeOne(Inventory inventory, int slot) {
        ItemStack current = inventory.slots()[slot];
        if (current.isEmpty()) return inventory;

        ItemStack[] slots = inventory.slots().clone();
        int left = current.count() - 1;
        slots[slot] = left > 0 ? new ItemStack(current.itemId(), left, current.durability()) : ItemStack.EMPTY;
        return new Inventory(slots);
    }

    // Decrements durability of the tool in the given slot. If durability reaches 0, the tool breaks
    // and the slot becomes empty. Returns the updated inventory.
    public static Inventory decrementDurability(Inventory inventory, int slot) {
        ItemStack current = inventory.slots()[slot];
        if (current.isEmpty() || !ItemRegistry.isTool(current.itemId())) return inventory;

        ItemStack[] slots = inventory.slots().clone();
        int remaining = current.durability() - 1;
        slots[slot] = remaining > 0
                ? new ItemStack(current.itemId(), current.count(), remaining)
                : ItemStack.EMPTY;
        return new Inventory(slots);
    }

    // Creates a fresh tool ItemStack with full durability from the registry.
    public static ItemStack newTool(int toolItemId) {
        int durability = ItemRegistry.byId(toolItemId).durability();
        return new ItemStack(toolItemId, 1, durability);
    }

    private static int mergeIntoMatching(ItemStack[] slots, int itemId, int remaining, int maxStack) {
        for (int i = 0; i < slots.length && remaining > 0; i++) {
            ItemStack slot = slots[i];
            if (slot.isEmpty() || slot.itemId() != itemId) continue;
            int space = maxStack - slot.count();
            if (space <= 0) continue;
            int moved = Math.min(space, remaining);
            slots[i] = new ItemStack(itemId, slot.count() + moved);
            remaining -= moved;
        }
        return remaining;
    }

    private static int fillEmptySlots(ItemStack[] slots, int itemId, int remaining,
                                      int maxStack, int durability) {
        for (int i = 0; i < slots.length && remaining > 0; i++) {
            if (!slots[i].isEmpty()) continue;
            int moved = Math.min(maxStack, remaining);
            slots[i] = new ItemStack(itemId, moved, durability);
            remaining -= moved;
        }
        return remaining;
    }
}
