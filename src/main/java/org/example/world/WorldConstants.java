package org.example.world;

public final class WorldConstants {

    private WorldConstants() {}

    public static final int CHUNK_SIZE = 64;

    public static final int CHUNK_LOAD_RADIUS          = 3;
    public static final int CHUNK_UNLOAD_RADIUS        = 5;
    public static final int MAX_CHUNK_UPLOADS_PER_FRAME = 4;

    public static final byte BLOCK_AIR    = 0;
    public static final byte BLOCK_STONE  = 1;
    public static final byte BLOCK_DIRT   = 2;
    public static final byte BLOCK_GRASS  = 3;
    public static final byte BLOCK_WOOD   = 4;
    public static final byte BLOCK_LEAVES = 5;
    public static final byte BLOCK_WATER  = 6;

    public static final long  WORLD_SEED          = 42L;
    public static final int   TERRAIN_BASE_HEIGHT = 10;
    public static final int   TERRAIN_AMPLITUDE   = 8;
    public static final int   MOUNTAIN_AMPLITUDE  = 48;

    // Valleys below this fill with water; surfaces at/above ROCK_LEVEL expose bare stone.
    public static final int   WATER_LEVEL = 10;
    public static final int   ROCK_LEVEL  = 26;

    // One column in TREE_RARITY (statistically) sprouts a tree.
    public static final int TREE_RARITY            = 160;
    public static final int TREE_TRUNK_MIN_HEIGHT  = 4;
    public static final int TREE_TRUNK_MAX_HEIGHT  = 6;
    public static final int TREE_CANOPY_RADIUS     = 2;

    public static final float GRAVITY           = 20.0f;
    public static final float TERMINAL_VELOCITY = -50.0f;
    public static final float JUMP_IMPULSE      = 8.0f;
    public static final float PLAYER_EYE_HEIGHT = 1.6f;

    public static final float FLY_VERTICAL_SPEED        = 20.0f;
    public static final float DOUBLE_TAP_WINDOW_SECONDS = 0.3f;
}
