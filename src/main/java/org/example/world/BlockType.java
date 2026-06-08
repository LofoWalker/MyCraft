package org.example.world;

/**
 * Registry of block kinds: rendering color (top face may differ from sides) and
 * collision solidity. The block id stored in {@link org.example.components.VoxelChunkData}
 * is the index into {@link #BY_ID}, kept in sync with the {@code BLOCK_*} bytes in
 * {@link WorldConstants}. Lookup is an array index — no map in the meshing hot path.
 */
public enum BlockType {
    AIR   (0.00f, 0.00f, 0.00f, false),
    STONE (0.50f, 0.50f, 0.50f, true),
    DIRT  (0.55f, 0.35f, 0.15f, true),
    GRASS (0.35f, 0.70f, 0.25f,  0.55f, 0.35f, 0.15f, true),
    WOOD  (0.40f, 0.26f, 0.13f, true),
    LEAVES(0.18f, 0.50f, 0.16f, true),
    WATER (0.20f, 0.40f, 0.85f, false),
    UNKNOWN(1.00f, 0.00f, 1.00f, true);

    private final float[] colorTop;
    private final float[] colorSide;
    private final boolean solid;

    BlockType(float tr, float tg, float tb, float sr, float sg, float sb, boolean solid) {
        this.colorTop  = new float[]{ tr, tg, tb };
        this.colorSide = new float[]{ sr, sg, sb };
        this.solid     = solid;
    }

    BlockType(float r, float g, float b, boolean solid) {
        this(r, g, b, r, g, b, solid);
    }

    public float[] colorTop()  { return colorTop; }
    public float[] colorSide() { return colorSide; }
    public boolean solid()     { return solid; }

    // Index order must match the BLOCK_* byte ids in WorldConstants.
    private static final BlockType[] BY_ID = { AIR, STONE, DIRT, GRASS, WOOD, LEAVES, WATER };

    public static BlockType byId(byte id) {
        int i = id & 0xFF;
        return i < BY_ID.length ? BY_ID[i] : UNKNOWN;
    }
}
