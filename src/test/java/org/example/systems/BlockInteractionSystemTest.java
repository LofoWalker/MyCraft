package org.example.systems;

import org.example.components.CameraComponent;
import org.example.components.ChunkComponent;
import org.example.components.ChunkDirty;
import org.example.components.PlayerInput;
import org.example.components.Position;
import org.example.components.Rotation;
import org.example.components.VoxelChunkData;
import org.example.ecs.Entity;
import org.example.ecs.World;
import org.example.world.WorldConstants;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BlockInteractionSystemTest {

    private static Map<Long, VoxelChunkData> oneChunk(VoxelChunkData data) {
        Map<Long, VoxelChunkData> map = new HashMap<>();
        map.put(CollisionSystem.chunkKey(0, 0), data);
        return map;
    }

    @Test
    void raycastHitsSolidBlockAhead() {
        VoxelChunkData data = VoxelChunkData.empty();
        data.set(2, 60, 2, WorldConstants.BLOCK_STONE);

        int[] hit = BlockInteractionSystem.raycastSolid(
                2.5f, 60.5f, 6.5f, 0f, 0f, -1f, WorldConstants.PLAYER_REACH, oneChunk(data));

        assertArrayEquals(new int[]{2, 60, 2}, hit);
    }

    @Test
    void raycastReturnsNullWhenNothingInReach() {
        VoxelChunkData data = VoxelChunkData.empty();
        data.set(2, 60, 2, WorldConstants.BLOCK_STONE);

        int[] hit = BlockInteractionSystem.raycastSolid(
                2.5f, 60.5f, 30.5f, 0f, 0f, -1f, WorldConstants.PLAYER_REACH, oneChunk(data));

        assertNull(hit);
    }

    @Test
    void breakingClearsTargetedVoxelAndFlagsChunk() {
        World world = new World();
        VoxelChunkData data = VoxelChunkData.empty();
        data.set(2, 60, 2, WorldConstants.BLOCK_STONE);
        Entity chunk = spawnChunk(world, data);
        Entity player = spawnPlayer(world, true);

        new BlockInteractionSystem().update(world, 1f / 60f);

        assertEquals(WorldConstants.BLOCK_AIR, data.get(2, 60, 2));
        assertTrue(world.has(chunk, ChunkDirty.class));
        assertNotNull(player); // player drives the interaction
    }

    @Test
    void heldButtonBreaksOnlyOneBlockPerPress() {
        World world = new World();
        VoxelChunkData data = VoxelChunkData.empty();
        data.set(2, 60, 2, WorldConstants.BLOCK_STONE);
        data.set(2, 60, 1, WorldConstants.BLOCK_STONE);
        spawnChunk(world, data);
        spawnPlayer(world, true);

        BlockInteractionSystem system = new BlockInteractionSystem();
        system.update(world, 1f / 60f); // breaks the nearest block
        system.update(world, 1f / 60f); // button still held -> no second break

        assertEquals(WorldConstants.BLOCK_AIR,   data.get(2, 60, 2));
        assertEquals(WorldConstants.BLOCK_STONE, data.get(2, 60, 1));
    }

    private static Entity spawnChunk(World world, VoxelChunkData data) {
        Entity chunk = world.create();
        world.add(chunk, new ChunkComponent(0, 0));
        world.add(chunk, data);
        return chunk;
    }

    private static Entity spawnPlayer(World world, boolean breaking) {
        Entity player = world.create();
        float eyeOffset = WorldConstants.PLAYER_EYE_HEIGHT;
        world.add(player, new Position(2.5f, 60.5f - eyeOffset, 6.5f));
        world.add(player, new Rotation(0f, 0f)); // looking straight along -z
        world.add(player, new CameraComponent(70f, 0.1f, 1000f));
        world.add(player, new PlayerInput(false, false, false, false, false, false, 0f, 0f, breaking));
        return player;
    }
}
