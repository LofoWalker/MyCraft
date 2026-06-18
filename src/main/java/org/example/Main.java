package org.example;

import org.example.components.*;
import org.example.ecs.Entity;
import org.example.ecs.SystemScheduler;
import org.example.ecs.World;
import org.example.io.WorldStorage;
import org.example.io.WorldStorage.LevelData;
import org.example.render.Shader;
import org.example.render.TextureAtlas;
import org.example.systems.*;
import org.example.components.MobType;
import org.example.world.Inventories;
import org.example.world.Mobs;
import org.example.world.WorldConstants;
import org.example.worldgen.TerrainShape;

import java.util.concurrent.ThreadLocalRandom;

public class Main {

    private static final int    SPAWN_X    = 8;
    private static final int    SPAWN_Z    = 8;
    private static final String WORLD_NAME = "world";

    static void main(String[] args) {
        try (Window window = new Window(1920, 1080, "MyCraft")) {
            window.init();
            run(window);
        }
    }

    private static void run(Window window) {
        World world = new World();
        SystemScheduler simScheduler    = new SystemScheduler();
        SystemScheduler renderScheduler = new SystemScheduler();

        WorldStorage storage = WorldStorage.forWorld(WORLD_NAME);
        long seed;
        Entity player = world.create();

        if (storage.levelExists()) {
            LevelData level = storage.readLevel();
            seed = level.seed();
            System.out.println("Loaded world seed: " + seed);
            restorePlayer(world, player, level);
        } else {
            seed = ThreadLocalRandom.current().nextLong();
            System.out.println("New world seed: " + seed);
            initNewPlayer(world, player, seed);
            storage.writeLevel(snapshotLevel(world, player, seed));
        }

        Entity worldClock = world.create();
        world.add(worldClock, new TimeOfDay(0f));

        // Test mob: a cow spawned a few blocks away from the player, falls to ground under gravity.
        float mobSpawnY = world.get(player, Position.class).map(Position::y).orElse((float) spawnHeight(seed));
        Mobs.spawn(world, MobType.Kind.COW, SPAWN_X + 3, mobSpawnY, SPAWN_Z + 3);

        window.captureCursor();

        try (Shader shader = Shader.fromResources("/shaders/basic.vert", "/shaders/basic.frag");
             Shader waterShader = Shader.fromResources("/shaders/water.vert", "/shaders/water.frag");
             TextureAtlas atlas = TextureAtlas.loadFromClasspath("/textures/blocks.png");
             ChunkStreamingSystem chunkStreaming = new ChunkStreamingSystem(seed, storage);
             SkySystem sky = new SkySystem();
             BlockHighlightSystem blockHighlight = new BlockHighlightSystem();
             BlockBreakOverlaySystem breakOverlay = new BlockBreakOverlaySystem();
             ItemRenderSystem itemRender = new ItemRenderSystem();
             HudSystem hud = new HudSystem(window);
             EntityRenderSystem entityRender = new EntityRenderSystem();
             InventoryScreenSystem inventoryScreen = new InventoryScreenSystem(window)) {

            simScheduler.add(new TimeSystem());
            simScheduler.add(new InputSystem(window));
            simScheduler.add(new HotbarSelectionSystem());
            simScheduler.add(new BlockInteractionSystem());
            simScheduler.add(new FluidSystem());
            simScheduler.add(new FlightControlSystem());
            simScheduler.add(new PhysicsSystem());
            simScheduler.add(new MovementSystem());
            simScheduler.add(new ItemMotionSystem());
            // Before CollisionSystem: it captures fall-impact Velocity.y before collision zeroes it.
            simScheduler.add(new HealthSystem());
            // After HealthSystem: hunger-funded regen/starvation reads this tick's settled health and
            // the post-damage delay timer; HungerSystem (not HealthSystem) owns all health regen.
            simScheduler.add(new HungerSystem());
            simScheduler.add(new CollisionSystem());
            simScheduler.add(new ItemPickupSystem());
            simScheduler.add(new MobAiSystem());
            simScheduler.add(new PassiveSpawnSystem());

            renderScheduler.add(new CameraSystem(window.getAspectRatio()));
            renderScheduler.add(sky);
            renderScheduler.add(chunkStreaming);
            renderScheduler.add(new RenderSystem(shader, waterShader, atlas));
            renderScheduler.add(blockHighlight);
            renderScheduler.add(breakOverlay);
            renderScheduler.add(itemRender);
            renderScheduler.add(entityRender);
            renderScheduler.add(hud);
            // Inventory screen overlays everything else; runs last in the render schedule.
            renderScheduler.add(inventoryScreen);

            GameLoop.run(window, world, simScheduler, renderScheduler);

            // Clean shutdown: flush modified chunks then persist the player's final state.
            chunkStreaming.flushModifiedChunks(world);
            storage.writeLevel(snapshotLevel(world, player, seed));
        }
    }

    // -------------------------------------------------------------------------
    // Player initialisation and restore
    // -------------------------------------------------------------------------

    private static void initNewPlayer(World world, Entity player, long seed) {
        float spawnY = spawnHeight(seed);
        world.add(player, new Position(SPAWN_X, spawnY, SPAWN_Z));
        world.add(player, new Rotation(-30f, -20f));
        addSharedComponents(world, player);
        world.add(player, new SpawnPoint(SPAWN_X, spawnY, SPAWN_Z));
    }

    private static void restorePlayer(World world, Entity player, LevelData level) {
        world.add(player, level.position());
        world.add(player, level.rotation());
        addSharedComponents(world, player);
        Position p = level.position();
        world.add(player, new SpawnPoint(p.x(), p.y(), p.z()));
        // Overwrite the default health/hunger/inventory/hotbar with the saved values.
        world.add(player, level.health());
        world.add(player, level.hunger());
        world.add(player, level.inventory());
        world.add(player, level.hotbar());
    }

    // Components that are always created fresh (physics state, timers, etc.) even on load.
    private static void addSharedComponents(World world, Entity player) {
        world.add(player, new Velocity(0f, 0f, 0f));
        world.add(player, new Gravity(WorldConstants.GRAVITY));
        world.add(player, new ColliderAABB(0.6f, 1.8f, 0.6f));
        world.add(player, new CameraComponent(70f, 0.1f, 1000f));
        world.add(player, new PlayerInput(false, false, false, false, false, false, 0f, 0f, false, false,
                false, 0, WorldConstants.NO_HOTBAR_SELECT, false));
        world.add(player, new Hotbar(0));
        world.add(player, startingInventory());
        world.add(player, new Health(WorldConstants.MAX_HEALTH, WorldConstants.MAX_HEALTH));
        world.add(player, new Hunger(WorldConstants.MAX_FOOD, WorldConstants.MAX_FOOD));
        world.add(player, new HungerTimers(0f, 0f, 0f));
        world.add(player, new Breath(WorldConstants.BREATH_SECONDS));
        world.add(player, new DamageImmunity(0f));
        world.add(player, new DamageTimers(0f, 0f, 0f));
    }

    // Capture a consistent snapshot of the player for level.dat. Called on clean exit.
    private static LevelData snapshotLevel(World world, Entity player, long seed) {
        Position pos    = world.get(player, Position.class)
                               .orElse(new Position(SPAWN_X, 64, SPAWN_Z));
        Rotation rot    = world.get(player, Rotation.class).orElse(new Rotation(0f, 0f));
        Health   health = world.get(player, Health.class)
                               .orElse(new Health(WorldConstants.MAX_HEALTH, WorldConstants.MAX_HEALTH));
        Hunger   hunger = world.get(player, Hunger.class)
                               .orElse(new Hunger(WorldConstants.MAX_FOOD, WorldConstants.MAX_FOOD));
        TimeOfDay tod   = new TimeOfDay(0f);
        Hotbar   hotbar = world.get(player, Hotbar.class).orElse(new Hotbar(0));
        Inventory inv   = world.get(player, Inventory.class).orElse(startingInventory());
        return LevelData.of(seed, pos, rot, health, hunger, tod, hotbar, inv);
    }

    // A few full stacks of placeable blocks so the player can build straight away. They land in the
    // first hotbar slots, so slot 0 (the default selection) holds stone.
    private static Inventory startingInventory() {
        Inventory inventory = Inventories.empty();
        inventory = Inventories.add(inventory, new ItemStack(WorldConstants.BLOCK_STONE, WorldConstants.MAX_STACK)).inventory();
        inventory = Inventories.add(inventory, new ItemStack(WorldConstants.BLOCK_DIRT,  WorldConstants.MAX_STACK)).inventory();
        inventory = Inventories.add(inventory, new ItemStack(WorldConstants.BLOCK_WOOD,  WorldConstants.MAX_STACK)).inventory();
        inventory = Inventories.add(inventory, new ItemStack(WorldConstants.BLOCK_TORCH, WorldConstants.MAX_STACK)).inventory();
        // A stack of food (non-block id) so eating (key F) is testable in-game; it is NOT placeable.
        inventory = Inventories.add(inventory, new ItemStack(WorldConstants.ITEM_BREAD, 8)).inventory();
        return inventory;
    }

    // Drop the player just above the real surface at the spawn column so they land on solid ground
    // instead of falling through the void or spawning inside terrain.
    private static float spawnHeight(long seed) {
        int surfaceY = new TerrainShape(seed).surfaceY(SPAWN_X, SPAWN_Z);
        return surfaceY + WorldConstants.PLAYER_SPAWN_CLEARANCE;
    }
}
