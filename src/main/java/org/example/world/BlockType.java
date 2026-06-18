package org.example.world;

/**
 * Registry of block kinds: rendering color (top face may differ from sides) and
 * collision solidity. The block id stored in {@link org.example.components.VoxelChunkData}
 * is the index into {@link #BY_ID}, kept in sync with the {@code BLOCK_*} bytes in
 * {@link WorldConstants}. Lookup is an array index — no map in the meshing hot path.
 */
public enum BlockType {
    // effectiveTool: tool kind that speeds up breaking (and may be required for drops).
    // requiredMiningLevel: minimum ToolMaterial.miningLevel needed to produce a drop (0 = any tool or bare hand).
    AIR    (0.00f, 0.00f, 0.00f, false, 0, Tile.DIRT,         Tile.DIRT,        Tile.DIRT,        ToolKind.NONE,    0),
    STONE  (0.50f, 0.50f, 0.50f, true,  5, Tile.STONE,        Tile.STONE,       Tile.STONE,       ToolKind.PICKAXE, 1),
    DIRT   (0.55f, 0.35f, 0.15f, true,  2, Tile.DIRT,         Tile.DIRT,        Tile.DIRT,        ToolKind.SHOVEL,  0),
    GRASS  (0.35f, 0.70f, 0.25f, 0.55f, 0.35f, 0.15f, true, 2, Tile.GRASS_TOP, Tile.GRASS_SIDE,  Tile.DIRT,        ToolKind.SHOVEL,  0),
    WOOD   (0.40f, 0.26f, 0.13f, true,  3, Tile.WOOD_TOP,     Tile.WOOD_SIDE,   Tile.WOOD_TOP,    ToolKind.AXE,     0),
    LEAVES (0.18f, 0.50f, 0.16f, true,  1, Tile.LEAVES,       Tile.LEAVES,      Tile.LEAVES,      ToolKind.NONE,    0),
    WATER  (0.20f, 0.40f, 0.85f, false, 0, Tile.WATER,        Tile.WATER,       Tile.WATER,       ToolKind.NONE,    0),
    IRON   (0.78f, 0.66f, 0.52f, true,  7, Tile.IRON_ORE,     Tile.IRON_ORE,    Tile.IRON_ORE,    ToolKind.PICKAXE, 2),
    DIAMOND(0.40f, 0.85f, 0.90f, true,  9, Tile.DIAMOND_ORE,  Tile.DIAMOND_ORE, Tile.DIAMOND_ORE, ToolKind.PICKAXE, 3),
    // A torch is non-solid (light/walk through it) and emits blocklight; it is mined in a single hit.
    TORCH  (1.00f, 0.85f, 0.40f, false, 1, WorldConstants.TORCH_EMISSION, Tile.TORCH, Tile.TORCH, Tile.TORCH, ToolKind.NONE, 0),
    SAND   (0.93f, 0.87f, 0.62f, true,  1, Tile.SAND, Tile.SAND, Tile.SAND, ToolKind.SHOVEL, 0),
    // Flowing water levels 7..1 (ids 11..17): non-solid, same atlas tile as static water.
    WATER_FLOW_7(0.20f, 0.40f, 0.85f, false, 0, Tile.WATER, Tile.WATER, Tile.WATER, ToolKind.NONE, 0),
    WATER_FLOW_6(0.20f, 0.40f, 0.85f, false, 0, Tile.WATER, Tile.WATER, Tile.WATER, ToolKind.NONE, 0),
    WATER_FLOW_5(0.20f, 0.40f, 0.85f, false, 0, Tile.WATER, Tile.WATER, Tile.WATER, ToolKind.NONE, 0),
    WATER_FLOW_4(0.20f, 0.40f, 0.85f, false, 0, Tile.WATER, Tile.WATER, Tile.WATER, ToolKind.NONE, 0),
    WATER_FLOW_3(0.20f, 0.40f, 0.85f, false, 0, Tile.WATER, Tile.WATER, Tile.WATER, ToolKind.NONE, 0),
    WATER_FLOW_2(0.20f, 0.40f, 0.85f, false, 0, Tile.WATER, Tile.WATER, Tile.WATER, ToolKind.NONE, 0),
    WATER_FLOW_1(0.20f, 0.40f, 0.85f, false, 0, Tile.WATER, Tile.WATER, Tile.WATER, ToolKind.NONE, 0),
    // Lava source (id 18) and flowing levels 6..1 (ids 19..24): non-solid, slow-moving fluid.
    LAVA        (0.95f, 0.40f, 0.05f, false, 0, Tile.LAVA, Tile.LAVA, Tile.LAVA, ToolKind.NONE, 0),
    LAVA_FLOW_6 (0.95f, 0.40f, 0.05f, false, 0, Tile.LAVA, Tile.LAVA, Tile.LAVA, ToolKind.NONE, 0),
    LAVA_FLOW_5 (0.95f, 0.40f, 0.05f, false, 0, Tile.LAVA, Tile.LAVA, Tile.LAVA, ToolKind.NONE, 0),
    LAVA_FLOW_4 (0.95f, 0.40f, 0.05f, false, 0, Tile.LAVA, Tile.LAVA, Tile.LAVA, ToolKind.NONE, 0),
    LAVA_FLOW_3 (0.95f, 0.40f, 0.05f, false, 0, Tile.LAVA, Tile.LAVA, Tile.LAVA, ToolKind.NONE, 0),
    LAVA_FLOW_2 (0.95f, 0.40f, 0.05f, false, 0, Tile.LAVA, Tile.LAVA, Tile.LAVA, ToolKind.NONE, 0),
    LAVA_FLOW_1 (0.95f, 0.40f, 0.05f, false, 0, Tile.LAVA, Tile.LAVA, Tile.LAVA, ToolKind.NONE, 0),
    // Obsidian (id 25): created where lava source meets water; needs a diamond pickaxe.
    OBSIDIAN        (0.10f, 0.08f, 0.15f, true,  50, Tile.OBSIDIAN,       Tile.OBSIDIAN,       Tile.OBSIDIAN,    ToolKind.PICKAXE, 4),
    // Crafting table (id 26): placed by the player; opens a 3×3 crafting grid on right-click.
    CRAFTING_TABLE  (0.55f, 0.36f, 0.15f, 0.40f, 0.26f, 0.13f, true, 3, Tile.CRAFTING_TOP, Tile.CRAFTING_SIDE, Tile.WOOD_TOP, ToolKind.AXE, 0),
    UNKNOWN(1.00f, 0.00f, 1.00f, true,  1, Tile.STONE,        Tile.STONE,       Tile.STONE,       ToolKind.NONE,    0);

    // Linear tile indices into textures/blocks.png (index = tileY*16 + tileX, top-left origin).
    // Nested holder so the constants are initialised before the enum constants reference them
    // (a plain static field cannot be referenced from an enum constant's constructor arguments).
    private static final class Tile {
        static final int GRASS_TOP   = 0;
        static final int GRASS_SIDE  = 1;
        static final int DIRT        = 2;
        static final int STONE       = 3;
        static final int WOOD_TOP    = 4;
        static final int WOOD_SIDE   = 5;
        static final int LEAVES      = 6;
        static final int WATER       = 7;
        static final int SAND        = 8;
        static final int IRON_ORE    = 9;
        static final int DIAMOND_ORE = 10;
        static final int TORCH       = 11;
        // Fluid and new blocks (STEP-32).
        static final int LAVA        = 12;
        static final int OBSIDIAN        = 13;
        // Crafting table: top face shows the 2x2 work surface, sides show the wood texture variant.
        static final int CRAFTING_TOP  = 14;
        static final int CRAFTING_SIDE = 15;
    }

    private static final int NO_EMISSION = 0;

    private final float[]  colorTop;
    private final float[]  colorSide;
    private final boolean  solid;
    // Bare-hand hits needed to break the block; tools with the right kind deal more damage per hit.
    private final int      hardness;
    // Blocklight level this block radiates (0 = not a light source); seeds the LightEngine flood-fill.
    private final int      lightEmission;
    private final int      tileTop;
    private final int      tileSide;
    private final int      tileBottom;
    // Tool family that is "effective" against this block (accelerates breaking, required for drops).
    private final ToolKind effectiveTool;
    // Minimum ToolMaterial.miningLevel required to obtain a drop (0 = any means incl. bare hands).
    private final int      requiredMiningLevel;

    BlockType(float tr, float tg, float tb, float sr, float sg, float sb, boolean solid, int hardness,
              int lightEmission, int tileTop, int tileSide, int tileBottom,
              ToolKind effectiveTool, int requiredMiningLevel) {
        this.colorTop            = new float[]{ tr, tg, tb };
        this.colorSide           = new float[]{ sr, sg, sb };
        this.solid               = solid;
        this.hardness            = hardness;
        this.lightEmission       = lightEmission;
        this.tileTop             = tileTop;
        this.tileSide            = tileSide;
        this.tileBottom          = tileBottom;
        this.effectiveTool       = effectiveTool;
        this.requiredMiningLevel = requiredMiningLevel;
    }

    // Separate-color (top vs side) constructor without light emission.
    BlockType(float tr, float tg, float tb, float sr, float sg, float sb, boolean solid, int hardness,
              int tileTop, int tileSide, int tileBottom,
              ToolKind effectiveTool, int requiredMiningLevel) {
        this(tr, tg, tb, sr, sg, sb, solid, hardness, NO_EMISSION,
                tileTop, tileSide, tileBottom, effectiveTool, requiredMiningLevel);
    }

    // Uniform color constructor without light emission.
    BlockType(float r, float g, float b, boolean solid, int hardness,
              int tileTop, int tileSide, int tileBottom,
              ToolKind effectiveTool, int requiredMiningLevel) {
        this(r, g, b, r, g, b, solid, hardness, NO_EMISSION,
                tileTop, tileSide, tileBottom, effectiveTool, requiredMiningLevel);
    }

    // Uniform color constructor WITH light emission.
    BlockType(float r, float g, float b, boolean solid, int hardness, int lightEmission,
              int tileTop, int tileSide, int tileBottom,
              ToolKind effectiveTool, int requiredMiningLevel) {
        this(r, g, b, r, g, b, solid, hardness, lightEmission,
                tileTop, tileSide, tileBottom, effectiveTool, requiredMiningLevel);
    }

    public float[]  colorTop()           { return colorTop; }
    public float[]  colorSide()          { return colorSide; }
    public boolean  solid()              { return solid; }
    public int      hardness()           { return hardness; }
    public int      lightEmission()      { return lightEmission; }
    public int      tileTop()            { return tileTop; }
    public int      tileSide()           { return tileSide; }
    public int      tileBottom()         { return tileBottom; }
    public ToolKind effectiveTool()      { return effectiveTool; }
    public int      requiredMiningLevel(){ return requiredMiningLevel; }

    // Mesh tint multiplied against the sampled atlas texel. Painted blocks tint white (texture shown
    // at full strength); UNKNOWN keeps its magenta so an unmapped id stays visibly wrong in-world.
    private static final float[] WHITE_TINT = { 1f, 1f, 1f };

    public float[] tint() {
        return this == UNKNOWN ? colorTop : WHITE_TINT;
    }

    // Index order must match the BLOCK_* byte ids in WorldConstants.
    // ids: 0=AIR 1=STONE 2=DIRT 3=GRASS 4=WOOD 5=LEAVES 6=WATER 7=IRON 8=DIAMOND 9=TORCH 10=SAND
    //      11..17=WATER_FLOW_7..1  18=LAVA  19..24=LAVA_FLOW_6..1  25=OBSIDIAN  26=CRAFTING_TABLE
    private static final BlockType[] BY_ID = {
        AIR, STONE, DIRT, GRASS, WOOD, LEAVES, WATER, IRON, DIAMOND, TORCH, SAND,
        WATER_FLOW_7, WATER_FLOW_6, WATER_FLOW_5, WATER_FLOW_4,
        WATER_FLOW_3, WATER_FLOW_2, WATER_FLOW_1,
        LAVA, LAVA_FLOW_6, LAVA_FLOW_5, LAVA_FLOW_4, LAVA_FLOW_3, LAVA_FLOW_2, LAVA_FLOW_1,
        OBSIDIAN, CRAFTING_TABLE
    };

    public static BlockType byId(byte id) {
        int i = id & 0xFF;
        return i < BY_ID.length ? BY_ID[i] : UNKNOWN;
    }
}
