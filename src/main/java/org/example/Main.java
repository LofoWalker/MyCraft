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
        world.add(player, new Position(8f, 14f, 24f));
        world.add(player, new Rotation(0f, -20f));
        world.add(player, new CameraComponent(70f, 0.1f, 1000f));
        world.add(player, new PlayerInput(false, false, false, false, false, 0f, 0f));

        spawnChunk(world);

        window.captureCursor();

        try (Shader shader = Shader.fromResources("/shaders/basic.vert", "/shaders/basic.frag");
             ChunkMeshingSystem chunkMesher = new ChunkMeshingSystem()) {

            simScheduler.add(new InputSystem(window));
            simScheduler.add(new MovementSystem());

            renderScheduler.add(new CameraSystem(window.getAspectRatio()));
            renderScheduler.add(chunkMesher);
            renderScheduler.add(new RenderSystem(shader));

            GameLoop.run(window, world, simScheduler, renderScheduler);
        }
    }

    private static void spawnChunk(World world) {
        Entity chunk = world.create();
        world.add(chunk, new Position(0f, 0f, 0f));
        world.add(chunk, new ChunkComponent(0, 0));
        world.add(chunk, fillTerrain());
    }

    static VoxelChunkData fillTerrain() {
        int S = WorldConstants.CHUNK_SIZE;
        VoxelChunkData data = VoxelChunkData.empty();
        for (int bx = 0; bx < S; bx++) {
            for (int bz = 0; bz < S; bz++) {
                for (int by = 0; by < 6; by++) data.set(bx, by, bz, WorldConstants.BLOCK_STONE);
                for (int by = 6; by < 8; by++) data.set(bx, by, bz, WorldConstants.BLOCK_DIRT);
                data.set(bx, 8, bz, WorldConstants.BLOCK_GRASS);
            }
        }
        return data;
    }
}
