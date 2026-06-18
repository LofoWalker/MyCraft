package org.example.systems;

import org.example.AppState;
import org.example.AppStateHolder;
import org.example.Window;
import org.example.ecs.GameSystem;
import org.example.ecs.World;
import org.example.render.HudLayout.Rect;
import org.example.render.Shader;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Pause overlay rendered on top of the frozen game world. Shows "Resume" and "Save &amp; Quit" buttons.
 * The cursor is released while paused; Escape or "Resume" re-captures it and resumes simulation.
 *
 * <p>Input is polled directly via GLFW (same reasoning as {@link MainMenuSystem}).
 */
public final class PauseMenuSystem implements GameSystem, AutoCloseable {

    // ---- Layout constants -----------------------------------------------
    private static final float BUTTON_WIDTH  = 280f;
    private static final float BUTTON_HEIGHT = 48f;
    private static final float BUTTON_GAP    = 14f;

    // ---- Colours -------------------------------------------------------
    private static final float[] OVERLAY_BG  = { 0.00f, 0.00f, 0.05f };
    private static final float   OVERLAY_ALPHA = 0.60f;
    private static final float[] BUTTON_COLOR = { 0.15f, 0.15f, 0.20f };
    private static final float   BUTTON_ALPHA = 0.92f;
    private static final float[] HOVER_COLOR  = { 0.25f, 0.45f, 0.25f };
    private static final float[] QUIT_COLOR   = { 0.30f, 0.10f, 0.10f };
    private static final float[] QUIT_HOVER   = { 0.50f, 0.15f, 0.15f };

    // ---- State ---------------------------------------------------------
    private final Window         window;
    private final AppStateHolder stateHolder;
    private final Runnable       saveAndQuit;
    private final Shader         shader;
    private final int            quadVao;
    private final int            quadVbo;
    private final Matrix4f       projection = new Matrix4f();
    private final Matrix4f       model      = new Matrix4f();

    private int     hoveredButton = 0;
    private boolean prevEnter     = false;
    private boolean prevEsc       = false;
    private boolean prevUp        = false;
    private boolean prevDown      = false;

    public PauseMenuSystem(Window window, AppStateHolder stateHolder, Runnable saveAndQuit) {
        this.window      = window;
        this.stateHolder = stateHolder;
        this.saveAndQuit = saveAndQuit;
        this.shader      = Shader.fromResources("/shaders/hud.vert", "/shaders/hud.frag");
        this.quadVao     = glGenVertexArrays();
        this.quadVbo     = glGenBuffers();
        uploadUnitQuad();
    }

    private void uploadUnitQuad() {
        glBindVertexArray(quadVao);
        FloatBuffer buffer = MemoryUtil.memAllocFloat(8);
        try {
            buffer.put(new float[]{ 0f, 0f,  1f, 0f,  1f, 1f,  0f, 1f }).flip();
            glBindBuffer(GL_ARRAY_BUFFER, quadVbo);
            glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);
        } finally {
            MemoryUtil.memFree(buffer);
        }
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 2 * Float.BYTES, 0L);
        glEnableVertexAttribArray(0);
        glBindVertexArray(0);
    }

    @Override
    public void update(World world, float dt) {
        int w = window.getWidth();
        int h = window.getHeight();
        projection.setOrtho(0f, w, h, 0f, -1f, 1f);

        beginOverlay();
        shader.bind();
        shader.setUniformMatrix4f("uProjection", projection);
        glBindVertexArray(quadVao);

        // Semi-transparent dark overlay over the frozen world
        drawRect(new Rect(0, 0, w, h), OVERLAY_BG, OVERLAY_ALPHA);

        float cx     = w * 0.5f;
        float startY = h * 0.38f;

        // "Resume" button
        drawButton(0, centered(cx, startY, BUTTON_WIDTH, BUTTON_HEIGHT));
        startY += BUTTON_HEIGHT + BUTTON_GAP;

        // "Save & Quit" button
        drawQuitButton(1, centered(cx, startY, BUTTON_WIDTH, BUTTON_HEIGHT));

        glBindVertexArray(0);
        shader.unbind();
        endOverlay();

        handleInput();
    }

    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------

    private void handleInput() {
        long handle = window.getHandle();
        boolean enter = glfwGetKey(handle, GLFW_KEY_ENTER) == GLFW_PRESS;
        boolean esc   = glfwGetKey(handle, GLFW_KEY_ESCAPE) == GLFW_PRESS;
        boolean up    = glfwGetKey(handle, GLFW_KEY_UP)    == GLFW_PRESS;
        boolean down  = glfwGetKey(handle, GLFW_KEY_DOWN)  == GLFW_PRESS;

        boolean justEnter = enter && !prevEnter;
        boolean justEsc   = esc   && !prevEsc;
        boolean justUp    = up    && !prevUp;
        boolean justDown  = down  && !prevDown;

        prevEnter = enter;
        prevEsc   = esc;
        prevUp    = up;
        prevDown  = down;

        if (justUp)   hoveredButton = Math.max(0, hoveredButton - 1);
        if (justDown) hoveredButton = Math.min(1, hoveredButton + 1);

        if (justEsc || (justEnter && hoveredButton == 0)) resume();
        else if (justEnter && hoveredButton == 1) saveAndQuit.run();
    }

    private void resume() {
        window.captureCursor();
        stateHolder.request(AppState.IN_GAME);
        hoveredButton = 0;
    }

    // -------------------------------------------------------------------------
    // GL helpers
    // -------------------------------------------------------------------------

    private void drawButton(int index, Rect rect) {
        float[] color = (index == hoveredButton) ? HOVER_COLOR : BUTTON_COLOR;
        drawRect(rect, color, BUTTON_ALPHA);
    }

    private void drawQuitButton(int index, Rect rect) {
        float[] color = (index == hoveredButton) ? QUIT_HOVER : QUIT_COLOR;
        drawRect(rect, color, BUTTON_ALPHA);
    }

    private void drawRect(Rect rect, float[] color, float alpha) {
        model.translation(rect.x(), rect.y(), 0f).scale(rect.w(), rect.h(), 1f);
        shader.setUniformMatrix4f("uModel", model);
        shader.setUniform3f("uColor", color[0], color[1], color[2]);
        shader.setUniform1f("uAlpha", alpha);
        glDrawArrays(GL_TRIANGLE_FAN, 0, 4);
    }

    private static Rect centered(float cx, float cy, float w, float h) {
        return new Rect(cx - w * 0.5f, cy - h * 0.5f, w, h);
    }

    private static void beginOverlay() {
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    }

    private static void endOverlay() {
        glDisable(GL_BLEND);
        glEnable(GL_DEPTH_TEST);
    }

    @Override
    public void close() {
        glDeleteBuffers(quadVbo);
        glDeleteVertexArrays(quadVao);
        shader.close();
    }
}
