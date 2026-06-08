package org.example.systems;

import org.example.Window;
import org.example.components.PlayerInput;
import org.example.ecs.Entity;
import org.example.ecs.GameSystem;
import org.example.ecs.World;

import static org.lwjgl.glfw.GLFW.*;

public final class InputSystem implements GameSystem {

    @FunctionalInterface
    interface KeyQuery {
        int getKey(long window, int key);
    }

    private final long      windowHandle;
    private final KeyQuery  keyQuery;
    private final Runnable  closeAction;
    private float accumulatedDeltaX;
    private float accumulatedDeltaY;

    public InputSystem(Window window) {
        this.windowHandle = window.getHandle();
        this.keyQuery     = (win, key) -> glfwGetKey(win, key);
        this.closeAction  = () -> glfwSetWindowShouldClose(this.windowHandle, true);
        registerMouseDeltaCallback();
    }

    // Package-private: allows testing without GLFW
    InputSystem(KeyQuery keyQuery, Runnable closeAction) {
        this.windowHandle = 0L;
        this.keyQuery     = keyQuery;
        this.closeAction  = closeAction;
    }

    // Package-private: inject simulated mouse delta in tests
    void accumulateDelta(float dx, float dy) {
        accumulatedDeltaX += dx;
        accumulatedDeltaY += dy;
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

        boolean forward     = keyQuery.getKey(windowHandle, GLFW_KEY_W)     == GLFW_PRESS;
        boolean backward    = keyQuery.getKey(windowHandle, GLFW_KEY_S)     == GLFW_PRESS;
        boolean strafeLeft  = keyQuery.getKey(windowHandle, GLFW_KEY_A)     == GLFW_PRESS;
        boolean strafeRight = keyQuery.getKey(windowHandle, GLFW_KEY_D)     == GLFW_PRESS;
        boolean jump        = keyQuery.getKey(windowHandle, GLFW_KEY_SPACE) == GLFW_PRESS;

        for (int eid : world.query(PlayerInput.class)) {
            world.add(new Entity(eid), new PlayerInput(forward, backward, strafeLeft, strafeRight, jump, dx, dy));
        }
    }
}
