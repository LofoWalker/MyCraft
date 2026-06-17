package org.example.systems;

import org.example.components.RenderCamera;
import org.example.components.TimeOfDay;
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

    // Fixed east-west tilt: the sun's arc leans slightly along X so it does not pass through the exact
    // zenith. The remaining motion is the day rotation in the Y-Z plane (full circle over one day).
    private static final float SUN_AXIS_TILT_X = 0.30f;
    private static final float FULL_CIRCLE     = (float) (2.0 * Math.PI);

    private final Shader shader;
    private final int    emptyVao;

    // Reused every frame: the sky pass must not allocate.
    private final Matrix4f invProjection = new Matrix4f();
    private final Matrix4f invView       = new Matrix4f();
    private final Vector3f bodyDir       = new Vector3f();

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

        float dayFraction = readDayFraction(world);
        celestialBodyDirection(dayFraction, bodyDir);
        float dayFactor = TimeSystem.globalLightFactor(dayFraction);

        // The sky fills every pixel and sits behind everything: no depth, no culling.
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);

        shader.bind();
        shader.setUniformMatrix4f("uInvProjection", invProjection);
        shader.setUniformMatrix4f("uInvView",       invView);
        shader.setUniform3f("uSunDir", bodyDir.x, bodyDir.y, bodyDir.z);
        shader.setUniform1f("uDayFactor", dayFactor);
        shader.setUniform1f("uTime", (float) glfwGetTime());

        glBindVertexArray(emptyVao);
        glDrawArrays(GL_TRIANGLES, 0, 3);
        glBindVertexArray(0);

        shader.unbind();

        glEnable(GL_CULL_FACE);
        glEnable(GL_DEPTH_TEST);
    }

    private static float readDayFraction(World world) {
        int[] clocks = world.query(TimeOfDay.class);
        if (clocks.length == 0) return 0.0f;
        return world.get(new Entity(clocks[0]), TimeOfDay.class).orElseThrow().dayFraction();
    }

    // The lit celestial body's direction. By day it is the sun; once the sun dips below the horizon
    // we hand the shader the moon direction (the sun mirrored to the opposite side) so a body is
    // always above the horizon to anchor the sky and cast the disc.
    static void celestialBodyDirection(float dayFraction, Vector3f out) {
        float angle = dayFraction * FULL_CIRCLE;
        float y = (float) Math.sin(angle);
        float z = (float) -Math.cos(angle);
        out.set(SUN_AXIS_TILT_X, y, z);
        if (y < 0.0f) out.negate();
        out.normalize();
    }

    @Override
    public void close() {
        glDeleteVertexArrays(emptyVao);
        shader.close();
    }
}
