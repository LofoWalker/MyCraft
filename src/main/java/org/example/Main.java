package org.example;

import org.example.components.CameraComponent;
import org.example.components.Position;
import org.example.components.Rotation;
import org.example.ecs.Entity;
import org.example.ecs.SystemScheduler;
import org.example.ecs.World;
import org.example.render.Mesh;
import org.example.render.Shader;
import org.example.systems.CameraSystem;
import org.example.systems.RenderSystem;

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
        world.add(player, new Position(0f, 0f, 3f));
        world.add(player, new Rotation(0f, 0f));
        world.add(player, new CameraComponent(70f, 0.1f, 1000f));

        try (Shader shader   = Shader.fromResources("/shaders/basic.vert", "/shaders/basic.frag");
             Mesh   cubeMesh = Mesh.createTestCube()) {

            renderScheduler.add(new CameraSystem(window.getAspectRatio()));
            renderScheduler.add(new RenderSystem(shader, cubeMesh));

            GameLoop.run(window, world, simScheduler, renderScheduler);
        }
    }
}
