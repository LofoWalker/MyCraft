package org.example.systems;

import org.example.components.ChunkMeshComponent;
import org.example.components.Position;
import org.example.components.RenderCamera;
import org.example.ecs.Entity;
import org.example.ecs.GameSystem;
import org.example.ecs.World;
import org.example.render.Frustum;
import org.example.render.Shader;
import org.example.world.WorldConstants;
import org.joml.Matrix4f;

public final class RenderSystem implements GameSystem {

    private final Shader shader;

    // Reused every frame: the per-chunk draw loop must not allocate.
    private final Frustum  frustum        = new Frustum();
    private final Matrix4f viewProjection = new Matrix4f();
    private final Matrix4f model          = new Matrix4f();

    public RenderSystem(Shader shader) {
        this.shader = shader;
    }

    @Override
    public void update(World world, float dt) {
        int[] cameras = world.query(RenderCamera.class);
        if (cameras.length == 0) return;

        RenderCamera camera = world.get(new Entity(cameras[0]), RenderCamera.class).orElseThrow();
        viewProjection.set(camera.projection()).mul(camera.view());
        frustum.update(viewProjection);

        shader.bind();
        shader.setUniformMatrix4f("uView",       camera.view());
        shader.setUniformMatrix4f("uProjection", camera.projection());

        for (int eid : world.query(ChunkMeshComponent.class, Position.class)) {
            Entity entity = new Entity(eid);
            Position pos = world.get(entity, Position.class).orElseThrow();
            if (!isChunkVisible(pos)) continue;
            shader.setUniformMatrix4f("uModel", model.translation(pos.x(), pos.y(), pos.z()));
            world.get(entity, ChunkMeshComponent.class).orElseThrow().mesh().draw();
        }

        shader.unbind();
    }

    private boolean isChunkVisible(Position pos) {
        float sx = WorldConstants.CHUNK_SIZE_XZ;
        float h  = WorldConstants.WORLD_HEIGHT;
        return frustum.isBoxVisible(
            pos.x(),      pos.y(),     pos.z(),
            pos.x() + sx, pos.y() + h, pos.z() + sx
        );
    }
}
