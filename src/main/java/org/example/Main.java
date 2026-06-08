package org.example;

import org.example.components.*;
import org.example.ecs.Entity;
import org.example.ecs.SystemScheduler;
import org.example.ecs.World;
import org.example.render.Shader;
import org.example.systems.*;
import org.example.world.WorldConstants;

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
        world.add(player, new Position(8f, 20f, 8f));
        world.add(player, new Rotation(-30f, -20f));
        world.add(player, new Velocity(0f, 0f, 0f));
        world.add(player, new Gravity(WorldConstants.GRAVITY));
        world.add(player, new ColliderAABB(0.6f, 1.8f, 0.6f));
        world.add(player, new CameraComponent(70f, 0.1f, 1000f));
        world.add(player, new PlayerInput(false, false, false, false, false, 0f, 0f));

        spawnChunks(world);

        window.captureCursor();

        try (Shader shader = Shader.fromResources("/shaders/basic.vert", "/shaders/basic.frag");
             ChunkMeshingSystem chunkMesher = new ChunkMeshingSystem()) {

            simScheduler.add(new InputSystem(window));
            simScheduler.add(new PhysicsSystem());
            simScheduler.add(new MovementSystem());
            simScheduler.add(new CollisionSystem());

            renderScheduler.add(new CameraSystem(window.getAspectRatio()));
            renderScheduler.add(new WorldGenSystem(WorldConstants.WORLD_SEED));
            renderScheduler.add(chunkMesher);
            renderScheduler.add(new RenderSystem(shader));

            GameLoop.run(window, world, simScheduler, renderScheduler);
        }
    }

    static void spawnChunks(World world) {
        int radius = 3;
        for (int cx = -radius; cx <= radius; cx++) {
            for (int cz = -radius; cz <= radius; cz++) {
                spawnEmptyChunk(world, cx, cz);
            }
        }
    }

    private static void spawnEmptyChunk(World world, int cx, int cz) {
        int S = WorldConstants.CHUNK_SIZE;
        Entity chunk = world.create();
        world.add(chunk, new Position((float) (cx * S), 0f, (float) (cz * S)));
        world.add(chunk, new ChunkComponent(cx, cz));
        world.add(chunk, VoxelChunkData.empty());
    }
}
