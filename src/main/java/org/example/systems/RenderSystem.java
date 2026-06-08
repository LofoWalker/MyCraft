package org.example.systems;

import org.example.components.RenderCamera;
import org.example.ecs.Entity;
import org.example.ecs.GameSystem;
import org.example.ecs.World;
import org.example.render.Mesh;
import org.example.render.Shader;
import org.joml.Matrix4f;

public final class RenderSystem implements GameSystem {

    private final Shader shader;
    private final Mesh   cubeMesh;

    public RenderSystem(Shader shader, Mesh cubeMesh) {
        this.shader   = shader;
        this.cubeMesh = cubeMesh;
    }

    @Override
    public void update(World world, float dt) {
        int[] cameras = world.query(RenderCamera.class);
        if (cameras.length == 0) return;

        RenderCamera camera = world.get(new Entity(cameras[0]), RenderCamera.class).orElseThrow();

        shader.bind();
        shader.setUniformMatrix4f("uModel",      new Matrix4f());
        shader.setUniformMatrix4f("uView",       camera.view());
        shader.setUniformMatrix4f("uProjection", camera.projection());
        cubeMesh.draw();
        shader.unbind();
    }
}
