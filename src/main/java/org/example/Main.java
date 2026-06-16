package org.example;

import org.example.components.*;
import org.example.ecs.Entity;
import org.example.ecs.SystemScheduler;
import org.example.ecs.World;
import org.example.render.Shader;
import org.example.systems.*;
import org.example.world.WorldConstants;

import java.util.concurrent.ThreadLocalRandom;

public class Main {
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

        Entity player = world.create();
        world.add(player, new Position(8f, WorldConstants.FLAT_SURFACE_LEVEL + 10f, 8f));
        world.add(player, new Rotation(-30f, -20f));
        world.add(player, new Velocity(0f, 0f, 0f));
        world.add(player, new Gravity(WorldConstants.GRAVITY));
        world.add(player, new ColliderAABB(0.6f, 1.8f, 0.6f));
        world.add(player, new CameraComponent(70f, 0.1f, 1000f));
        world.add(player, new PlayerInput(false, false, false, false, false, false, 0f, 0f));

        window.captureCursor();

        long seed = ThreadLocalRandom.current().nextLong();
        System.out.println("World seed: " + seed);

        try (Shader shader = Shader.fromResources("/shaders/basic.vert", "/shaders/basic.frag");
             ChunkStreamingSystem chunkStreaming = new ChunkStreamingSystem(seed);
             SkySystem sky = new SkySystem()) {

            simScheduler.add(new InputSystem(window));
            simScheduler.add(new FlightControlSystem());
            simScheduler.add(new PhysicsSystem());
            simScheduler.add(new MovementSystem());
            simScheduler.add(new CollisionSystem());

            renderScheduler.add(new CameraSystem(window.getAspectRatio()));
            renderScheduler.add(sky);
            renderScheduler.add(chunkStreaming);
            renderScheduler.add(new RenderSystem(shader));

            GameLoop.run(window, world, simScheduler, renderScheduler);
        }
    }
}
