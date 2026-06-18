package org.example.components;

// A stack of identical items. itemId reuses the BLOCK_* byte ids for blocks, food ids for
// consumables, and tool ids for tools (see world.ItemRegistry). count is bounded by
// ItemRegistry.maxStack(itemId) (tools stack to 1). durability tracks remaining uses for tools
// (0 for non-tools; when it reaches 0 the tool breaks). All stacking/mutation logic lives in
// world.Inventories. EMPTY is the no-null sentinel for an empty slot.
public record ItemStack(int itemId, int count, int durability) {

    // Convenience constructor for non-tool items (durability is not meaningful).
    public ItemStack(int itemId, int count) {
        this(itemId, count, 0);
    }

    public static final ItemStack EMPTY = new ItemStack(0, 0, 0);

    public boolean isEmpty() {
        return count <= 0;
    }
}
