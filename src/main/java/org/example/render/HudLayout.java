package org.example.render;

import org.example.world.WorldConstants;

/**
 * Pure 2D layout math for the HUD, expressed in screen pixels with the origin at the
 * top-left corner (y grows downward). No OpenGL, no JOML state — every method is a plain
 * function of the screen size, so the geometry can be unit-tested without a GL context.
 *
 * <p>A {@link Rect} is an axis-aligned rectangle (x, y = top-left corner; w, h = size).
 */
public final class HudLayout {

    // Crosshair: a thin plus sign centered on the screen.
    public static final float CROSSHAIR_LENGTH    = 18f;
    public static final float CROSSHAIR_THICKNESS = 2f;

    // Hotbar: nine square cells with a uniform gap, sitting a fixed margin above the bottom edge.
    public static final float SLOT_SIZE          = 56f;
    public static final float SLOT_GAP           = 6f;
    public static final float HOTBAR_BOTTOM_MARGIN = 24f;
    // Item preview is inset inside its slot so the slot border stays visible around it.
    public static final float ITEM_INSET         = 8f;
    // The selected slot grows by this much on every side to read as "highlighted".
    public static final float SELECTION_GROW      = 4f;

    private HudLayout() {}

    public record Rect(float x, float y, float w, float h) {

        public float centerX() { return x + w * 0.5f; }
        public float centerY() { return y + h * 0.5f; }
    }

    public static Rect crosshairHorizontal(int screenWidth, int screenHeight) {
        float cx = screenWidth * 0.5f;
        float cy = screenHeight * 0.5f;
        return centered(cx, cy, CROSSHAIR_LENGTH, CROSSHAIR_THICKNESS);
    }

    public static Rect crosshairVertical(int screenWidth, int screenHeight) {
        float cx = screenWidth * 0.5f;
        float cy = screenHeight * 0.5f;
        return centered(cx, cy, CROSSHAIR_THICKNESS, CROSSHAIR_LENGTH);
    }

    public static float hotbarWidth() {
        int slots = WorldConstants.HOTBAR_SLOTS;
        return slots * SLOT_SIZE + (slots - 1) * SLOT_GAP;
    }

    /** Top-left-anchored cell rectangle for the given hotbar slot (0-based). */
    public static Rect slot(int index, int screenWidth, int screenHeight) {
        float originX = (screenWidth - hotbarWidth()) * 0.5f;
        float y       = screenHeight - HOTBAR_BOTTOM_MARGIN - SLOT_SIZE;
        float x       = originX + index * (SLOT_SIZE + SLOT_GAP);
        return new Rect(x, y, SLOT_SIZE, SLOT_SIZE);
    }

    /** The slot rectangle grown on every side, used to outline the selected slot. */
    public static Rect selectionFrame(int index, int screenWidth, int screenHeight) {
        Rect base = slot(index, screenWidth, screenHeight);
        return new Rect(base.x() - SELECTION_GROW, base.y() - SELECTION_GROW,
                base.w() + 2 * SELECTION_GROW, base.h() + 2 * SELECTION_GROW);
    }

    /** The item-preview rectangle inset inside its slot. */
    public static Rect itemPreview(int index, int screenWidth, int screenHeight) {
        Rect base = slot(index, screenWidth, screenHeight);
        return new Rect(base.x() + ITEM_INSET, base.y() + ITEM_INSET,
                base.w() - 2 * ITEM_INSET, base.h() - 2 * ITEM_INSET);
    }

    private static Rect centered(float cx, float cy, float w, float h) {
        return new Rect(cx - w * 0.5f, cy - h * 0.5f, w, h);
    }
}
