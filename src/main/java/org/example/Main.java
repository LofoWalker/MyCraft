package org.example;

import org.example.components.*;
import org.example.ecs.Entity;
import org.example.ecs.SystemScheduler;
import org.example.ecs.World;
import org.example.render.Shader;
import org.example.render.TextureAtlas;
import org.example.systems.*;
import org.example.world.Inventories;
import org.example.world.WorldConstants;
import org.example.worldgen.TerrainShape;

import java.util.concurrent.ThreadLocalRandom;

public class Main {

    private static final int   SPAWN_X = 8;
    private static final int   SPAWN_Z = 8;

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

        long seed = ThreadLocalRandom.current().nextLong();
        System.out.println("World seed: " + seed);

        Entity player = world.create();
        world.add(player, new Position(SPAWN_X, spawnHeight(seed), SPAWN_Z));
        world.add(player, new Rotation(-30f, -20f));
        world.add(player, new Velocity(0f, 0f, 0f));
        world.add(player, new Gravity(WorldConstants.GRAVITY));
        world.add(player, new ColliderAABB(0.6f, 1.8f, 0.6f));
        world.add(player, new CameraComponent(70f, 0.1f, 1000f));
        world.add(player, new PlayerInput(false, false, false, false, false, false, 0f, 0f, false, false,
                0, WorldConstants.NO_HOTBAR_SELECT));
        world.add(player, new Hotbar(0));
        world.add(player, startingInventory());

        window.captureCursor();

        try (Shader shader = Shader.fromResources("/shaders/basic.vert", "/shaders/basic.frag");
             TextureAtlas atlas = TextureAtlas.loadFromClasspath("/textures/blocks.png");
             ChunkStreamingSystem chunkStreaming = new ChunkStreamingSystem(seed);
             SkySystem sky = new SkySystem();
             BlockHighlightSystem blockHighlight = new BlockHighlightSystem();
             BlockBreakOverlaySystem breakOverlay = new BlockBreakOverlaySystem();
             ItemRenderSystem itemRender = new ItemRenderSystem();
             HudSystem hud = new HudSystem(window)) {

            simScheduler.add(new InputSystem(window));
            simScheduler.add(new HotbarSelectionSystem());
            simScheduler.add(new BlockInteractionSystem());
            simScheduler.add(new FlightControlSystem());
            simScheduler.add(new PhysicsSystem());
            simScheduler.add(new MovementSystem());
            simScheduler.add(new ItemMotionSystem());
            simScheduler.add(new CollisionSystem());
            simScheduler.add(new ItemPickupSystem());

            renderScheduler.add(new CameraSystem(window.getAspectRatio()));
            renderScheduler.add(sky);
            renderScheduler.add(chunkStreaming);
            renderScheduler.add(new RenderSystem(shader, atlas));
            renderScheduler.add(blockHighlight);
            renderScheduler.add(breakOverlay);
            renderScheduler.add(itemRender);
            renderScheduler.add(hud);

            GameLoop.run(window, world, simScheduler, renderScheduler);
        }
    }

    // A few full stacks of placeable blocks so the player can build straight away. They land in the
    // first hotbar slots, so slot 0 (the default selection) holds stone.
    private static Inventory startingInventory() {
        Inventory inventory = Inventories.empty();
        inventory = Inventories.add(inventory, new ItemStack(WorldConstants.BLOCK_STONE, WorldConstants.MAX_STACK)).inventory();
        inventory = Inventories.add(inventory, new ItemStack(WorldConstants.BLOCK_DIRT,  WorldConstants.MAX_STACK)).inventory();
        inventory = Inventories.add(inventory, new ItemStack(WorldConstants.BLOCK_WOOD,  WorldConstants.MAX_STACK)).inventory();
        return inventory;
    }

    // Drop the player just above the real surface at the spawn column so they land on solid ground
    // instead of falling through the void or spawning inside terrain.
    private static float spawnHeight(long seed) {
        int surfaceY = new TerrainShape(seed).surfaceY(SPAWN_X, SPAWN_Z);
        return surfaceY + WorldConstants.PLAYER_SPAWN_CLEARANCE;
    }
}
