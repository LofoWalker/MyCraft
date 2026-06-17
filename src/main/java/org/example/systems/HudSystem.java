package org.example.systems;

import org.example.components.Health;
import org.example.components.Hotbar;
import org.example.components.Hunger;
import org.example.components.Inventory;
import org.example.components.ItemStack;
import org.example.ecs.Entity;
import org.example.ecs.GameSystem;
import org.example.ecs.World;
import org.example.render.BitmapFont;
import org.example.render.HudLayout;
import org.example.render.HudLayout.Rect;
import org.example.render.Shader;
import org.example.Window;
import org.example.world.BlockType;
import org.example.world.WorldConstants;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * 2D HUD overlay: a crosshair plus a nine-slot hotbar reflecting the player's inventory and selected
 * slot. Runs last in the render schedule. Sets up a pixel-space orthographic projection with depth
 * testing off and blending on, then restores the previous state. Pure rendering: it only reads the
 * ECS and never mutates the simulation. Every quad is drawn from one shared unit-quad VAO with one
 * reused model matrix and projection matrix, so no GL buffers or matrices are allocated per frame.
 * (Layout helpers return tiny short-lived {@link Rect} value records; the JIT scalar-replaces them.)
 */
public final class HudSystem implements GameSystem, AutoCloseable {

    private static final float COUNT_CELL_SIZE = 3f;
    private static final float COUNT_PADDING   = 4f;

    private static final float[] CROSSHAIR_COLOR   = { 1f, 1f, 1f };
    private static final float[] HEART_FULL_COLOR  = { 0.85f, 0.15f, 0.15f };
    private static final float[] HEART_EMPTY_COLOR = { 0.20f, 0.20f, 0.20f };
    private static final float[] FOOD_FULL_COLOR   = { 0.80f, 0.55f, 0.20f };
    private static final float[] FOOD_EMPTY_COLOR  = { 0.20f, 0.20f, 0.20f };
    private static final float[] SLOT_COLOR        = { 0.10f, 0.10f, 0.10f };
    private static final float[] SELECTION_COLOR   = { 1f, 1f, 1f };
    private static final float[] COUNT_COLOR       = { 1f, 1f, 1f };
    private static final float   SLOT_ALPHA        = 0.55f;
    private static final float   SELECTION_ALPHA   = 0.95f;
    private static final float   OPAQUE            = 1f;

    private final Window   window;
    private final Shader   shader;
    private final int      quadVao;
    private final int      quadVbo;

    // Reused every frame: the HUD pass must not allocate.
    private final Matrix4f projection = new Matrix4f();
    private final Matrix4f model      = new Matrix4f();

    public HudSystem(Window window) {
        this.window = window;
        this.shader = Shader.fromResources("/shaders/hud.vert", "/shaders/hud.frag");
        this.quadVao = glGenVertexArrays();
        this.quadVbo = glGenBuffers();
        uploadUnitQuad();
    }

    private void uploadUnitQuad() {
        glBindVertexArray(quadVao);
        FloatBuffer buffer = MemoryUtil.memAllocFloat(8);
        try {
            buffer.put(new float[] { 0f, 0f,  1f, 0f,  1f, 1f,  0f, 1f }).flip();
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
        int width  = window.getWidth();
        int height = window.getHeight();
        // Top-left origin: map x in [0,width] and y in [0,height] (y down) onto NDC.
        projection.setOrtho(0f, width, height, 0f, -1f, 1f);

        beginOverlay();
        shader.bind();
        shader.setUniformMatrix4f("uProjection", projection);
        glBindVertexArray(quadVao);

        drawCrosshair(width, height);
        drawHotbar(world, width, height);
        drawHealth(world, width, height);
        drawFood(world, width, height);

        glBindVertexArray(0);
        shader.unbind();
        endOverlay();
    }

    private void drawCrosshair(int width, int height) {
        drawRect(HudLayout.crosshairHorizontal(width, height), CROSSHAIR_COLOR, OPAQUE);
        drawRect(HudLayout.crosshairVertical(width, height), CROSSHAIR_COLOR, OPAQUE);
    }

    private void drawHotbar(World world, int width, int height) {
        Inventory inventory = playerInventory(world);
        int selected = selectedSlot(world);

        for (int i = 0; i < WorldConstants.HOTBAR_SLOTS; i++) {
            if (i == selected) {
                drawRect(HudLayout.selectionFrame(i, width, height), SELECTION_COLOR, SELECTION_ALPHA);
            }
            drawRect(HudLayout.slot(i, width, height), SLOT_COLOR, SLOT_ALPHA);
            drawSlotItem(inventory, i, width, height);
        }
    }

    private void drawHealth(World world, int width, int height) {
        Health health = playerHealth(world);
        if (health == null) return;
        int hearts = HudLayout.heartCount(health.max());
        int full   = Math.max(health.current(), 0) / HudLayout.HEALTH_PER_HEART;
        for (int i = 0; i < hearts; i++) {
            float[] color = i < full ? HEART_FULL_COLOR : HEART_EMPTY_COLOR;
            drawRect(HudLayout.heart(i, width, height), color, OPAQUE);
        }
    }

    private void drawFood(World world, int width, int height) {
        Hunger hunger = playerHunger(world);
        if (hunger == null) return;
        int icons = HudLayout.foodIconCount(WorldConstants.MAX_FOOD);
        int full  = Math.max(hunger.food(), 0) / HudLayout.FOOD_PER_ICON;
        for (int i = 0; i < icons; i++) {
            float[] color = i < full ? FOOD_FULL_COLOR : FOOD_EMPTY_COLOR;
            drawRect(HudLayout.food(i, width, height), color, OPAQUE);
        }
    }

    private void drawSlotItem(Inventory inventory, int index, int width, int height) {
        if (inventory == null) return;
        ItemStack stack = inventory.slots()[index];
        if (stack.isEmpty()) return;

        float[] color = BlockType.byId((byte) stack.itemId()).colorTop();
        drawRect(HudLayout.itemPreview(index, width, height), color, OPAQUE);
        if (stack.count() > 1) {
            drawCount(stack.count(), HudLayout.slot(index, width, height));
        }
    }

    private void drawCount(int count, Rect slot) {
        float textWidth = BitmapFont.measureWidth(count, COUNT_CELL_SIZE);
        float startX = slot.x() + slot.w() - COUNT_PADDING - textWidth;
        float startY = slot.y() + slot.h() - COUNT_PADDING - BitmapFont.GLYPH_ROWS * COUNT_CELL_SIZE;
        drawNumber(count, startX, startY);
    }

    private void drawNumber(int value, float startX, float startY) {
        int digits = BitmapFont.digitCount(value);
        for (int d = 0; d < digits; d++) {
            int digit = digitAt(value, digits, d);
            float glyphX = startX + d * BitmapFont.GLYPH_ADVANCE_CELLS * COUNT_CELL_SIZE;
            drawGlyph(digit, glyphX, startY);
        }
    }

    private void drawGlyph(int digit, float glyphX, float glyphY) {
        for (int row = 0; row < BitmapFont.GLYPH_ROWS; row++) {
            for (int col = 0; col < BitmapFont.GLYPH_COLS; col++) {
                if (!BitmapFont.isCellLit(digit, col, row)) continue;
                float x = glyphX + col * COUNT_CELL_SIZE;
                float y = glyphY + row * COUNT_CELL_SIZE;
                drawRect(new Rect(x, y, COUNT_CELL_SIZE, COUNT_CELL_SIZE), COUNT_COLOR, OPAQUE);
            }
        }
    }

    private static int digitAt(int value, int digits, int position) {
        int power = 1;
        for (int i = 0; i < digits - 1 - position; i++) power *= 10;
        return (value / power) % 10;
    }

    private void drawRect(Rect rect, float[] color, float alpha) {
        model.translation(rect.x(), rect.y(), 0f).scale(rect.w(), rect.h(), 1f);
        shader.setUniformMatrix4f("uModel", model);
        shader.setUniform3f("uColor", color[0], color[1], color[2]);
        shader.setUniform1f("uAlpha", alpha);
        glDrawArrays(GL_TRIANGLE_FAN, 0, 4);
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

    private static Inventory playerInventory(World world) {
        int[] players = world.query(Inventory.class);
        if (players.length == 0) return null;
        return world.get(new Entity(players[0]), Inventory.class).orElse(null);
    }

    private static Health playerHealth(World world) {
        int[] players = world.query(Health.class);
        if (players.length == 0) return null;
        return world.get(new Entity(players[0]), Health.class).orElse(null);
    }

    private static Hunger playerHunger(World world) {
        int[] players = world.query(Hunger.class);
        if (players.length == 0) return null;
        return world.get(new Entity(players[0]), Hunger.class).orElse(null);
    }

    private static int selectedSlot(World world) {
        int[] players = world.query(Hotbar.class);
        if (players.length == 0) return WorldConstants.NO_HOTBAR_SELECT;
        return world.get(new Entity(players[0]), Hotbar.class).map(Hotbar::selectedSlot)
                .orElse(WorldConstants.NO_HOTBAR_SELECT);
    }

    @Override
    public void close() {
        glDeleteBuffers(quadVbo);
        glDeleteVertexArrays(quadVao);
        shader.close();
    }
}
