package org.example.world;

// Pure, stateless table that maps (blockType, heldItemId) to the item stack that should drop when
// a block is broken. Contains no game-state; every method is deterministic and fully testable.
// Hot-path safe: all lookups are array index operations, no HashMap.
public final class BlockDrops {

    private BlockDrops() {}

    // Returns the item id that the block drops, or -1 if there is no drop.
    // -1 is used as "nothing drops" (caller must check before spawning an entity).
    public static int dropItemId(BlockType blockType, int heldItemId) {
        if (blockType == BlockType.AIR || blockType == BlockType.WATER) return NO_DROP;

        // Check mining-level requirement first: if the held tool cannot harvest this block, no drop.
        if (!meetsRequiredLevel(blockType, heldItemId)) return NO_DROP;

        // Special case: stone drops cobblestone (represented as STONE itself in this engine since
        // there is no separate cobblestone block yet — keep symmetric with the existing system).
        return blockType.ordinal(); // item id == block type ordinal (block id)
    }

    // Returns true when the player would actually get a drop for breaking this block with this item.
    public static boolean yieldsDrop(BlockType blockType, int heldItemId) {
        return dropItemId(blockType, heldItemId) != NO_DROP;
    }

    // Sentinel for "no drop produced".
    public static final int NO_DROP = -1;

    // --- Private helpers ---

    private static boolean meetsRequiredLevel(BlockType blockType, int heldItemId) {
        int required = blockType.requiredMiningLevel();
        if (required == 0) return true; // any tool or bare hands is fine

        ItemRegistry.ItemData item = ItemRegistry.byId(heldItemId);
        if (!item.isTool()) return false; // bare hands cannot mine level-gated blocks
        if (item.toolKind() != blockType.effectiveTool()) return false; // wrong tool family

        ToolMaterial material = item.toolMaterial();
        return material != null && material.miningLevel() >= required;
    }
}
