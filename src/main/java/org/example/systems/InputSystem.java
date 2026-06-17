package org.example.systems;

import org.example.Window;
import org.example.components.PlayerInput;
import org.example.ecs.Entity;
import org.example.ecs.GameSystem;
import org.example.ecs.World;
import org.example.world.WorldConstants;

import static org.lwjgl.glfw.GLFW.*;

public final class InputSystem implements GameSystem {

    @FunctionalInterface
    interface KeyQuery {
        int getKey(long window, int key);
    }

    @FunctionalInterface
    interface MouseButtonQuery {
        int getButton(long window, int button);
    }

    private final long             windowHandle;
    private final KeyQuery         keyQuery;
    private final MouseButtonQuery mouseButtonQuery;
    private final Runnable         closeAction;
    private float accumulatedDeltaX;
    private float accumulatedDeltaY;
    private int   accumulatedScroll;

    public InputSystem(Window window) {
        this.windowHandle     = window.getHandle();
        this.keyQuery         = (win, key) -> glfwGetKey(win, key);
        this.mouseButtonQuery = (win, button) -> glfwGetMouseButton(win, button);
        this.closeAction      = () -> glfwSetWindowShouldClose(this.windowHandle, true);
        registerMouseDeltaCallback();
        registerScrollCallback();
    }

    // Package-private: allows testing without GLFW
    InputSystem(KeyQuery keyQuery, Runnable closeAction) {
        this.windowHandle     = 0L;
        this.keyQuery         = keyQuery;
        this.mouseButtonQuery = (win, button) -> GLFW_RELEASE;
        this.closeAction      = closeAction;
    }

    // Package-private: inject simulated mouse delta in tests
    void accumulateDelta(float dx, float dy) {
        accumulatedDeltaX += dx;
        accumulatedDeltaY += dy;
    }

    // Package-private: inject simulated scroll ticks in tests
    void accumulateScroll(int ticks) {
        accumulatedScroll += ticks;
    }

    private void registerMouseDeltaCallback() {
        double[] lastPos = {Double.NaN, Double.NaN};
        glfwSetCursorPosCallback(windowHandle, (win, x, y) -> {
            if (!Double.isNaN(lastPos[0])) {
                accumulatedDeltaX += (float) (x - lastPos[0]);
                accumulatedDeltaY += (float) (y - lastPos[1]);
            }
            lastPos[0] = x;
            lastPos[1] = y;
        });
    }

    // Scroll up (yOffset > 0) advances the hotbar selection forward by one slot; scroll down steps
    // back. Accumulated across frames like the mouse delta, then consumed each tick.
    private void registerScrollCallback() {
        glfwSetScrollCallback(windowHandle, (win, xOffset, yOffset) ->
                accumulatedScroll += (int) Math.signum(yOffset));
    }

    @Override
    public void update(World world, float dt) {
        if (keyQuery.getKey(windowHandle, GLFW_KEY_ESCAPE) == GLFW_PRESS) {
            closeAction.run();
            return;
        }

        float dx = accumulatedDeltaX;
        float dy = accumulatedDeltaY;
        accumulatedDeltaX = 0;
        accumulatedDeltaY = 0;

        int scroll = accumulatedScroll;
        accumulatedScroll = 0;
        int hotbarSelect = readHotbarSelect();

        boolean forward     = keyQuery.getKey(windowHandle, GLFW_KEY_W)     == GLFW_PRESS;
        boolean backward    = keyQuery.getKey(windowHandle, GLFW_KEY_S)     == GLFW_PRESS;
        boolean strafeLeft  = keyQuery.getKey(windowHandle, GLFW_KEY_A)     == GLFW_PRESS;
        boolean strafeRight = keyQuery.getKey(windowHandle, GLFW_KEY_D)     == GLFW_PRESS;
        boolean jump        = keyQuery.getKey(windowHandle, GLFW_KEY_SPACE)        == GLFW_PRESS;
        boolean descend     = keyQuery.getKey(windowHandle, GLFW_KEY_LEFT_CONTROL) == GLFW_PRESS;
        boolean breakBlock  = mouseButtonQuery.getButton(windowHandle, GLFW_MOUSE_BUTTON_LEFT)  == GLFW_PRESS;
        boolean placeBlock  = mouseButtonQuery.getButton(windowHandle, GLFW_MOUSE_BUTTON_RIGHT) == GLFW_PRESS;

        for (int eid : world.query(PlayerInput.class)) {
            world.add(new Entity(eid), new PlayerInput(forward, backward, strafeLeft, strafeRight,
                    jump, descend, dx, dy, breakBlock, placeBlock, scroll, hotbarSelect));
        }
    }

    // Maps the held number key 1..9 to a hotbar slot index 0..8; NO_HOTBAR_SELECT when none is down.
    private int readHotbarSelect() {
        for (int slot = 0; slot < WorldConstants.HOTBAR_SLOTS; slot++) {
            if (keyQuery.getKey(windowHandle, GLFW_KEY_1 + slot) == GLFW_PRESS) return slot;
        }
        return WorldConstants.NO_HOTBAR_SELECT;
    }
}
