package org.example.systems;

import org.example.Window;
import org.example.components.CraftingGrid;
import org.example.components.Inventory;
import org.example.components.InventoryScreen;
import org.example.components.ItemStack;
import org.example.components.PlayerInput;
import org.example.ecs.Entity;
import org.example.ecs.GameSystem;
import org.example.ecs.World;
import org.example.render.BitmapFont;
import org.example.render.HudLayout;
import org.example.render.HudLayout.Rect;
import org.example.render.Shader;
import org.example.world.BlockType;
import org.example.world.Inventories;
import org.example.world.RecipeBook;
import org.example.world.WorldConstants;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.util.Optional;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;

// Inventory screen: opened/closed by pressing E. While open, the cursor is free and movement/block
// interaction is suspended. Draws the player's bag slots, hotbar strip, crafting grid, and result
// slot as coloured quads (same pipeline as HudSystem). Click handling picks up and places stacks;
// each grid change re-evaluates RecipeBook to update the craft result.
// Implements AutoCloseable so Main can close it with a try-with-resources.
public final class InventoryScreenSystem implements GameSystem, AutoCloseable {

    // --- Layout constants (all in screen pixels, origin top-left) ---
    private static final float PANEL_PADDING    = 16f;
    private static final float CELL_SIZE        = 48f;
    private static final float CELL_GAP         = 6f;
    private static final float SECTION_GAP      = 14f;
    private static final float RESULT_CELL_SIZE = 56f;
    private static final float ARROW_SIZE       = 24f;

    // Background panel colours.
    private static final float[] PANEL_COLOR   = { 0.12f, 0.12f, 0.12f };
    private static final float   PANEL_ALPHA   = 0.88f;
    private static final float[] SLOT_COLOR    = { 0.22f, 0.22f, 0.22f };
    private static final float   SLOT_ALPHA    = 1.0f;
    private static final float[] HOVER_COLOR   = { 0.35f, 0.35f, 0.35f };
    private static final float[] ITEM_COLOR_FALLBACK = { 0.60f, 0.55f, 0.50f };
    private static final float[] RESULT_COLOR  = { 0.28f, 0.22f, 0.10f };
    private static final float[] ARROW_COLOR   = { 0.65f, 0.55f, 0.20f };
    private static final float   OPAQUE        = 1.0f;

    // Item-count text constants.
    private static final float COUNT_CELL = 3f;
    private static final float COUNT_PAD  = 4f;
    private static final float[] COUNT_COLOR = { 1f, 1f, 1f };

    // Mouse button state for edge detection.
    private boolean leftClickHeldPreviously;
    // Tracks whether the screen was open on the previous tick to detect newly-opened states
    // (e.g. opened by BlockInteractionSystem via right-click on a crafting table).
    private boolean wasScreenOpenPreviously;

    private final Window  window;
    private final Shader  shader;
    private final int     quadVao;
    private final int     quadVbo;

    private final Matrix4f projection = new Matrix4f();
    private final Matrix4f model      = new Matrix4f();

    public InventoryScreenSystem(Window window) {
        this.window = window;
        this.shader = Shader.fromResources("/shaders/hud.vert", "/shaders/hud.frag");
        this.quadVao = glGenVertexArrays();
        this.quadVbo = glGenBuffers();
        uploadUnitQuad();
    }

    private void uploadUnitQuad() {
        glBindVertexArray(quadVao);
        FloatBuffer buf = MemoryUtil.memAllocFloat(8);
        try {
            buf.put(new float[]{ 0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f }).flip();
            glBindBuffer(GL_ARRAY_BUFFER, quadVbo);
            glBufferData(GL_ARRAY_BUFFER, buf, GL_STATIC_DRAW);
        } finally {
            MemoryUtil.memFree(buf);
        }
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 2 * Float.BYTES, 0L);
        glEnableVertexAttribArray(0);
        glBindVertexArray(0);
    }

    @Override
    public void update(World world, float dt) {
        int[] playerIds = world.query(PlayerInput.class, Inventory.class);
        if (playerIds.length == 0) return;

        Entity player = new Entity(playerIds[0]);
        PlayerInput input = world.get(player, PlayerInput.class).orElseThrow();

        handleToggle(world, player, input);

        Optional<InventoryScreen> screenOpt = world.get(player, InventoryScreen.class);
        // If the screen was just added by another system (e.g. right-click on crafting table),
        // ensure the cursor is freed and the crafting grid is initialised.
        boolean isOpenNow = screenOpt.isPresent();
        if (isOpenNow && !wasScreenOpenPreviously) {
            InventoryScreen screen0 = screenOpt.get();
            int size = screen0.craftingTableOpen()
                    ? WorldConstants.CRAFTING_GRID_LARGE
                    : WorldConstants.CRAFTING_GRID_SMALL;
            if (!world.has(player, CraftingGrid.class)) {
                world.add(player, CraftingGrid.empty(size));
            }
            window.freeCursor();
        }
        wasScreenOpenPreviously = isOpenNow;
        if (screenOpt.isEmpty()) return;

        InventoryScreen screen = updateCursor(world, player, screenOpt.get());
        screen = handleClick(world, player, screen);
        world.add(player, screen);

        render(world, player, screen);
    }

    // --- Toggle and cursor capture ---

    private void handleToggle(World world, Entity player, PlayerInput input) {
        if (!input.toggleInventory()) return;

        if (world.has(player, InventoryScreen.class)) {
            closeScreen(world, player);
        } else {
            openScreen(world, player, false);
        }
    }

    private void openScreen(World world, Entity player, boolean craftingTable) {
        world.add(player, InventoryScreen.open(craftingTable));
        // Ensure a crafting grid of the right size exists.
        int size = craftingTable
                ? WorldConstants.CRAFTING_GRID_LARGE
                : WorldConstants.CRAFTING_GRID_SMALL;
        if (!world.has(player, CraftingGrid.class)) {
            world.add(player, CraftingGrid.empty(size));
        } else {
            CraftingGrid existing = world.get(player, CraftingGrid.class).orElseThrow();
            if (existing.size() != size) {
                returnGridToInventory(world, player, existing);
                world.add(player, CraftingGrid.empty(size));
            }
        }
        window.freeCursor();
    }

    void closeScreen(World world, Entity player) {
        // Return held item to inventory before removing the screen component.
        world.get(player, InventoryScreen.class).ifPresent(screen -> {
            if (!screen.heldStack().isEmpty()) {
                world.get(player, Inventory.class).ifPresent(inv ->
                        world.add(player, Inventories.add(inv, screen.heldStack()).inventory()));
            }
        });
        world.get(player, CraftingGrid.class).ifPresent(grid -> {
            returnGridToInventory(world, player, grid);
            world.remove(player, CraftingGrid.class);
        });
        world.remove(player, InventoryScreen.class);
        window.captureCursor();
    }

    // Spills all crafting grid items back into the player's inventory (or drops them on full inv).
    private static void returnGridToInventory(World world, Entity player, CraftingGrid grid) {
        Inventory inv = world.get(player, Inventory.class).orElse(Inventories.empty());
        for (ItemStack slot : grid.slots()) {
            if (!slot.isEmpty()) {
                Inventories.AddResult result = Inventories.add(inv, slot);
                inv = result.inventory();
                // remainder is dropped — simplification for now (full inventory edge case).
            }
        }
        world.add(player, inv);
    }

    private InventoryScreen updateCursor(World world, Entity player, InventoryScreen screen) {
        double cx = window.getCursorX();
        double cy = window.getCursorY();
        return new InventoryScreen(cx, cy, screen.heldStack(), screen.craftingTableOpen(),
                screen.furnaceEntityId(), screen.chestEntityId());
    }

    // --- Click handling ---

    private InventoryScreen handleClick(World world, Entity player, InventoryScreen screen) {
        boolean leftDown = glfwGetMouseButton(window.getHandle(), GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS;
        boolean justClicked = leftDown && !leftClickHeldPreviously;
        leftClickHeldPreviously = leftDown;
        if (!justClicked) return screen;

        int width  = window.getWidth();
        int height = window.getHeight();
        float mx = (float) screen.cursorX();
        float my = (float) screen.cursorY();

        Layout layout = new Layout(width, height, craftGridSize(screen));

        // Check inventory bag slots (rows 1-3, i.e. slots 9-35).
        for (int row = 0; row < BACKPACK_ROWS; row++) {
            for (int col = 0; col < WorldConstants.HOTBAR_SLOTS; col++) {
                int slot = WorldConstants.HOTBAR_SLOTS + row * WorldConstants.HOTBAR_SLOTS + col;
                Rect r = layout.bagSlot(row, col);
                if (contains(r, mx, my)) {
                    return clickInventorySlot(world, player, screen, slot);
                }
            }
        }

        // Check hotbar strip at bottom of panel.
        for (int col = 0; col < WorldConstants.HOTBAR_SLOTS; col++) {
            Rect r = layout.hotbarSlot(col);
            if (contains(r, mx, my)) {
                return clickInventorySlot(world, player, screen, col);
            }
        }

        // Check crafting grid slots.
        CraftingGrid grid = world.get(player, CraftingGrid.class).orElse(null);
        if (grid != null) {
            int size = grid.size();
            for (int row = 0; row < size; row++) {
                for (int col = 0; col < size; col++) {
                    Rect r = layout.craftSlot(row, col);
                    if (contains(r, mx, my)) {
                        return clickCraftSlot(world, player, screen, grid, row * size + col);
                    }
                }
            }
            // Check result slot.
            Rect resultRect = layout.resultSlot();
            if (contains(resultRect, mx, my) && !grid.craftResult().isEmpty()) {
                return takeCraftResult(world, player, screen, grid);
            }
        }

        return screen;
    }

    // Swaps the held stack with the inventory slot stack (standard pick-up / put-down).
    private InventoryScreen clickInventorySlot(World world, Entity player,
                                               InventoryScreen screen, int slot) {
        Inventory inv = world.get(player, Inventory.class).orElse(Inventories.empty());
        ItemStack slotStack = Inventories.get(inv, slot);
        ItemStack held      = screen.heldStack();

        ItemStack[] slots = inv.slots().clone();
        slots[slot] = held;
        world.add(player, new Inventory(slots));
        return new InventoryScreen(screen.cursorX(), screen.cursorY(), slotStack,
                screen.craftingTableOpen(), screen.furnaceEntityId(), screen.chestEntityId());
    }

    // Swaps the held stack with a crafting grid cell, then re-evaluates the recipe.
    private InventoryScreen clickCraftSlot(World world, Entity player, InventoryScreen screen,
                                           CraftingGrid grid, int index) {
        ItemStack slotStack = grid.slots()[index];
        ItemStack held      = screen.heldStack();

        ItemStack[] newSlots = grid.slots().clone();
        newSlots[index] = held;

        CraftingGrid updated = recomputeResult(newSlots, grid.size());
        world.add(player, updated);
        return new InventoryScreen(screen.cursorX(), screen.cursorY(), slotStack,
                screen.craftingTableOpen(), screen.furnaceEntityId(), screen.chestEntityId());
    }

    // Takes the craft result: consumes one of each ingredient and gives the result to the held stack.
    private InventoryScreen takeCraftResult(World world, Entity player, InventoryScreen screen,
                                            CraftingGrid grid) {
        ItemStack result = grid.craftResult();
        ItemStack[] newSlots = consumeIngredients(grid.slots().clone());
        CraftingGrid updated = recomputeResult(newSlots, grid.size());
        world.add(player, updated);
        return new InventoryScreen(screen.cursorX(), screen.cursorY(), result,
                screen.craftingTableOpen(), screen.furnaceEntityId(), screen.chestEntityId());
    }

    // Decrements each non-empty cell by one (removes it if count reaches zero).
    private static ItemStack[] consumeIngredients(ItemStack[] slots) {
        for (int i = 0; i < slots.length; i++) {
            if (slots[i].isEmpty()) continue;
            int left = slots[i].count() - 1;
            slots[i] = left > 0 ? new ItemStack(slots[i].itemId(), left, slots[i].durability())
                                 : ItemStack.EMPTY;
        }
        return slots;
    }

    // Runs RecipeBook.match on the grid and returns a new CraftingGrid with the updated craftResult.
    private static CraftingGrid recomputeResult(ItemStack[] slots, int size) {
        int[] idGrid = new int[slots.length];
        for (int i = 0; i < slots.length; i++) {
            idGrid[i] = slots[i].isEmpty() ? 0 : slots[i].itemId();
        }
        Optional<ItemStack> match = RecipeBook.get().match(idGrid, size, size);
        ItemStack result = match.orElse(ItemStack.EMPTY);
        return new CraftingGrid(slots, size, result);
    }

    // --- Rendering ---

    private void render(World world, Entity player, InventoryScreen screen) {
        int width  = window.getWidth();
        int height = window.getHeight();
        projection.setOrtho(0f, width, height, 0f, -1f, 1f);

        beginOverlay();
        shader.bind();
        shader.setUniformMatrix4f("uProjection", projection);
        glBindVertexArray(quadVao);

        Layout layout = new Layout(width, height, craftGridSize(screen));

        drawPanel(layout.panel());
        drawInventorySlots(world, player, layout, screen);
        drawCraftingSection(world, player, layout, screen);
        drawHeldItem(screen);

        glBindVertexArray(0);
        shader.unbind();
        endOverlay();
    }

    private void drawPanel(Rect panel) {
        drawRect(panel, PANEL_COLOR, PANEL_ALPHA);
    }

    private void drawInventorySlots(World world, Entity player, Layout layout,
                                    InventoryScreen screen) {
        Inventory inv = world.get(player, Inventory.class).orElse(Inventories.empty());
        float mx = (float) screen.cursorX();
        float my = (float) screen.cursorY();

        // Backpack rows (slots 9–35).
        for (int row = 0; row < BACKPACK_ROWS; row++) {
            for (int col = 0; col < WorldConstants.HOTBAR_SLOTS; col++) {
                int slot = WorldConstants.HOTBAR_SLOTS + row * WorldConstants.HOTBAR_SLOTS + col;
                Rect r = layout.bagSlot(row, col);
                float[] bg = contains(r, mx, my) ? HOVER_COLOR : SLOT_COLOR;
                drawRect(r, bg, SLOT_ALPHA);
                drawItemInRect(inv.slots()[slot], r);
            }
        }

        // Hotbar strip (slots 0–8).
        for (int col = 0; col < WorldConstants.HOTBAR_SLOTS; col++) {
            Rect r = layout.hotbarSlot(col);
            float[] bg = contains(r, mx, my) ? HOVER_COLOR : SLOT_COLOR;
            drawRect(r, bg, SLOT_ALPHA);
            drawItemInRect(inv.slots()[col], r);
        }
    }

    private void drawCraftingSection(World world, Entity player, Layout layout,
                                     InventoryScreen screen) {
        CraftingGrid grid = world.get(player, CraftingGrid.class).orElse(null);
        if (grid == null) return;

        float mx = (float) screen.cursorX();
        float my = (float) screen.cursorY();
        int size = grid.size();

        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                Rect r = layout.craftSlot(row, col);
                float[] bg = contains(r, mx, my) ? HOVER_COLOR : SLOT_COLOR;
                drawRect(r, bg, SLOT_ALPHA);
                drawItemInRect(grid.slots()[row * size + col], r);
            }
        }

        // Arrow.
        drawRect(layout.arrow(), ARROW_COLOR, OPAQUE);

        // Result slot.
        Rect resultRect = layout.resultSlot();
        ItemStack result = grid.craftResult();
        float[] resultBg = result.isEmpty() ? SLOT_COLOR
                : (contains(resultRect, mx, my) ? HOVER_COLOR : RESULT_COLOR);
        drawRect(resultRect, resultBg, SLOT_ALPHA);
        drawItemInRect(result, resultRect);
    }

    private void drawHeldItem(InventoryScreen screen) {
        if (screen.heldStack().isEmpty()) return;
        float cx = (float) screen.cursorX();
        float cy = (float) screen.cursorY();
        float half = CELL_SIZE * 0.5f;
        Rect r = new Rect(cx - half, cy - half, CELL_SIZE, CELL_SIZE);
        drawItemInRect(screen.heldStack(), r);
    }

    private void drawItemInRect(ItemStack stack, Rect r) {
        if (stack.isEmpty()) return;
        float inset = CELL_SIZE * 0.12f;
        Rect inner = new Rect(r.x() + inset, r.y() + inset,
                r.w() - 2 * inset, r.h() - 2 * inset);
        float[] color = itemColor(stack.itemId());
        drawRect(inner, color, OPAQUE);
        if (stack.count() > 1) drawCount(stack.count(), r);
    }

    private static float[] itemColor(int itemId) {
        if (itemId >= 0 && itemId <= WorldConstants.MAX_BLOCK_ID) {
            return BlockType.byId((byte) itemId).colorTop();
        }
        return ITEM_COLOR_FALLBACK;
    }

    private void drawCount(int count, Rect slot) {
        float textWidth = BitmapFont.measureWidth(count, COUNT_CELL);
        float startX = slot.x() + slot.w() - COUNT_PAD - textWidth;
        float startY = slot.y() + slot.h() - COUNT_PAD - BitmapFont.GLYPH_ROWS * COUNT_CELL;
        drawNumber(count, startX, startY);
    }

    private void drawNumber(int value, float startX, float startY) {
        int digits = BitmapFont.digitCount(value);
        for (int d = 0; d < digits; d++) {
            int digit = digitAt(value, digits, d);
            float gx = startX + d * BitmapFont.GLYPH_ADVANCE_CELLS * COUNT_CELL;
            drawGlyph(digit, gx, startY);
        }
    }

    private void drawGlyph(int digit, float gx, float gy) {
        for (int row = 0; row < BitmapFont.GLYPH_ROWS; row++) {
            for (int col = 0; col < BitmapFont.GLYPH_COLS; col++) {
                if (!BitmapFont.isCellLit(digit, col, row)) continue;
                float x = gx + col * COUNT_CELL;
                float y = gy + row * COUNT_CELL;
                drawRect(new Rect(x, y, COUNT_CELL, COUNT_CELL), COUNT_COLOR, OPAQUE);
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

    private static boolean contains(Rect r, float mx, float my) {
        return mx >= r.x() && mx < r.x() + r.w()
            && my >= r.y() && my < r.y() + r.h();
    }

    private static int craftGridSize(InventoryScreen screen) {
        return screen.craftingTableOpen()
                ? WorldConstants.CRAFTING_GRID_LARGE
                : WorldConstants.CRAFTING_GRID_SMALL;
    }

    @Override
    public void close() {
        glDeleteBuffers(quadVbo);
        glDeleteVertexArrays(quadVao);
        shader.close();
    }

    // --- Layout helper (all sizes computed once per frame from screen dimensions) ---

    private static final int BACKPACK_ROWS = WorldConstants.BACKPACK_SLOTS / WorldConstants.HOTBAR_SLOTS;

    // Computes slot rectangles for the inventory panel.
    // Panel is centred horizontally and placed in the upper half of the screen.
    private static final class Layout {

        private final float panelX;
        private final float panelY;
        private final float bagOriginX;
        private final float bagOriginY;
        private final float hotbarOriginX;
        private final float hotbarOriginY;
        private final float craftOriginX;
        private final float craftOriginY;
        private final int   craftSize;
        private final Rect  panelRect;

        Layout(int screenW, int screenH, int craftSize) {
            this.craftSize = craftSize;

            // Inventory section width: 9 columns.
            float invSectionW = WorldConstants.HOTBAR_SLOTS * (CELL_SIZE + CELL_GAP) - CELL_GAP;
            // Craft section width: craftSize columns + arrow + result.
            float craftSectionW = craftSize * (CELL_SIZE + CELL_GAP) - CELL_GAP
                    + ARROW_SIZE + CELL_GAP + RESULT_CELL_SIZE;

            float panelW = Math.max(invSectionW, craftSectionW) + 2 * PANEL_PADDING;
            float invSectionH = (BACKPACK_ROWS + 1) * (CELL_SIZE + CELL_GAP) - CELL_GAP;
            float craftSectionH = craftSize * (CELL_SIZE + CELL_GAP) - CELL_GAP;
            float panelH = invSectionH + SECTION_GAP + craftSectionH + 2 * PANEL_PADDING;

            this.panelX = (screenW - panelW) * 0.5f;
            this.panelY = (screenH - panelH) * 0.5f;
            this.panelRect = new Rect(panelX, panelY, panelW, panelH);

            this.bagOriginX  = panelX + PANEL_PADDING;
            this.bagOriginY  = panelY + PANEL_PADDING;

            this.hotbarOriginX = bagOriginX;
            this.hotbarOriginY = bagOriginY + BACKPACK_ROWS * (CELL_SIZE + CELL_GAP);

            this.craftOriginX = panelX + PANEL_PADDING;
            this.craftOriginY = hotbarOriginY + CELL_SIZE + CELL_GAP + SECTION_GAP;
        }

        Rect panel() { return panelRect; }

        Rect bagSlot(int row, int col) {
            return new Rect(bagOriginX + col * (CELL_SIZE + CELL_GAP),
                            bagOriginY + row * (CELL_SIZE + CELL_GAP),
                            CELL_SIZE, CELL_SIZE);
        }

        Rect hotbarSlot(int col) {
            return new Rect(hotbarOriginX + col * (CELL_SIZE + CELL_GAP),
                            hotbarOriginY, CELL_SIZE, CELL_SIZE);
        }

        Rect craftSlot(int row, int col) {
            return new Rect(craftOriginX + col * (CELL_SIZE + CELL_GAP),
                            craftOriginY + row * (CELL_SIZE + CELL_GAP),
                            CELL_SIZE, CELL_SIZE);
        }

        Rect arrow() {
            float ax = craftOriginX + craftSize * (CELL_SIZE + CELL_GAP);
            float ay = craftOriginY + (craftSize * (CELL_SIZE + CELL_GAP) - ARROW_SIZE) * 0.5f;
            return new Rect(ax, ay, ARROW_SIZE, ARROW_SIZE);
        }

        Rect resultSlot() {
            float rx = craftOriginX + craftSize * (CELL_SIZE + CELL_GAP) + ARROW_SIZE + CELL_GAP;
            float ry = craftOriginY + (craftSize * (CELL_SIZE + CELL_GAP) - RESULT_CELL_SIZE) * 0.5f;
            return new Rect(rx, ry, RESULT_CELL_SIZE, RESULT_CELL_SIZE);
        }
    }
}
