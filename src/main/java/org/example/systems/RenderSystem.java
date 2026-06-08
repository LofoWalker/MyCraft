package org.example.systems;

import org.example.components.ChunkMeshComponent;
import org.example.components.Position;
import org.example.components.RenderCamera;
import org.example.ecs.Entity;
import org.example.ecs.GameSystem;
import org.example.ecs.World;
import org.example.render.Shader;
import org.joml.Matrix4f;

public final class RenderSystem implements GameSystem {

    private final Shader shader;

    public RenderSystem(Shader shader) {
        this.shader = shader;
    }

    @Override
    public void update(World world, float dt) {
        int[] cameras = world.query(RenderCamera.class);
        if (cameras.length == 0) return;

        RenderCamera camera = world.get(new Entity(cameras[0]), RenderCamera.class).orElseThrow();

        shader.bind();
        shader.setUniformMatrix4f("uView",       camera.view());
        shader.setUniformMatrix4f("uProjection", camera.projection());

        for (int eid : world.query(ChunkMeshComponent.class, Position.class)) {
            Entity entity = new Entity(eid);
            Position pos = world.get(entity, Position.class).orElseThrow();
            ChunkMeshComponent chunkMesh = world.get(entity, ChunkMeshComponent.class).orElseThrow();
            shader.setUniformMatrix4f("uModel", new Matrix4f().translation(pos.x(), pos.y(), pos.z()));
            chunkMesh.mesh().draw();
        }

        shader.unbind();
    }
}
