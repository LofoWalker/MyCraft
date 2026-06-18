package org.example.world;

// Immutable flat table of every item in the game, indexed by item id. Block ids (0..MAX_BLOCK_ID)
// have entries automatically derived from BlockType. Tool and other non-block items start at
// TOOL_ID_BASE. Lookup is a plain array index — no HashMap on any path.
public final class ItemRegistry {

    // Tool item ids: group them by kind then by material for easy range checks.
    public static final int PICKAXE_WOOD    = 200;
    public static final int PICKAXE_STONE   = 201;
    public static final int PICKAXE_IRON    = 202;
    public static final int PICKAXE_GOLD    = 203;
    public static final int PICKAXE_DIAMOND = 204;

    public static final int AXE_WOOD    = 210;
    public static final int AXE_STONE   = 211;
    public static final int AXE_IRON    = 212;
    public static final int AXE_GOLD    = 213;
    public static final int AXE_DIAMOND = 214;

    public static final int SHOVEL_WOOD    = 220;
    public static final int SHOVEL_STONE   = 221;
    public static final int SHOVEL_IRON    = 222;
    public static final int SHOVEL_GOLD    = 223;
    public static final int SHOVEL_DIAMOND = 224;

    public static final int SWORD_WOOD    = 230;
    public static final int SWORD_STONE   = 231;
    public static final int SWORD_IRON    = 232;
    public static final int SWORD_GOLD    = 233;
    public static final int SWORD_DIAMOND = 234;

    private static final int TABLE_SIZE = 512;

    // Per-item data; indexed by itemId. Slots that have no item defined use DEFAULT_ENTRY.
    public record ItemData(
            int       itemId,
            int       maxStack,
            boolean   isBlock,
            boolean   isTool,
            ToolKind  toolKind,
            ToolMaterial toolMaterial,
            int       durability
    ) {}

    private static final ItemData DEFAULT_ENTRY = new ItemData(
            -1, WorldConstants.MAX_STACK, false, false, ToolKind.NONE, null, 0);

    private static final ItemData[] TABLE = new ItemData[TABLE_SIZE];

    static {
        // Block entries — one per BlockType ordinal (ordinal == block id).
        BlockType[] blockTypes = BlockType.values();
        for (int id = 0; id < blockTypes.length; id++) {
            BlockType bt = blockTypes[id];
            // Skip UNKNOWN (id maps past the BLOCK_* range).
            if (id > WorldConstants.MAX_BLOCK_ID) break;
            TABLE[id] = new ItemData(id, WorldConstants.MAX_STACK, bt != BlockType.AIR,
                    false, ToolKind.NONE, null, 0);
        }

        // Food entries (non-block, non-tool).
        TABLE[WorldConstants.ITEM_APPLE] = foodEntry(WorldConstants.ITEM_APPLE);
        TABLE[WorldConstants.ITEM_BREAD] = foodEntry(WorldConstants.ITEM_BREAD);

        // Intermediate crafting material: sticks (not a food, not a tool, not a block).
        TABLE[WorldConstants.ITEM_STICK] = new ItemData(
                WorldConstants.ITEM_STICK, WorldConstants.MAX_STACK, false, false, ToolKind.NONE, null, 0);

        // Smelting output items (STEP-28). Iron ingot is produced by smelting iron ore in a furnace.
        TABLE[WorldConstants.ITEM_IRON_INGOT] = new ItemData(
                WorldConstants.ITEM_IRON_INGOT, WorldConstants.MAX_STACK, false, false, ToolKind.NONE, null, 0);

        // Tools.
        registerTool(PICKAXE_WOOD,    ToolKind.PICKAXE, ToolMaterial.WOOD,    59);
        registerTool(PICKAXE_STONE,   ToolKind.PICKAXE, ToolMaterial.STONE,   131);
        registerTool(PICKAXE_IRON,    ToolKind.PICKAXE, ToolMaterial.IRON,    250);
        registerTool(PICKAXE_GOLD,    ToolKind.PICKAXE, ToolMaterial.GOLD,    32);
        registerTool(PICKAXE_DIAMOND, ToolKind.PICKAXE, ToolMaterial.DIAMOND, 1561);

        registerTool(AXE_WOOD,    ToolKind.AXE, ToolMaterial.WOOD,    59);
        registerTool(AXE_STONE,   ToolKind.AXE, ToolMaterial.STONE,   131);
        registerTool(AXE_IRON,    ToolKind.AXE, ToolMaterial.IRON,    250);
        registerTool(AXE_GOLD,    ToolKind.AXE, ToolMaterial.GOLD,    32);
        registerTool(AXE_DIAMOND, ToolKind.AXE, ToolMaterial.DIAMOND, 1561);

        registerTool(SHOVEL_WOOD,    ToolKind.SHOVEL, ToolMaterial.WOOD,    59);
        registerTool(SHOVEL_STONE,   ToolKind.SHOVEL, ToolMaterial.STONE,   131);
        registerTool(SHOVEL_IRON,    ToolKind.SHOVEL, ToolMaterial.IRON,    250);
        registerTool(SHOVEL_GOLD,    ToolKind.SHOVEL, ToolMaterial.GOLD,    32);
        registerTool(SHOVEL_DIAMOND, ToolKind.SHOVEL, ToolMaterial.DIAMOND, 1561);

        registerTool(SWORD_WOOD,    ToolKind.SWORD, ToolMaterial.WOOD,    59);
        registerTool(SWORD_STONE,   ToolKind.SWORD, ToolMaterial.STONE,   131);
        registerTool(SWORD_IRON,    ToolKind.SWORD, ToolMaterial.IRON,    250);
        registerTool(SWORD_GOLD,    ToolKind.SWORD, ToolMaterial.GOLD,    32);
        registerTool(SWORD_DIAMOND, ToolKind.SWORD, ToolMaterial.DIAMOND, 1561);

        // Fill any remaining slots with the default sentinel.
        for (int i = 0; i < TABLE.length; i++) {
            if (TABLE[i] == null) TABLE[i] = DEFAULT_ENTRY;
        }
    }

    private ItemRegistry() {}

    // --- Public API (array lookups, no HashMap) ---

    public static ItemData byId(int itemId) {
        if (itemId < 0 || itemId >= TABLE.length) return DEFAULT_ENTRY;
        return TABLE[itemId];
    }

    public static int maxStack(int itemId) {
        return byId(itemId).maxStack();
    }

    public static boolean isTool(int itemId) {
        return byId(itemId).isTool();
    }

    public static ToolKind toolKind(int itemId) {
        return byId(itemId).toolKind();
    }

    public static ToolMaterial toolMaterial(int itemId) {
        return byId(itemId).toolMaterial();
    }

    // Damage per hit when the player holds itemId and strikes blockType. Bare hands use
    // BARE_HAND_DAMAGE; the correct tool for the block multiplies the effective hardness-per-hit.
    public static int damagePerHit(BlockType blockType, int heldItemId) {
        ItemData item = byId(heldItemId);
        if (!item.isTool()) return WorldConstants.BARE_HAND_DAMAGE;

        boolean correctKind = item.toolKind() == blockType.effectiveTool();
        if (!correctKind) return WorldConstants.BARE_HAND_DAMAGE;

        // Apply the material speed multiplier: each hit deals more "damage" (counted in hits).
        float multiplier = item.toolMaterial().speedMultiplier();
        return Math.max(1, Math.round(multiplier));
    }

    // --- Private helpers ---

    private static ItemData foodEntry(int id) {
        return new ItemData(id, WorldConstants.MAX_STACK, false, false, ToolKind.NONE, null, 0);
    }

    private static void registerTool(int id, ToolKind kind, ToolMaterial material, int durability) {
        TABLE[id] = new ItemData(id, 1, false, true, kind, material, durability);
    }
}
