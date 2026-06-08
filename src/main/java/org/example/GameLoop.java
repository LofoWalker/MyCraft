package org.example;

import org.example.ecs.SystemScheduler;
import org.example.ecs.World;

import static org.lwjgl.glfw.GLFW.glfwGetTime;
import static org.lwjgl.glfw.GLFW.glfwSetWindowTitle;

public final class GameLoop {

    private static final double FIXED_DT            = 1.0 / 60.0;
    private static final double MAX_FRAME_TIME       = 0.25;
    private static final double FPS_REPORT_INTERVAL  = 1.0;

    private GameLoop() {}

    // Package-private: pure timing logic — testable without GLFW
    static double runFixedSteps(double accumulator, double fixedDt, Runnable tick) {
        while (accumulator >= fixedDt) {
            tick.run();
            accumulator -= fixedDt;
        }
        return accumulator;
    }

    public static void run(Window window, World world,
                           SystemScheduler simScheduler, SystemScheduler renderScheduler) {
        double previous    = glfwGetTime();
        double accumulator = 0.0;
        double fpsTimer    = 0.0;
        int    frameCount  = 0;

        while (!window.shouldClose()) {
            double current = glfwGetTime();
            double elapsed = Math.min(current - previous, MAX_FRAME_TIME);
            previous = current;

            accumulator += elapsed;
            accumulator = runFixedSteps(accumulator, FIXED_DT,
                    () -> simScheduler.update(world, (float) FIXED_DT));

            window.clear();
            renderScheduler.update(world, (float) elapsed);
            window.swapBuffers();
            window.pollEvents();

            frameCount++;
            fpsTimer += elapsed;
            if (fpsTimer >= FPS_REPORT_INTERVAL) {
                glfwSetWindowTitle(window.getHandle(), "MyCraft | FPS: " + frameCount);
                frameCount = 0;
                fpsTimer  -= FPS_REPORT_INTERVAL;
            }
        }
    }
}
