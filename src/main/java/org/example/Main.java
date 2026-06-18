package org.example;

import org.example.audio.SoundEngine;
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

    static void main(String[] args) {
        try (Window window = new Window(1920, 1080, "MyCraft")) {
            window.init();
            run(window);
        }
    }

    private static void run(Window window) {
        AppStateHolder stateHolder = new AppStateHolder(AppState.MAIN_MENU);

        // These are populated once the player starts a world.
        World[]            worldRef   = { null };
        SystemScheduler[]  simRef     = { null };
        SystemScheduler[]  renderRef  = { null };
        Entity[]           playerRef  = { null };
        WorldStorage[]     storageRef = { null };
        long[]             seedRef    = { 0L };

        // Resources that need to be closed are tracked here and closed on exit.
        AutoCloseable[] gameResources = { null };

        window.releaseCursor();

        // One OpenAL device for the whole run (null on headless machines); closed at exit.
        SoundEngine soundEngine = tryCreateSoundEngine();

        SystemScheduler menuScheduler  = new SystemScheduler();
        SystemScheduler pauseScheduler = new SystemScheduler();

        // The callback is invoked by MainMenuSystem when the player picks a world.
        MainMenuSystem.WorldSetupCallback onWorldReady = (worldName, mode) -> {
            // Clean up any previous session
            closeQuietly(gameResources[0]);
            gameResources[0] = null;

            WorldStorage storage = WorldStorage.forWorld(worldName);
            storageRef[0]        = storage;

            World world   = new World();
            Entity player = world.create();
            worldRef[0]   = world;
            playerRef[0]  = player;

            long seed;
            GameMode gameMode = new GameMode(mode);

            if (storage.levelExists()) {
                LevelData level = storage.readLevel();
                seed = level.seed();
                // Honour the persisted mode over the menu selection (load-world path).
                gameMode = level.gameMode();
                restorePlayer(world, player, level);
            } else {
                seed = ThreadLocalRandom.current().nextLong();
                initNewPlayer(world, player, seed, gameMode);
                storage.writeLevel(snapshotLevel(world, player, seed, gameMode));
            }
            seedRef[0] = seed;

            // Always overwrite the GameMode component to reflect what was loaded/chosen.
            world.add(player, gameMode);

            Entity worldClock = world.create();
            world.add(worldClock, new TimeOfDay(0f));

            float mobSpawnY = world.get(player, Position.class).map(Position::y)
                    .orElse((float) spawnHeight(seed));
            Mobs.spawn(world, MobType.Kind.COW, SPAWN_X + 3, mobSpawnY, SPAWN_Z + 3);

            SystemScheduler sim    = new SystemScheduler();
            SystemScheduler render = new SystemScheduler();
            simRef[0]    = sim;
            renderRef[0] = render;

            try {
                Shader         shader     = Shader.fromResources("/shaders/basic.vert", "/shaders/basic.frag");
                Shader         waterShader= Shader.fromResources("/shaders/water.vert", "/shaders/water.frag");
                TextureAtlas   atlas      = TextureAtlas.loadFromClasspath("/textures/blocks.png");
                ChunkStreamingSystem chunkStreaming = new ChunkStreamingSystem(seed, storage);
                SkySystem             sky           = new SkySystem();
                BlockHighlightSystem  blockHighlight= new BlockHighlightSystem();
                BlockBreakOverlaySystem breakOverlay= new BlockBreakOverlaySystem();
                ItemRenderSystem      itemRender    = new ItemRenderSystem();
                HudSystem             hud           = new HudSystem(window);
                EntityRenderSystem    entityRender  = new EntityRenderSystem();
                InventoryScreenSystem inventoryScreen = new InventoryScreenSystem(window);

                gameResources[0] = () -> {
                    chunkStreaming.flushModifiedChunks(worldRef[0]);
                    GameMode gm = worldRef[0].get(playerRef[0], GameMode.class)
                            .orElse(new GameMode(GameMode.Mode.SURVIVAL));
                    storageRef[0].writeLevel(snapshotLevel(worldRef[0], playerRef[0], seedRef[0], gm));
                    chunkStreaming.close();
                    sky.close();
                    blockHighlight.close();
                    breakOverlay.close();
                    itemRender.close();
                    hud.close();
                    entityRender.close();
                    inventoryScreen.close();
                    shader.close();
                    waterShader.close();
                    atlas.close();
                };

                sim.add(new TimeSystem());
                sim.add(new InputSystem(window, stateHolder));
                sim.add(new HotbarSelectionSystem());
                // Combat runs before block interaction: a click on a mob strikes it; otherwise the
                // click falls through to block breaking.
                sim.add(new PlayerCombatSystem());
                sim.add(new BlockInteractionSystem());
                sim.add(new FluidSystem());
                sim.add(new BlockGravitySystem());
                sim.add(new RandomTickSystem());
                sim.add(new FlightControlSystem());
                sim.add(new PhysicsSystem());
                sim.add(new MovementSystem());
                sim.add(new ItemMotionSystem());
                sim.add(new HealthSystem());
                sim.add(new HungerSystem());
                sim.add(new CollisionSystem());
                sim.add(new ItemPickupSystem());
                sim.add(new MobAiSystem());
                sim.add(new PassiveSpawnSystem());
                sim.add(new HostileSpawnSystem());

                render.add(new CameraSystem(window.getAspectRatio()));
                render.add(sky);
                render.add(chunkStreaming);
                render.add(new RenderSystem(shader, waterShader, atlas));
                render.add(blockHighlight);
                render.add(breakOverlay);
                render.add(itemRender);
                render.add(entityRender);
                render.add(hud);
                // Inventory screen overlays everything else; runs last in the render schedule.
                render.add(inventoryScreen);
                if (soundEngine != null) {
                    render.add(new AudioSystem(soundEngine));
                }

            } catch (Exception e) {
                throw new RuntimeException("Failed to initialise game resources", e);
            }

            window.captureCursor();
            stateHolder.request(AppState.IN_GAME);
        };

        MainMenuSystem mainMenu = new MainMenuSystem(window, stateHolder, onWorldReady);
        menuScheduler.add(mainMenu);

        // Pause menu: "Resume" or "Save & Quit"
        Runnable saveAndQuit = () -> {
            if (worldRef[0] != null && storageRef[0] != null) {
                GameMode gm = worldRef[0].get(playerRef[0], GameMode.class)
                        .orElse(new GameMode(GameMode.Mode.SURVIVAL));
                storageRef[0].writeLevel(snapshotLevel(worldRef[0], playerRef[0], seedRef[0], gm));
            }
            closeQuietly(gameResources[0]);
            gameResources[0] = null;
            worldRef[0]  = null;
            playerRef[0] = null;
            window.releaseCursor();
            stateHolder.request(AppState.MAIN_MENU);
        };
        PauseMenuSystem pauseMenu = new PauseMenuSystem(window, stateHolder, saveAndQuit);
        pauseScheduler.add(pauseMenu);

        // A proxy render scheduler that always forwards to the current render scheduler.
        // This lets GameLoop hold a single stable reference while we swap out game schedulers.
        SystemScheduler renderProxy = new SystemScheduler();
        renderProxy.add((world2, dt) -> {
            if (renderRef[0] != null && worldRef[0] != null) {
                renderRef[0].update(worldRef[0], dt);
            }
        });

        // Sim proxy similarly
        SystemScheduler simProxy = new SystemScheduler();
        simProxy.add((world2, dt) -> {
            if (simRef[0] != null && worldRef[0] != null) {
                simRef[0].update(worldRef[0], dt);
            }
        });

        // Use a dummy world as the top-level world; actual ECS ops go through worldRef.
        World dummyWorld = new World();

        try (mainMenu; pauseMenu) {
            GameLoop.run(window, dummyWorld, stateHolder,
                    simProxy, renderProxy, menuScheduler, pauseScheduler);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            closeQuietly(gameResources[0]);
            if (soundEngine != null) {
                soundEngine.close();
            }
        }
    }

    // Opens an OpenAL device if one is available; returns null on headless / CI machines.
    private static SoundEngine tryCreateSoundEngine() {
        try {
            return new SoundEngine();
        } catch (Exception e) {
            System.err.println("[Audio] No OpenAL device — running without sound: " + e.getMessage());
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // Player initialisation and restore
    // -------------------------------------------------------------------------

    private static void initNewPlayer(World world, Entity player, long seed, GameMode gameMode) {
        float spawnY = spawnHeight(seed);
        world.add(player, new Position(SPAWN_X, spawnY, SPAWN_Z));
        world.add(player, new Rotation(-30f, -20f));
        addSharedComponents(world, player, gameMode);
        world.add(player, new SpawnPoint(SPAWN_X, spawnY, SPAWN_Z));
    }

    private static void restorePlayer(World world, Entity player, LevelData level) {
        world.add(player, level.position());
        world.add(player, level.rotation());
        addSharedComponents(world, player, level.gameMode());
        Position p = level.position();
        world.add(player, new SpawnPoint(p.x(), p.y(), p.z()));
        // Overwrite the default health/hunger/inventory/hotbar with the saved values.
        world.add(player, level.health());
        world.add(player, level.hunger());
        world.add(player, level.inventory());
        world.add(player, level.hotbar());
    }

    // Components that are always created fresh (physics state, timers, etc.) even on load.
    private static void addSharedComponents(World world, Entity player, GameMode gameMode) {
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
        world.add(player, gameMode);
        // In creative mode start with flying enabled.
        if (gameMode.isCreative()) world.add(player, new Flying());
    }

    // Capture a consistent snapshot of the player for level.dat. Called on clean exit.
    private static LevelData snapshotLevel(World world, Entity player, long seed, GameMode gameMode) {
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
        GameMode gm     = world.get(player, GameMode.class).orElse(gameMode);
        return LevelData.of(seed, pos, rot, health, hunger, tod, hotbar, inv, gm);
    }

    // A few full stacks of placeable blocks so the player can build straight away.
    private static Inventory startingInventory() {
        Inventory inventory = Inventories.empty();
        inventory = Inventories.add(inventory, new ItemStack(WorldConstants.BLOCK_STONE, WorldConstants.MAX_STACK)).inventory();
        inventory = Inventories.add(inventory, new ItemStack(WorldConstants.BLOCK_DIRT,  WorldConstants.MAX_STACK)).inventory();
        inventory = Inventories.add(inventory, new ItemStack(WorldConstants.BLOCK_WOOD,  WorldConstants.MAX_STACK)).inventory();
        inventory = Inventories.add(inventory, new ItemStack(WorldConstants.BLOCK_TORCH, WorldConstants.MAX_STACK)).inventory();
        inventory = Inventories.add(inventory, new ItemStack(WorldConstants.ITEM_BREAD, 8)).inventory();
        return inventory;
    }

    private static float spawnHeight(long seed) {
        int surfaceY = new TerrainShape(seed).surfaceY(SPAWN_X, SPAWN_Z);
        return surfaceY + WorldConstants.PLAYER_SPAWN_CLEARANCE;
    }

    private static void closeQuietly(AutoCloseable resource) {
        if (resource == null) return;
        try {
            resource.close();
        } catch (Exception e) {
            System.err.println("Warning: failed to close game resources: " + e.getMessage());
        }
    }
}
