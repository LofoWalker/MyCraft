package org.example.systems;

import org.example.components.RenderCamera;
import org.example.components.TargetedBlock;
import org.example.ecs.Entity;
import org.example.ecs.GameSystem;
import org.example.ecs.World;
import org.example.render.Mesh;
import org.example.render.Shader;
import org.joml.Matrix4f;

import static org.lwjgl.opengl.GL11.*;

// Targeting feedback: draws a black wireframe outline around the block the player is looking at.
// Reads TargetedBlock (sim) plus RenderCamera, and runs before the break overlay so the outline sits
// underneath any mining tint. Reuses one cube mesh and one matrix; no per-frame allocation.
public final class BlockHighlightSystem implements GameSystem, AutoCloseable {

    // Slightly larger than the block so the outline sits just outside its faces, avoiding z-fighting.
    private static final float OUTLINE_SCALE = 1.002f;
    private static final float OUTLINE_ALPHA = 1.0f;

    private final Shader   shader;
    private final Mesh     cube;
    private final Matrix4f model = new Matrix4f();

    public BlockHighlightSystem() {
        this.shader = Shader.fromResources("/shaders/highlight.vert", "/shaders/highlight.frag");
        this.cube   = Mesh.createTestCube();
    }

    @Override
    public void update(World world, float dt) {
        int[] cameras = world.query(RenderCamera.class);
        int[] targets = world.query(TargetedBlock.class);
        if (cameras.length == 0 || targets.length == 0) return;

        RenderCamera  camera = world.get(new Entity(cameras[0]), RenderCamera.class).orElseThrow();
        TargetedBlock target = world.get(new Entity(targets[0]), TargetedBlock.class).orElseThrow();
        drawOutline(camera, target);
    }

    private void drawOutline(RenderCamera camera, TargetedBlock target) {
        model.translation(target.x() + 0.5f, target.y() + 0.5f, target.z() + 0.5f).scale(OUTLINE_SCALE);

        glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
        shader.bind();
        shader.setUniformMatrix4f("uModel",      model);
        shader.setUniformMatrix4f("uView",       camera.view());
        shader.setUniformMatrix4f("uProjection", camera.projection());
        shader.setUniform3f("uColor", 0f, 0f, 0f);
        shader.setUniform1f("uAlpha", OUTLINE_ALPHA);
        cube.draw();
        shader.unbind();
        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
    }

    @Override
    public void close() {
        cube.close();
        shader.close();
    }
}
