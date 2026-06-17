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
    }

    private final float[] colorTop;
    private final float[] colorSide;
    private final boolean solid;
    // Bare-hand hits needed to break the block; tools will later reduce the effective count.
    private final int     hardness;
    private final int     tileTop;
    private final int     tileSide;
    private final int     tileBottom;

    BlockType(float tr, float tg, float tb, float sr, float sg, float sb, boolean solid, int hardness,
              int tileTop, int tileSide, int tileBottom) {
        this.colorTop   = new float[]{ tr, tg, tb };
        this.colorSide  = new float[]{ sr, sg, sb };
        this.solid      = solid;
        this.hardness   = hardness;
        this.tileTop    = tileTop;
        this.tileSide   = tileSide;
        this.tileBottom = tileBottom;
    }

    BlockType(float r, float g, float b, boolean solid, int hardness,
              int tileTop, int tileSide, int tileBottom) {
        this(r, g, b, r, g, b, solid, hardness, tileTop, tileSide, tileBottom);
    }

    public float[] colorTop()  { return colorTop; }
    public float[] colorSide() { return colorSide; }
    public boolean solid()     { return solid; }
    public int     hardness()  { return hardness; }
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
    private static final BlockType[] BY_ID = { AIR, STONE, DIRT, GRASS, WOOD, LEAVES, WATER, IRON, DIAMOND };

    public static BlockType byId(byte id) {
        int i = id & 0xFF;
        return i < BY_ID.length ? BY_ID[i] : UNKNOWN;
    }
}
