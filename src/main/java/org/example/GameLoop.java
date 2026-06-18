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

    /** Pure AppState transition logic — testable without GLFW. */
    static boolean simShouldRun(AppState state) {
        return state == AppState.IN_GAME;
    }

    static boolean menuShouldRun(AppState state) {
        return state == AppState.MAIN_MENU;
    }

    static boolean pauseShouldRun(AppState state) {
        return state == AppState.PAUSED;
    }

    public static void run(Window window, World world,
                           AppStateHolder stateHolder,
                           SystemScheduler simScheduler,
                           SystemScheduler renderScheduler,
                           SystemScheduler menuScheduler,
                           SystemScheduler pauseScheduler) {
        PerformanceMonitor perf = new PerformanceMonitor();
        double previous    = glfwGetTime();
        double accumulator = 0.0;
        double fpsTimer    = 0.0;
        int    frameCount  = 0;

        while (!window.shouldClose()) {
            stateHolder.advance();
            AppState state = stateHolder.current();

            double current = glfwGetTime();
            double elapsed = Math.min(current - previous, MAX_FRAME_TIME);
            previous = current;

            accumulator += elapsed;
            if (simShouldRun(state)) {
                accumulator = runFixedSteps(accumulator, FIXED_DT,
                        () -> simScheduler.update(world, (float) FIXED_DT));
            } else {
                // Drain the accumulator so it does not overflow when resuming.
                accumulator = 0.0;
            }

            window.clear();
            if (menuShouldRun(state)) {
                menuScheduler.update(world, (float) elapsed);
            } else if (pauseShouldRun(state)) {
                renderScheduler.update(world, (float) elapsed);
                pauseScheduler.update(world, (float) elapsed);
            } else {
                renderScheduler.update(world, (float) elapsed);
            }
            window.swapBuffers();
            window.pollEvents();

            frameCount++;
            fpsTimer += elapsed;
            if (fpsTimer >= FPS_REPORT_INTERVAL) {
                glfwSetWindowTitle(window.getHandle(), perf.title(frameCount));
                frameCount = 0;
                fpsTimer  -= FPS_REPORT_INTERVAL;
            }
        }
    }

    /** Legacy overload kept for backward compatibility with existing tests. */
    public static void run(Window window, World world,
                           SystemScheduler simScheduler, SystemScheduler renderScheduler) {
        AppStateHolder holder = new AppStateHolder(AppState.IN_GAME);
        SystemScheduler empty = new SystemScheduler();
        run(window, world, holder, simScheduler, renderScheduler, empty, empty);
    }
}
