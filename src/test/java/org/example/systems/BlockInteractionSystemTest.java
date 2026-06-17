package org.example.systems;

import org.example.components.BlockBreakProgress;
import org.example.components.CameraComponent;
import org.example.components.ChunkComponent;
import org.example.components.ChunkDirty;
import org.example.components.PlayerInput;
import org.example.components.Position;
import org.example.components.Rotation;
import org.example.components.TargetedBlock;
import org.example.components.VoxelChunkData;
import org.example.ecs.Entity;
import org.example.ecs.World;
import org.example.world.BlockType;
import org.example.world.WorldConstants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BlockInteractionSystemTest {

    private static final float DT = 1f / 60f;

    @Test
    void targetsTheBlockBeingLookedAt() {
        World world = new World();
        VoxelChunkData data = VoxelChunkData.empty();
        data.set(2, 60, 2, WorldConstants.BLOCK_STONE);
        spawnChunk(world, data);
        Entity player = spawnPlayer(world);
        BlockInteractionSystem system = new BlockInteractionSystem();

        system.update(world, DT);

        TargetedBlock target = world.get(player, TargetedBlock.class).orElseThrow();
        assertEquals(2, target.x());
        assertEquals(60, target.y());
        assertEquals(2, target.z());
    }

    @Test
    void clearsTargetWhenLookingAtNothing() {
        World world = new World();
        VoxelChunkData data = VoxelChunkData.empty(); // no solid block in range
        spawnChunk(world, data);
        Entity player = spawnPlayer(world);
        BlockInteractionSystem system = new BlockInteractionSystem();

        system.update(world, DT);

        assertFalse(world.has(player, TargetedBlock.class));
    }

    @Test
    void oneHitBlockBreaksInASingleClickAndFlagsChunk() {
        World world = new World();
        VoxelChunkData data = VoxelChunkData.empty();
        data.set(2, 60, 2, WorldConstants.BLOCK_LEAVES); // hardness 1
        Entity chunk  = spawnChunk(world, data);
        Entity player = spawnPlayer(world);
        BlockInteractionSystem system = new BlockInteractionSystem();

        click(world, player, system);

        assertEquals(WorldConstants.BLOCK_AIR, data.get(2, 60, 2));
        assertTrue(world.has(chunk, ChunkDirty.class));
    }

    @Test
    void hardBlockSurvivesUntilEnoughHits() {
        World world = new World();
        VoxelChunkData data = VoxelChunkData.empty();
        data.set(2, 60, 2, WorldConstants.BLOCK_STONE);
        spawnChunk(world, data);
        Entity player = spawnPlayer(world);
        BlockInteractionSystem system = new BlockInteractionSystem();

        int hardness = BlockType.STONE.hardness();
        for (int i = 0; i < hardness - 1; i++) click(world, player, system);

        assertEquals(WorldConstants.BLOCK_STONE, data.get(2, 60, 2));
        BlockBreakProgress progress = world.get(player, BlockBreakProgress.class).orElseThrow();
        assertTrue(progress.targets(2, 60, 2));
        assertEquals(hardness - 1, progress.damage());

        click(world, player, system); // final hit
        assertEquals(WorldConstants.BLOCK_AIR, data.get(2, 60, 2));
        assertFalse(world.has(player, BlockBreakProgress.class));
    }

    @Test
    void heldButtonDealsOnlyOneHitPerPress() {
        World world = new World();
        VoxelChunkData data = VoxelChunkData.empty();
        data.set(2, 60, 2, WorldConstants.BLOCK_LEAVES);
        data.set(2, 60, 1, WorldConstants.BLOCK_LEAVES);
        spawnChunk(world, data);
        Entity player = spawnPlayer(world);
        BlockInteractionSystem system = new BlockInteractionSystem();

        setBreaking(world, player, true);
        system.update(world, DT); // breaks the nearest block
        system.update(world, DT); // button still held -> no second hit

        assertEquals(WorldConstants.BLOCK_AIR,    data.get(2, 60, 2));
        assertEquals(WorldConstants.BLOCK_LEAVES, data.get(2, 60, 1));
    }

    @Test
    void switchingTargetResetsProgress() {
        World world = new World();
        VoxelChunkData data = VoxelChunkData.empty();
        data.set(2, 60, 2, WorldConstants.BLOCK_STONE); // block A
        data.set(3, 60, 2, WorldConstants.BLOCK_STONE); // block B
        spawnChunk(world, data);
        Entity player = spawnPlayer(world);
        BlockInteractionSystem system = new BlockInteractionSystem();

        aimX(world, player, 2.5f);
        for (int i = 0; i < BlockType.STONE.hardness() - 1; i++) click(world, player, system); // A almost broken

        aimX(world, player, 3.5f);
        click(world, player, system); // switch to B -> A's progress is abandoned

        aimX(world, player, 2.5f);
        click(world, player, system); // back to A -> starts over

        assertEquals(WorldConstants.BLOCK_STONE, data.get(2, 60, 2));
        assertEquals(WorldConstants.BLOCK_STONE, data.get(3, 60, 2));
    }

    private static void click(World world, Entity player, BlockInteractionSystem system) {
        setBreaking(world, player, true);
        system.update(world, DT);
        setBreaking(world, player, false);
        system.update(world, DT);
    }

    private static void setBreaking(World world, Entity player, boolean breaking) {
        world.add(player, new PlayerInput(false, false, false, false, false, false, 0f, 0f, breaking));
    }

    private static void aimX(World world, Entity player, float x) {
        Position p = world.get(player, Position.class).orElseThrow();
        world.add(player, new Position(x, p.y(), p.z()));
    }

    private static Entity spawnChunk(World world, VoxelChunkData data) {
        Entity chunk = world.create();
        world.add(chunk, new ChunkComponent(0, 0));
        world.add(chunk, data);
        return chunk;
    }

    private static Entity spawnPlayer(World world) {
        Entity player = world.create();
        float eyeOffset = WorldConstants.PLAYER_EYE_HEIGHT;
        world.add(player, new Position(2.5f, 60.5f - eyeOffset, 6.5f));
        world.add(player, new Rotation(0f, 0f)); // looking straight along -z
        world.add(player, new CameraComponent(70f, 0.1f, 1000f));
        world.add(player, new PlayerInput(false, false, false, false, false, false, 0f, 0f, false));
        return player;
    }
}
