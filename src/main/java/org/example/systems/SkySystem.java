package org.example.systems;

import org.example.components.RenderCamera;
import org.example.ecs.Entity;
import org.example.ecs.GameSystem;
import org.example.ecs.World;
import org.example.render.Shader;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.glfwGetTime;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;

public final class SkySystem implements GameSystem, AutoCloseable {

    private static final Vector3f SUN_DIRECTION =
        new Vector3f(0.35f, 0.55f, 0.45f).normalize();

    private final Shader shader;
    private final int    emptyVao;

    // Reused every frame: the sky pass must not allocate.
    private final Matrix4f invProjection = new Matrix4f();
    private final Matrix4f invView       = new Matrix4f();

    public SkySystem() {
        this.shader   = Shader.fromResources("/shaders/sky.vert", "/shaders/sky.frag");
        this.emptyVao = glGenVertexArrays();
    }

    @Override
    public void update(World world, float dt) {
        int[] cameras = world.query(RenderCamera.class);
        if (cameras.length == 0) return;

        RenderCamera camera = world.get(new Entity(cameras[0]), RenderCamera.class).orElseThrow();
        camera.projection().invert(invProjection);
        camera.view().invert(invView);

        // The sky fills every pixel and sits behind everything: no depth, no culling.
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);

        shader.bind();
        shader.setUniformMatrix4f("uInvProjection", invProjection);
        shader.setUniformMatrix4f("uInvView",       invView);
        shader.setUniform3f("uSunDir", SUN_DIRECTION.x, SUN_DIRECTION.y, SUN_DIRECTION.z);
        shader.setUniform1f("uTime", (float) glfwGetTime());

        glBindVertexArray(emptyVao);
        glDrawArrays(GL_TRIANGLES, 0, 3);
        glBindVertexArray(0);

        shader.unbind();

        glEnable(GL_CULL_FACE);
        glEnable(GL_DEPTH_TEST);
    }

    @Override
    public void close() {
        glDeleteVertexArrays(emptyVao);
        shader.close();
    }
}
