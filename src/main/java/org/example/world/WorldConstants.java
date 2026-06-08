package org.example.world;

public final class WorldConstants {

    private WorldConstants() {}

    public static final int CHUNK_SIZE = 16;

    public static final byte BLOCK_AIR   = 0;
    public static final byte BLOCK_STONE = 1;
    public static final byte BLOCK_DIRT  = 2;
    public static final byte BLOCK_GRASS = 3;

    public static final long WORLD_SEED          = 42L;
    public static final int  TERRAIN_BASE_HEIGHT = 7;
    public static final int  TERRAIN_AMPLITUDE   = 5;
}
