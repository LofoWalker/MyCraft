package org.example.ecs;

import java.util.ArrayList;
import java.util.List;

public class SystemScheduler {
    private final List<GameSystem> systems = new ArrayList<>();

    public void add(GameSystem system) {
        systems.add(system);
    }

    public void update(World world, float dt) {
        for (GameSystem system : systems) {
            system.update(world, dt);
        }
    }
}
