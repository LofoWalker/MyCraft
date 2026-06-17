package org.example;

import org.example.components.*;
import org.example.ecs.Entity;
import org.example.ecs.SystemScheduler;
import org.example.ecs.World;
import org.example.render.Shader;
import org.example.systems.*;
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
        world.add(player, new PlayerInput(false, false, false, false, false, false, 0f, 0f, false));

        window.captureCursor();

        try (Shader shader = Shader.fromResources("/shaders/basic.vert", "/shaders/basic.frag");
             ChunkStreamingSystem chunkStreaming = new ChunkStreamingSystem(seed);
             SkySystem sky = new SkySystem();
             BlockBreakOverlaySystem breakOverlay = new BlockBreakOverlaySystem()) {

            simScheduler.add(new InputSystem(window));
            simScheduler.add(new BlockInteractionSystem());
            simScheduler.add(new FlightControlSystem());
            simScheduler.add(new PhysicsSystem());
            simScheduler.add(new MovementSystem());
            simScheduler.add(new CollisionSystem());

            renderScheduler.add(new CameraSystem(window.getAspectRatio()));
            renderScheduler.add(sky);
            renderScheduler.add(chunkStreaming);
            renderScheduler.add(new RenderSystem(shader));
            renderScheduler.add(breakOverlay);

            GameLoop.run(window, world, simScheduler, renderScheduler);
        }
    }

    // Drop the player just above the real surface at the spawn column so they land on solid ground
    // instead of falling through the void or spawning inside terrain.
    private static float spawnHeight(long seed) {
        int surfaceY = new TerrainShape(seed).surfaceY(SPAWN_X, SPAWN_Z);
        return surfaceY + WorldConstants.PLAYER_SPAWN_CLEARANCE;
    }
}
