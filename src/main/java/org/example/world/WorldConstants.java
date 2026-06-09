package org.example.world;

public final class WorldConstants {

    private WorldConstants() {}

    // Horizontal footprint of a chunk; the vertical extent is the full world height.
    // A chunk is therefore a column CHUNK_SIZE_XZ × WORLD_HEIGHT × CHUNK_SIZE_XZ.
    public static final int CHUNK_SIZE_XZ = 32;
    public static final int WORLD_HEIGHT  = 256;

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
    // Base sits well above WATER_LEVEL so dry land dominates and water only pools in real
    // depressions (lakes) and river channels, instead of flooding every low column.
    public static final int   TERRAIN_BASE_HEIGHT = 46;
    public static final int   TERRAIN_AMPLITUDE   = 10;
    public static final int   MOUNTAIN_AMPLITUDE  = 140;

    // Lakes: with plains kept flat (low TERRAIN_AMPLITUDE) the gentle hills no longer dip
    // below sea level, so a separate low-frequency noise sinks broad shallow basins into the
    // lowlands where water pools. Low frequency keeps plains locally flat between basins.
    public static final double BASIN_SCALE     = 0.010;
    public static final double BASIN_THRESHOLD = 0.05;
    public static final int    BASIN_OCTAVES   = 2;
    public static final int    BASIN_DEPTH     = 12;

    // Only mountain noise above this threshold lifts terrain, so plains and rolling hills
    // dominate and dramatic ranges stay rare. Range of the mask is roughly [-1, 1].
    public static final double MOUNTAIN_THRESHOLD = 0.20;

    // Valleys below this fill with water; surfaces at/above ROCK_LEVEL expose bare stone.
    public static final int   WATER_LEVEL = 40;
    public static final int   ROCK_LEVEL  = 96;

    // Rivers: a ridge of low-frequency noise (|noise| near zero) carves a winding channel
    // down to RIVER_BED_DEPTH below sea level. Only terrain up to RIVER_MAX_ELEVATION is
    // eligible, so rivers stay in the lowlands and never gouge canyons through mountains.
    public static final double RIVER_SCALE          = 0.004;
    public static final double RIVER_HALF_WIDTH     = 0.04;
    public static final int    RIVER_OCTAVES        = 2;
    public static final int    RIVER_BED_DEPTH      = 4;
    // Shared lowland ceiling: water features (rivers, lake basins) only apply at or below this
    // elevation, so they stay in the lowlands and never gouge channels or dents into mountains.
    public static final int    RIVER_MAX_ELEVATION  = WATER_LEVEL + 10;

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
