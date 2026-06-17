package org.example.systems;

import org.example.components.BlockBreakProgress;
import org.example.components.CameraComponent;
import org.example.components.ColliderAABB;
import org.example.components.ChunkComponent;
import org.example.components.ChunkDirty;
import org.example.components.Hotbar;
import org.example.components.Inventory;
import org.example.components.ItemStack;
import org.example.components.PlayerInput;
import org.example.components.Position;
import org.example.components.Rotation;
import org.example.components.TargetedBlock;
import org.example.components.VoxelChunkData;
import org.example.ecs.Entity;
import org.example.ecs.World;
import org.example.world.BlockType;
import org.example.world.Inventories;
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

    @Test
    void rightClickPlacesBlockAgainstTargetedFace() {
        World world = new World();
        VoxelChunkData data = VoxelChunkData.empty();
        data.set(2, 60, 2, WorldConstants.BLOCK_STONE);
        Entity chunk  = spawnChunk(world, data);
        Entity player = spawnPlayer(world);
        BlockInteractionSystem system = new BlockInteractionSystem();

        place(world, player, system); // looking -z, enters block from +z face -> cell (2,60,3)

        assertEquals(WorldConstants.BLOCK_STONE, data.get(2, 60, 3));
        assertTrue(world.has(chunk, ChunkDirty.class));
    }

    @Test
    void placementRefusedWhenCellNotAir() {
        World world = new World();
        VoxelChunkData data = VoxelChunkData.empty();
        data.set(2, 60, 2, WorldConstants.BLOCK_STONE); // targeted
        data.set(2, 60, 3, WorldConstants.BLOCK_DIRT);  // adjacent cell already filled
        spawnChunk(world, data);
        Entity player = spawnPlayer(world);
        BlockInteractionSystem system = new BlockInteractionSystem();

        place(world, player, system);

        assertEquals(WorldConstants.BLOCK_DIRT, data.get(2, 60, 3)); // unchanged
    }

    @Test
    void placementRefusedWhenItWouldTrapThePlayer() {
        World world = new World();
        VoxelChunkData data = VoxelChunkData.empty();
        Entity player = spawnPlayer(world);
        Position p = world.get(player, Position.class).orElseThrow();
        // Stand on the chunk and target a block one cell below the feet, so the face above it is the
        // very cell the player occupies.
        int feetX = (int) Math.floor(p.x());
        int feetY = (int) Math.floor(p.y());
        int feetZ = (int) Math.floor(p.z());
        data.set(feetX, feetY - 1, feetZ, WorldConstants.BLOCK_STONE);
        spawnChunk(world, data);
        aimStraightDown(world, player);
        BlockInteractionSystem system = new BlockInteractionSystem();

        place(world, player, system); // would place at feetY (inside the player) -> refused

        assertEquals(WorldConstants.BLOCK_AIR, data.get(feetX, feetY, feetZ));
    }

    @Test
    void heldRightButtonPlacesOnlyOnePerPress() {
        World world = new World();
        VoxelChunkData data = VoxelChunkData.empty();
        data.set(2, 60, 2, WorldConstants.BLOCK_STONE);
        spawnChunk(world, data);
        Entity player = spawnPlayer(world);
        BlockInteractionSystem system = new BlockInteractionSystem();

        setPlacing(world, player, true);
        system.update(world, DT); // places at (2,60,3)
        system.update(world, DT); // button still held -> edge-trigger blocks a second placement

        assertEquals(WorldConstants.BLOCK_STONE, data.get(2, 60, 3));
        assertEquals(WorldConstants.BLOCK_AIR,   data.get(2, 60, 4));
    }

    private static void place(World world, Entity player, BlockInteractionSystem system) {
        setPlacing(world, player, true);
        system.update(world, DT);
        setPlacing(world, player, false);
        system.update(world, DT);
    }

    private static void setPlacing(World world, Entity player, boolean placing) {
        world.add(player, new PlayerInput(false, false, false, false, false, false, 0f, 0f, false, placing,
                0, WorldConstants.NO_HOTBAR_SELECT));
    }

    private static void aimStraightDown(World world, Entity player) {
        world.add(player, new Rotation(0f, -90f));
    }

    private static void click(World world, Entity player, BlockInteractionSystem system) {
        setBreaking(world, player, true);
        system.update(world, DT);
        setBreaking(world, player, false);
        system.update(world, DT);
    }

    private static void setBreaking(World world, Entity player, boolean breaking) {
        world.add(player, new PlayerInput(false, false, false, false, false, false, 0f, 0f, breaking, false,
                0, WorldConstants.NO_HOTBAR_SELECT));
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
        world.add(player, new ColliderAABB(0.6f, 1.8f, 0.6f));
        world.add(player, new CameraComponent(70f, 0.1f, 1000f));
        world.add(player, new PlayerInput(false, false, false, false, false, false, 0f, 0f, false, false,
                0, WorldConstants.NO_HOTBAR_SELECT));
        world.add(player, new Hotbar(0));
        Inventory inventory = Inventories.add(Inventories.empty(),
                new ItemStack(WorldConstants.BLOCK_STONE, WorldConstants.MAX_STACK)).inventory();
        world.add(player, inventory);
        return player;
    }
}
