package org.example.systems;

import org.example.AppState;
import org.example.AppStateHolder;
import org.example.Window;
import org.example.components.GameMode;
import org.example.ecs.GameSystem;
import org.example.ecs.World;
import org.example.io.WorldStorage;
import org.example.render.HudLayout.Rect;
import org.example.render.Shader;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.*;

/**
 * Renders and drives the main menu: "New World", "Load World" (list), "Quit".
 * Uses the same HUD shader pipeline as {@link HudSystem} — flat coloured quads, top-left origin.
 *
 * <p>Input is polled directly via GLFW because the menu runs outside the simulation scheduler
 * (no InputSystem), so GLFW key states are read here each render frame and edge-detected to avoid
 * acting on held keys.
 *
 * <p>New-world creation flow: clicking "New World" cycles through mode choices (Survival / Creative)
 * before confirming. On confirm it calls back to the supplied {@link WorldSetupCallback}.
 */
public final class MainMenuSystem implements GameSystem, AutoCloseable {

    // ---- Layout constants -----------------------------------------------
    private static final float BUTTON_WIDTH    = 320f;
    private static final float BUTTON_HEIGHT   = 48f;
    private static final float BUTTON_GAP      = 14f;
    private static final float TITLE_HEIGHT    = 60f;
    private static final float TITLE_GAP       = 40f;
    private static final float MODE_LABEL_HEIGHT = 36f;

    private static final int   MAX_WORLDS_SHOWN = 5;
    private static final float WORLD_ROW_HEIGHT = 44f;
    private static final float WORLD_ROW_GAP    = 8f;

    // ---- Colours -------------------------------------------------------
    private static final float[] BG_COLOR       = { 0.08f, 0.08f, 0.12f };
    private static final float[] TITLE_COLOR    = { 0.30f, 0.75f, 0.30f };
    private static final float   TITLE_ALPHA    = 1.0f;
    private static final float[] BUTTON_COLOR   = { 0.15f, 0.15f, 0.20f };
    private static final float   BUTTON_ALPHA   = 0.90f;
    private static final float[] HOVER_COLOR    = { 0.25f, 0.45f, 0.25f };
    private static final float   HOVER_ALPHA    = 0.95f;
    private static final float[] SELECTED_COLOR = { 0.30f, 0.55f, 0.30f };
    private static final float[] QUIT_COLOR     = { 0.30f, 0.10f, 0.10f };
    private static final float[] QUIT_HOVER     = { 0.50f, 0.15f, 0.15f };
    private static final float[] MODE_COLOR     = { 0.20f, 0.35f, 0.55f };
    private static final float   OPAQUE         = 1.0f;

    // ---- Internal state -----------------------------------------------
    private enum Screen { MAIN, NEW_WORLD, LOAD_WORLD }

    private final Window         window;
    private final AppStateHolder stateHolder;
    private final WorldSetupCallback onWorldReady;
    private final Shader         shader;
    private final int            quadVao;
    private final int            quadVbo;
    private final Matrix4f       projection = new Matrix4f();
    private final Matrix4f       model      = new Matrix4f();

    private Screen    screen       = Screen.MAIN;
    private GameMode.Mode pendingMode = GameMode.Mode.SURVIVAL;
    // World list (loaded lazily when entering LOAD_WORLD sub-screen)
    private List<String> worldList = List.of();
    private int   worldListOffset  = 0;   // scroll offset
    // Edge detection for keyboard/mouse so held keys fire once
    private boolean prevEnter = false;
    private boolean prevEsc   = false;
    private boolean prevUp    = false;
    private boolean prevDown  = false;
    private boolean prevM     = false; // mode toggle key
    // Hovered button index within current screen (-1 = none)
    private int hoveredButton = -1;

    /** Callback invoked when the player confirms a world to enter (new or loaded). */
    @FunctionalInterface
    public interface WorldSetupCallback {
        void onWorldReady(String worldName, GameMode.Mode mode);
    }

    public MainMenuSystem(Window window, AppStateHolder stateHolder, WorldSetupCallback onWorldReady) {
        this.window       = window;
        this.stateHolder  = stateHolder;
        this.onWorldReady = onWorldReady;
        this.shader       = Shader.fromResources("/shaders/hud.vert", "/shaders/hud.frag");
        this.quadVao      = glGenVertexArrays();
        this.quadVbo      = glGenBuffers();
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

        // Full-screen background
        drawRect(new Rect(0, 0, w, h), BG_COLOR, 0.85f);

        switch (screen) {
            case MAIN      -> drawMainScreen(w, h);
            case NEW_WORLD -> drawNewWorldScreen(w, h);
            case LOAD_WORLD -> drawLoadWorldScreen(w, h);
        }

        glBindVertexArray(0);
        shader.unbind();
        endOverlay();

        handleInput();
    }

    // -------------------------------------------------------------------------
    // Screen rendering
    // -------------------------------------------------------------------------

    private void drawMainScreen(int w, int h) {
        float cx     = w * 0.5f;
        float startY = h * 0.25f;

        // Title bar
        drawRect(centered(cx, startY, BUTTON_WIDTH + 60, TITLE_HEIGHT), TITLE_COLOR, TITLE_ALPHA);
        startY += TITLE_HEIGHT + TITLE_GAP;

        // "New World" button
        drawButton(0, centered(cx, startY, BUTTON_WIDTH, BUTTON_HEIGHT));
        startY += BUTTON_HEIGHT + BUTTON_GAP;

        // "Load World" button
        drawButton(1, centered(cx, startY, BUTTON_WIDTH, BUTTON_HEIGHT));
        startY += BUTTON_HEIGHT + BUTTON_GAP;

        // "Quit" button
        drawQuitButton(2, centered(cx, startY, BUTTON_WIDTH, BUTTON_HEIGHT));
    }

    private void drawNewWorldScreen(int w, int h) {
        float cx     = w * 0.5f;
        float startY = h * 0.30f;

        // Mode indicator
        float[] modeColor = pendingMode == GameMode.Mode.CREATIVE
                ? new float[]{ 0.25f, 0.50f, 0.80f }
                : new float[]{ 0.55f, 0.30f, 0.10f };
        drawRect(centered(cx, startY, BUTTON_WIDTH, MODE_LABEL_HEIGHT), modeColor, 0.90f);
        startY += MODE_LABEL_HEIGHT + BUTTON_GAP;

        // "Toggle Mode" (M key hint)
        drawButton(0, centered(cx, startY, BUTTON_WIDTH, BUTTON_HEIGHT));
        startY += BUTTON_HEIGHT + BUTTON_GAP;

        // "Create" (Enter key)
        drawButton(1, centered(cx, startY, BUTTON_WIDTH, BUTTON_HEIGHT));
        startY += BUTTON_HEIGHT + BUTTON_GAP;

        // "Back"
        drawButton(2, centered(cx, startY, BUTTON_WIDTH, BUTTON_HEIGHT));
    }

    private void drawLoadWorldScreen(int w, int h) {
        float cx     = w * 0.5f;
        float startY = h * 0.22f;

        int shown = Math.min(worldList.size(), MAX_WORLDS_SHOWN);
        for (int i = 0; i < shown; i++) {
            int listIndex = i + worldListOffset;
            if (listIndex >= worldList.size()) break;
            float[] color = (i == hoveredButton) ? HOVER_COLOR : BUTTON_COLOR;
            drawRect(centered(cx, startY, BUTTON_WIDTH, WORLD_ROW_HEIGHT), color, BUTTON_ALPHA);
            startY += WORLD_ROW_HEIGHT + WORLD_ROW_GAP;
        }

        if (worldList.isEmpty()) {
            drawRect(centered(cx, startY, BUTTON_WIDTH, WORLD_ROW_HEIGHT), BUTTON_COLOR, 0.40f);
            startY += WORLD_ROW_HEIGHT + BUTTON_GAP * 3;
        }
        startY += BUTTON_GAP * 2;

        // "Back"
        drawQuitButton(MAX_WORLDS_SHOWN, centered(cx, startY, BUTTON_WIDTH, BUTTON_HEIGHT));
    }

    private void drawButton(int index, Rect rect) {
        float[] color = (index == hoveredButton) ? HOVER_COLOR : BUTTON_COLOR;
        drawRect(rect, color, BUTTON_ALPHA);
    }

    private void drawQuitButton(int index, Rect rect) {
        float[] color = (index == hoveredButton) ? QUIT_HOVER : QUIT_COLOR;
        drawRect(rect, color, BUTTON_ALPHA);
    }

    // -------------------------------------------------------------------------
    // Input handling
    // -------------------------------------------------------------------------

    private void handleInput() {
        long handle = window.getHandle();
        boolean enter = glfwGetKey(handle, GLFW_KEY_ENTER) == GLFW_PRESS;
        boolean esc   = glfwGetKey(handle, GLFW_KEY_ESCAPE) == GLFW_PRESS;
        boolean up    = glfwGetKey(handle, GLFW_KEY_UP)     == GLFW_PRESS;
        boolean down  = glfwGetKey(handle, GLFW_KEY_DOWN)   == GLFW_PRESS;
        boolean m     = glfwGetKey(handle, GLFW_KEY_M)      == GLFW_PRESS;

        boolean justEnter = enter && !prevEnter;
        boolean justEsc   = esc   && !prevEsc;
        boolean justUp    = up    && !prevUp;
        boolean justDown  = down  && !prevDown;
        boolean justM     = m     && !prevM;

        prevEnter = enter;
        prevEsc   = esc;
        prevUp    = up;
        prevDown  = down;
        prevM     = m;

        switch (screen) {
            case MAIN       -> handleMainInput(justEnter, justEsc, justUp, justDown);
            case NEW_WORLD  -> handleNewWorldInput(justEnter, justEsc, justM, justUp, justDown);
            case LOAD_WORLD -> handleLoadWorldInput(justEnter, justEsc, justUp, justDown);
        }
    }

    private void handleMainInput(boolean enter, boolean esc, boolean up, boolean down) {
        int buttons = 3;
        if (up)   hoveredButton = Math.max(0, hoveredButton - 1);
        if (down) hoveredButton = Math.min(buttons - 1, hoveredButton == -1 ? 0 : hoveredButton + 1);
        if (esc)  { glfwSetWindowShouldClose(window.getHandle(), true); return; }
        if (!enter) return;
        switch (hoveredButton) {
            case 0 -> { screen = Screen.NEW_WORLD; hoveredButton = -1; }
            case 1 -> {
                worldList = WorldStorage.listWorlds();
                worldListOffset = 0;
                screen = Screen.LOAD_WORLD;
                hoveredButton = worldList.isEmpty() ? MAX_WORLDS_SHOWN : 0;
            }
            case 2 -> glfwSetWindowShouldClose(window.getHandle(), true);
        }
    }

    private void handleNewWorldInput(boolean enter, boolean esc, boolean mToggle,
                                     boolean up, boolean down) {
        int buttons = 3;
        if (up)   hoveredButton = Math.max(0, hoveredButton - 1);
        if (down) hoveredButton = Math.min(buttons - 1, hoveredButton == -1 ? 0 : hoveredButton + 1);
        if (mToggle || (enter && hoveredButton == 0)) {
            pendingMode = (pendingMode == GameMode.Mode.SURVIVAL)
                    ? GameMode.Mode.CREATIVE
                    : GameMode.Mode.SURVIVAL;
        }
        if (esc || (enter && hoveredButton == 2)) { screen = Screen.MAIN; hoveredButton = 0; return; }
        if (enter && hoveredButton == 1) {
            String worldName = "world";
            onWorldReady.onWorldReady(worldName, pendingMode);
        }
    }

    private void handleLoadWorldInput(boolean enter, boolean esc, boolean up, boolean down) {
        if (worldList.isEmpty()) {
            if (esc || enter) { screen = Screen.MAIN; hoveredButton = 1; }
            return;
        }
        int shown = Math.min(worldList.size(), MAX_WORLDS_SHOWN);
        if (up)   hoveredButton = Math.max(0, hoveredButton - 1);
        if (down) hoveredButton = Math.min(shown, hoveredButton + 1);
        if (esc || (enter && hoveredButton == MAX_WORLDS_SHOWN)) {
            screen = Screen.MAIN;
            hoveredButton = 1;
            return;
        }
        if (enter && hoveredButton >= 0 && hoveredButton < shown) {
            int listIndex = hoveredButton + worldListOffset;
            if (listIndex < worldList.size()) {
                String name = worldList.get(listIndex);
                // Load existing world: mode will be read from level.dat
                onWorldReady.onWorldReady(name, GameMode.Mode.SURVIVAL);
            }
        }
    }

    // -------------------------------------------------------------------------
    // GL helpers
    // -------------------------------------------------------------------------

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
