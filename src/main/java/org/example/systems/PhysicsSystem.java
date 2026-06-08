package org.example.systems;

import org.example.components.Flying;
import org.example.components.Gravity;
import org.example.components.Velocity;
import org.example.ecs.Entity;
import org.example.ecs.GameSystem;
import org.example.ecs.World;
import org.example.world.WorldConstants;

public final class PhysicsSystem implements GameSystem {

    @Override
    public void update(World world, float dt) {
        for (int eid : world.query(Velocity.class, Gravity.class)) {
            Entity   entity = new Entity(eid);
            if (world.has(entity, Flying.class)) continue;
            Velocity vel    = world.get(entity, Velocity.class).orElseThrow();
            Gravity  grav   = world.get(entity, Gravity.class).orElseThrow();

            float vy = Math.max(vel.y() - grav.acceleration() * dt, WorldConstants.TERMINAL_VELOCITY);
            world.add(entity, new Velocity(vel.x(), vy, vel.z()));
        }
    }
}
