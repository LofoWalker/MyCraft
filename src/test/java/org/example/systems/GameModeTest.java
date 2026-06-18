package org.example.systems;

import org.example.components.*;
import org.example.ecs.Entity;
import org.example.ecs.World;
import org.example.world.BlockType;
import org.example.world.Inventories;
import org.example.world.WorldConstants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests pure decision points for creative / survival mode differences:
 * - Creative: place does NOT decrement the stack, break is instant.
 * - Survival: place decrements the stack, break follows hardness.
 */
class GameModeTest {

    private static final float DT = 1f / 60f;

    // -------------------------------------------------------------------------
    // Creative mode: infinite resources — placing a block never shrinks the stack
    // -------------------------------------------------------------------------

    @Test
    void creative_placeDoesNotDecrementStack() {
        World world = new World();
        VoxelChunkData data = VoxelChunkData.empty();
        data.set(2, 60, 2, WorldConstants.BLOCK_STONE); // target block
        spawnChunk(world, data);
        Entity player = spawnPlayer(world, GameMode.Mode.CREATIVE);
        int initialCount = stackCountInSlot(world, player, 0);
        BlockInteractionSystem system = new BlockInteractionSystem();

        place(world, player, system);

        int afterCount = stackCountInSlot(world, player, 0);
        assertEquals(initialCount, afterCount, "Creative: stack must not decrease after placing");
    }

    // -------------------------------------------------------------------------
    // Creative mode: instant break — any block disappears in one hit regardless of hardness
    // -------------------------------------------------------------------------

    @Test
    void creative_breakIsInstantForHardBlock() {
        World world = new World();
        VoxelChunkData data = VoxelChunkData.empty();
        data.set(2, 60, 2, WorldConstants.BLOCK_STONE); // hardness > 1
        spawnChunk(world, data);
        Entity player = spawnPlayer(world, GameMode.Mode.CREATIVE);
        BlockInteractionSystem system = new BlockInteractionSystem();

        // A single click must break the block regardless of hardness.
        click(world, player, system);

        assertEquals(WorldConstants.BLOCK_AIR, data.get(2, 60, 2),
                "Creative: stone must be destroyed in a single hit");
        assertFalse(world.has(player, BlockBreakProgress.class),
                "Creative: no break progress component should remain");
    }

    // -------------------------------------------------------------------------
    // Survival mode: standard resource drain — placing decrements the stack
    // -------------------------------------------------------------------------

    @Test
    void survival_placeDecrementsStack() {
        World world = new World();
        VoxelChunkData data = VoxelChunkData.empty();
        data.set(2, 60, 2, WorldConstants.BLOCK_STONE);
        spawnChunk(world, data);
        Entity player = spawnPlayer(world, GameMode.Mode.SURVIVAL);
        int initialCount = stackCountInSlot(world, player, 0);
        BlockInteractionSystem system = new BlockInteractionSystem();

        place(world, player, system);

        int afterCount = stackCountInSlot(world, player, 0);
        assertEquals(initialCount - 1, afterCount,
                "Survival: stack must decrease by 1 after placing");
    }

    // -------------------------------------------------------------------------
    // Survival mode: hardness governs break — stone needs multiple hits
    // -------------------------------------------------------------------------

    @Test
    void survival_breakFollowsHardness() {
        World world = new World();
        VoxelChunkData data = VoxelChunkData.empty();
        data.set(2, 60, 2, WorldConstants.BLOCK_STONE);
        spawnChunk(world, data);
        Entity player = spawnPlayer(world, GameMode.Mode.SURVIVAL);
        BlockInteractionSystem system = new BlockInteractionSystem();

        int hardness = BlockType.STONE.hardness();
        // All hits but the last must leave the block standing.
        for (int i = 0; i < hardness - 1; i++) click(world, player, system);
        assertEquals(WorldConstants.BLOCK_STONE, data.get(2, 60, 2),
                "Survival: block must survive until enough hits");

        click(world, player, system); // final hit
        assertEquals(WorldConstants.BLOCK_AIR, data.get(2, 60, 2),
                "Survival: block must be broken after enough hits");
    }

    // -------------------------------------------------------------------------
    // HealthSystem: creative skips all damage
    // -------------------------------------------------------------------------

    @Test
    void creative_healthSystemSkipsDamage() {
        World world = new World();
        Entity player = world.create();
        world.add(player, new Health(WorldConstants.MAX_HEALTH, WorldConstants.MAX_HEALTH));
        world.add(player, new Position(8, 200, 8));
        // High falling speed that would normally deal fall damage
        world.add(player, new Velocity(0f, -30f, 0f));
        world.add(player, new FallSpeed(30f));
        world.add(player, new Grounded());
        world.add(player, new DamageImmunity(0f));
        world.add(player, new DamageTimers(0f, 0f, 0f));
        world.add(player, new Breath(WorldConstants.BREATH_SECONDS));
        world.add(player, new GameMode(GameMode.Mode.CREATIVE));

        new HealthSystem().update(world, DT);

        int hp = world.get(player, Health.class).orElseThrow().current();
        assertEquals(WorldConstants.MAX_HEALTH, hp,
                "Creative: fall damage must be skipped entirely");
    }

    @Test
    void survival_healthSystemAppliesFallDamage() {
        World world = new World();
        Entity player = world.create();
        world.add(player, new Health(WorldConstants.MAX_HEALTH, WorldConstants.MAX_HEALTH));
        world.add(player, new Position(8, 200, 8));
        world.add(player, new Velocity(0f, -30f, 0f));
        world.add(player, new FallSpeed(30f));   // > SAFE_FALL_SPEED -> should take damage
        world.add(player, new Grounded());
        world.add(player, new DamageImmunity(0f));
        world.add(player, new DamageTimers(0f, 0f, 0f));
        world.add(player, new Breath(WorldConstants.BREATH_SECONDS));
        world.add(player, new GameMode(GameMode.Mode.SURVIVAL));

        new HealthSystem().update(world, DT);

        int hp = world.get(player, Health.class).orElseThrow().current();
        assertTrue(hp < WorldConstants.MAX_HEALTH,
                "Survival: fall damage must reduce health");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static void place(World world, Entity player, BlockInteractionSystem system) {
        world.add(player, new PlayerInput(false, false, false, false, false, false,
                0f, 0f, false, true, false, 0, WorldConstants.NO_HOTBAR_SELECT, false));
        system.update(world, DT);
        world.add(player, new PlayerInput(false, false, false, false, false, false,
                0f, 0f, false, false, false, 0, WorldConstants.NO_HOTBAR_SELECT, false));
        system.update(world, DT);
    }

    private static void click(World world, Entity player, BlockInteractionSystem system) {
        world.add(player, new PlayerInput(false, false, false, false, false, false,
                0f, 0f, true, false, false, 0, WorldConstants.NO_HOTBAR_SELECT, false));
        system.update(world, DT);
        world.add(player, new PlayerInput(false, false, false, false, false, false,
                0f, 0f, false, false, false, 0, WorldConstants.NO_HOTBAR_SELECT, false));
        system.update(world, DT);
    }

    private static Entity spawnChunk(World world, VoxelChunkData data) {
        Entity chunk = world.create();
        world.add(chunk, new ChunkComponent(0, 0));
        world.add(chunk, data);
        return chunk;
    }

    private static Entity spawnPlayer(World world, GameMode.Mode mode) {
        Entity player = world.create();
        float eyeOffset = WorldConstants.PLAYER_EYE_HEIGHT;
        world.add(player, new Position(2.5f, 60.5f - eyeOffset, 6.5f));
        world.add(player, new Rotation(0f, 0f)); // looking along -z
        world.add(player, new ColliderAABB(0.6f, 1.8f, 0.6f));
        world.add(player, new CameraComponent(70f, 0.1f, 1000f));
        world.add(player, new PlayerInput(false, false, false, false, false, false,
                0f, 0f, false, false, false, 0, WorldConstants.NO_HOTBAR_SELECT, false));
        world.add(player, new Hotbar(0));
        Inventory inventory = Inventories.add(Inventories.empty(),
                new ItemStack(WorldConstants.BLOCK_STONE, WorldConstants.MAX_STACK)).inventory();
        world.add(player, inventory);
        world.add(player, new GameMode(mode));
        return player;
    }

    private static int stackCountInSlot(World world, Entity player, int slot) {
        return world.get(player, Inventory.class)
                .map(inv -> Inventories.get(inv, slot).count())
                .orElse(0);
    }
}
