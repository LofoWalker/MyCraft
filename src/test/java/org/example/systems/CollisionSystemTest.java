package org.example.systems;

import org.example.components.*;
import org.example.ecs.Entity;
import org.example.ecs.World;
import org.example.world.WorldConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CollisionSystemTest {

    private World           world;
    private Entity          player;
    private CollisionSystem system;

    // Player size: 0.6 wide, 1.8 tall, 0.6 deep — same as Main
    private static final float W = 0.6f;
    private static final float H = 1.8f;
    private static final float D = 0.6f;

    @BeforeEach
    void setUp() {
        world  = new World();
        player = world.create();
        world.add(player, new ColliderAABB(W, H, D));
        system = new CollisionSystem();
    }

    // --- isSolid / chunkMap helpers ---

    private Map<Long, VoxelChunkData> solidFloorAt(int floorY) {
        VoxelChunkData data = VoxelChunkData.empty();
        int S = WorldConstants.CHUNK_SIZE;
        for (int x = 0; x < S; x++)
            for (int z = 0; z < S; z++)
                data.set(x, floorY, z, WorldConstants.BLOCK_GRASS);
        Map<Long, VoxelChunkData> map = new HashMap<>();
        map.put(CollisionSystem.chunkKey(0, 0), data);
        return map;
    }

    private Map<Long, VoxelChunkData> solidBlock(int bx, int by, int bz) {
        int S = WorldConstants.CHUNK_SIZE;
        int cx = Math.floorDiv(bx, S);
        int cz = Math.floorDiv(bz, S);
        int lx = bx - cx * S;
        int lz = bz - cz * S;
        VoxelChunkData data = VoxelChunkData.empty();
        data.set(lx, by, lz, WorldConstants.BLOCK_STONE);
        Map<Long, VoxelChunkData> map = new HashMap<>();
        map.put(CollisionSystem.chunkKey(cx, cz), data);
        return map;
    }

    // --- isSolid unit tests ---

    @Test
    void isSolidReturnsFalseForAir() {
        assertFalse(CollisionSystem.isSolid(0, 0, 0, new HashMap<>()));
    }

    @Test
    void isSolidReturnsFalseOutsideChunkHeight() {
        assertFalse(CollisionSystem.isSolid(0, -1, 0, new HashMap<>()));
        assertFalse(CollisionSystem.isSolid(0, WorldConstants.CHUNK_SIZE, 0, new HashMap<>()));
    }

    @Test
    void isSolidReturnsTrueForSolidBlock() {
        Map<Long, VoxelChunkData> map = solidBlock(3, 5, 3);
        assertTrue(CollisionSystem.isSolid(3, 5, 3, map));
    }

    // --- Full system: landing on floor ---

    @Test
    void playerLandsOnFloor() {
        // Floor at y=4; player falling with feet at y=4.1 → should be pushed to y=5
        world.add(player, new Position(8f, 4.1f, 8f));
        world.add(player, new Velocity(0f, -5f, 0f));

        Entity floorChunk = world.create();
        world.add(floorChunk, new ChunkComponent(0, 0));
        VoxelChunkData data = VoxelChunkData.empty();
        for (int x = 0; x < 16; x++)
            for (int z = 0; z < 16; z++)
                data.set(x, 4, z, WorldConstants.BLOCK_GRASS);
        world.add(floorChunk, data);

        system.update(world, 1f / 60f);

        Position pos = world.get(player, Position.class).orElseThrow();
        assertEquals(5f, pos.y(), 1e-3f);
    }

    @Test
    void playerIsGroundedAfterLanding() {
        world.add(player, new Position(8f, 5.0f, 8f));
        world.add(player, new Velocity(0f, -1f, 0f));

        Entity floorChunk = world.create();
        world.add(floorChunk, new ChunkComponent(0, 0));
        VoxelChunkData data = VoxelChunkData.empty();
        for (int x = 0; x < 16; x++)
            for (int z = 0; z < 16; z++)
                data.set(x, 4, z, WorldConstants.BLOCK_GRASS);
        world.add(floorChunk, data);

        system.update(world, 1f / 60f);

        assertTrue(world.has(player, Grounded.class));
    }

    @Test
    void velocityYZeroedOnLanding() {
        world.add(player, new Position(8f, 5.0f, 8f));
        world.add(player, new Velocity(0f, -5f, 0f));

        Entity floorChunk = world.create();
        world.add(floorChunk, new ChunkComponent(0, 0));
        VoxelChunkData data = VoxelChunkData.empty();
        for (int x = 0; x < 16; x++)
            for (int z = 0; z < 16; z++)
                data.set(x, 4, z, WorldConstants.BLOCK_GRASS);
        world.add(floorChunk, data);

        system.update(world, 1f / 60f);

        Velocity vel = world.get(player, Velocity.class).orElseThrow();
        assertEquals(0f, vel.y(), 1e-5f);
    }

    @Test
    void groundedRemovedWhenAirborne() {
        // Player is in mid-air (no solid block below) — Grounded must be absent
        world.add(player, new Grounded());
        world.add(player, new Position(8f, 10f, 8f));
        world.add(player, new Velocity(0f, -1f, 0f));
        // No chunk in world → no solid blocks

        system.update(world, 1f / 60f);

        assertFalse(world.has(player, Grounded.class));
    }

    @Test
    void playerBlockedByWallOnX() {
        // Wall block at x=9, player moving right with feet at (8.5, 5, 8)
        world.add(player, new Position(8.5f, 5f, 8f));
        world.add(player, new Velocity(10f, 0f, 0f));

        Entity wallChunk = world.create();
        world.add(wallChunk, new ChunkComponent(0, 0));
        VoxelChunkData data = VoxelChunkData.empty();
        // Fill column x=9 y=5..6
        data.set(9, 5, 8, WorldConstants.BLOCK_STONE);
        data.set(9, 6, 8, WorldConstants.BLOCK_STONE);
        world.add(wallChunk, data);

        system.update(world, 1f / 60f);

        Position pos = world.get(player, Position.class).orElseThrow();
        assertTrue(pos.x() + W / 2f <= 9f + 1e-3f, "Player should not penetrate wall");
    }

    @Test
    void noChunksNoCollision() {
        world.add(player, new Position(8f, 5f, 8f));
        world.add(player, new Velocity(0f, -5f, 0f));
        // No chunk entities → no blocks → player falls freely, position unchanged by collision
        system.update(world, 1f / 60f);
        assertFalse(world.has(player, Grounded.class));
    }
}
