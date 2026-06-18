package org.example.world;

/**
 * Registry of block kinds: rendering color (top face may differ from sides) and
 * collision solidity. The block id stored in {@link org.example.components.VoxelChunkData}
 * is the index into {@link #BY_ID}, kept in sync with the {@code BLOCK_*} bytes in
 * {@link WorldConstants}. Lookup is an array index — no map in the meshing hot path.
 */
public enum BlockType {
    AIR   (0.00f, 0.00f, 0.00f, false, 0, Tile.DIRT, Tile.DIRT, Tile.DIRT),
    STONE (0.50f, 0.50f, 0.50f, true,  5, Tile.STONE, Tile.STONE, Tile.STONE),
    DIRT  (0.55f, 0.35f, 0.15f, true,  2, Tile.DIRT, Tile.DIRT, Tile.DIRT),
    GRASS (0.35f, 0.70f, 0.25f,  0.55f, 0.35f, 0.15f, true, 2, Tile.GRASS_TOP, Tile.GRASS_SIDE, Tile.DIRT),
    WOOD  (0.40f, 0.26f, 0.13f, true,  3, Tile.WOOD_TOP, Tile.WOOD_SIDE, Tile.WOOD_TOP),
    LEAVES(0.18f, 0.50f, 0.16f, true,  1, Tile.LEAVES, Tile.LEAVES, Tile.LEAVES),
    WATER (0.20f, 0.40f, 0.85f, false, 0, Tile.WATER, Tile.WATER, Tile.WATER),
    IRON  (0.78f, 0.66f, 0.52f, true,  7, Tile.IRON_ORE, Tile.IRON_ORE, Tile.IRON_ORE),
    DIAMOND(0.40f, 0.85f, 0.90f, true, 9, Tile.DIAMOND_ORE, Tile.DIAMOND_ORE, Tile.DIAMOND_ORE),
    // A torch is non-solid (light/walk through it) and emits blocklight; it is mined in a single hit.
    TORCH (1.00f, 0.85f, 0.40f, false, 1, WorldConstants.TORCH_EMISSION, Tile.TORCH, Tile.TORCH, Tile.TORCH),
    // Flowing water levels 7..1 (ids 10..16): non-solid, same atlas tile as static water.
    WATER_FLOW_7(0.20f, 0.40f, 0.85f, false, 0, Tile.WATER, Tile.WATER, Tile.WATER),
    WATER_FLOW_6(0.20f, 0.40f, 0.85f, false, 0, Tile.WATER, Tile.WATER, Tile.WATER),
    WATER_FLOW_5(0.20f, 0.40f, 0.85f, false, 0, Tile.WATER, Tile.WATER, Tile.WATER),
    WATER_FLOW_4(0.20f, 0.40f, 0.85f, false, 0, Tile.WATER, Tile.WATER, Tile.WATER),
    WATER_FLOW_3(0.20f, 0.40f, 0.85f, false, 0, Tile.WATER, Tile.WATER, Tile.WATER),
    WATER_FLOW_2(0.20f, 0.40f, 0.85f, false, 0, Tile.WATER, Tile.WATER, Tile.WATER),
    WATER_FLOW_1(0.20f, 0.40f, 0.85f, false, 0, Tile.WATER, Tile.WATER, Tile.WATER),
    // Lava source (id 17) and flowing levels 6..1 (ids 18..23): non-solid, slow-moving fluid.
    LAVA        (0.95f, 0.40f, 0.05f, false, 0, Tile.LAVA, Tile.LAVA, Tile.LAVA),
    LAVA_FLOW_6 (0.95f, 0.40f, 0.05f, false, 0, Tile.LAVA, Tile.LAVA, Tile.LAVA),
    LAVA_FLOW_5 (0.95f, 0.40f, 0.05f, false, 0, Tile.LAVA, Tile.LAVA, Tile.LAVA),
    LAVA_FLOW_4 (0.95f, 0.40f, 0.05f, false, 0, Tile.LAVA, Tile.LAVA, Tile.LAVA),
    LAVA_FLOW_3 (0.95f, 0.40f, 0.05f, false, 0, Tile.LAVA, Tile.LAVA, Tile.LAVA),
    LAVA_FLOW_2 (0.95f, 0.40f, 0.05f, false, 0, Tile.LAVA, Tile.LAVA, Tile.LAVA),
    LAVA_FLOW_1 (0.95f, 0.40f, 0.05f, false, 0, Tile.LAVA, Tile.LAVA, Tile.LAVA),
    // Obsidian (id 24): created where lava source meets water; very hard to mine.
    OBSIDIAN    (0.10f, 0.08f, 0.15f, true,  50, Tile.OBSIDIAN, Tile.OBSIDIAN, Tile.OBSIDIAN),
    UNKNOWN(1.00f, 0.00f, 1.00f, true, 1, Tile.STONE, Tile.STONE, Tile.STONE);

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
        static final int OBSIDIAN    = 13;
    }

    private static final int NO_EMISSION = 0;

    private final float[] colorTop;
    private final float[] colorSide;
    private final boolean solid;
    // Bare-hand hits needed to break the block; tools will later reduce the effective count.
    private final int     hardness;
    // Blocklight level this block radiates (0 = not a light source); seeds the LightEngine flood-fill.
    private final int     lightEmission;
    private final int     tileTop;
    private final int     tileSide;
    private final int     tileBottom;

    BlockType(float tr, float tg, float tb, float sr, float sg, float sb, boolean solid, int hardness,
              int lightEmission, int tileTop, int tileSide, int tileBottom) {
        this.colorTop      = new float[]{ tr, tg, tb };
        this.colorSide     = new float[]{ sr, sg, sb };
        this.solid         = solid;
        this.hardness      = hardness;
        this.lightEmission = lightEmission;
        this.tileTop       = tileTop;
        this.tileSide      = tileSide;
        this.tileBottom    = tileBottom;
    }

    BlockType(float tr, float tg, float tb, float sr, float sg, float sb, boolean solid, int hardness,
              int tileTop, int tileSide, int tileBottom) {
        this(tr, tg, tb, sr, sg, sb, solid, hardness, NO_EMISSION, tileTop, tileSide, tileBottom);
    }

    BlockType(float r, float g, float b, boolean solid, int hardness,
              int tileTop, int tileSide, int tileBottom) {
        this(r, g, b, r, g, b, solid, hardness, NO_EMISSION, tileTop, tileSide, tileBottom);
    }

    BlockType(float r, float g, float b, boolean solid, int hardness, int lightEmission,
              int tileTop, int tileSide, int tileBottom) {
        this(r, g, b, r, g, b, solid, hardness, lightEmission, tileTop, tileSide, tileBottom);
    }

    public float[] colorTop()  { return colorTop; }
    public float[] colorSide() { return colorSide; }
    public boolean solid()     { return solid; }
    public int     hardness()  { return hardness; }
    public int     lightEmission() { return lightEmission; }
    public int     tileTop()    { return tileTop; }
    public int     tileSide()   { return tileSide; }
    public int     tileBottom() { return tileBottom; }

    // Mesh tint multiplied against the sampled atlas texel. Painted blocks tint white (texture shown
    // at full strength); UNKNOWN keeps its magenta so an unmapped id stays visibly wrong in-world.
    private static final float[] WHITE_TINT = { 1f, 1f, 1f };

    public float[] tint() {
        return this == UNKNOWN ? colorTop : WHITE_TINT;
    }

    // Index order must match the BLOCK_* byte ids in WorldConstants.
    // ids: 0=AIR 1=STONE 2=DIRT 3=GRASS 4=WOOD 5=LEAVES 6=WATER 7=IRON 8=DIAMOND 9=TORCH
    //      10..16=WATER_FLOW_7..1  17=LAVA  18..23=LAVA_FLOW_6..1  24=OBSIDIAN
    private static final BlockType[] BY_ID = {
        AIR, STONE, DIRT, GRASS, WOOD, LEAVES, WATER, IRON, DIAMOND, TORCH,
        WATER_FLOW_7, WATER_FLOW_6, WATER_FLOW_5, WATER_FLOW_4,
        WATER_FLOW_3, WATER_FLOW_2, WATER_FLOW_1,
        LAVA, LAVA_FLOW_6, LAVA_FLOW_5, LAVA_FLOW_4, LAVA_FLOW_3, LAVA_FLOW_2, LAVA_FLOW_1,
        OBSIDIAN
    };

    public static BlockType byId(byte id) {
        int i = id & 0xFF;
        return i < BY_ID.length ? BY_ID[i] : UNKNOWN;
    }
}
