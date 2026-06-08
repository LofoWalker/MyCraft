package org.example;

import org.example.ecs.SystemScheduler;
import org.example.ecs.World;

public class Main {
    static void main(String[] args) {
        try (Window window = new Window(1920, 1080, "MyCraft")) {
            window.init();
            World world = new World();
            SystemScheduler scheduler = new SystemScheduler();
            GameLoop.run(window, world, scheduler);
        }
    }
}
