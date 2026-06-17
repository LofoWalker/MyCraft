package org.example.world;

// Static table mapping a consumable item id to how much food and saturation eating it restores.
// Food ids live OUTSIDE the block id range (see WorldConstants.ITEM_*), so a food is never a block.
// Lookup is a small linear scan over a fixed array — not a hot path (driven by edge-triggered eating).
public final class Foods {

    public record Food(int itemId, int foodRestore, float saturationRestore) {}

    // Saturation restore is conventionally <= foodRestore (vanilla-style); the reservoir is then
    // re-clamped to the new food value by HungerMath.eat so it never exceeds the visible bar.
    private static final Food[] TABLE = {
            new Food(WorldConstants.ITEM_APPLE, 4, 2.4f),
            new Food(WorldConstants.ITEM_BREAD, 5, 6.0f),
    };

    private Foods() {}

    public static boolean isFood(int itemId) {
        return lookup(itemId) != null;
    }

    // Returns the Food for this id, or null when the id is not a registered consumable. Callers must
    // guard with isFood first; this is package-internal plumbing rather than a public ECS API surface.
    public static Food byId(int itemId) {
        Food food = lookup(itemId);
        if (food == null) throw new IllegalArgumentException("Not a food item id: " + itemId);
        return food;
    }

    private static Food lookup(int itemId) {
        for (Food food : TABLE) {
            if (food.itemId() == itemId) return food;
        }
        return null;
    }
}
