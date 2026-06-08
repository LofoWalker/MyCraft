package org.example.world;

public final class WorldConstants {

    private WorldConstants() {}

    public static final int CHUNK_SIZE = 512;

    public static final byte BLOCK_AIR   = 0;
    public static final byte BLOCK_STONE = 1;
    public static final byte BLOCK_DIRT  = 2;
    public static final byte BLOCK_GRASS = 3;

    public static final long  WORLD_SEED          = 42L;
    public static final int   TERRAIN_BASE_HEIGHT = 7;
    public static final int   TERRAIN_AMPLITUDE   = 5;

    public static final float GRAVITY           = 20.0f;
    public static final float TERMINAL_VELOCITY = -50.0f;
    public static final float JUMP_IMPULSE      = 8.0f;
    public static final float PLAYER_EYE_HEIGHT = 1.6f;
}
