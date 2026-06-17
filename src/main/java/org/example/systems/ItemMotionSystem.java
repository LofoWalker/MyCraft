package org.example.systems;

import org.example.components.Grounded;
import org.example.components.ItemEntity;
import org.example.components.Position;
import org.example.components.Velocity;
import org.example.ecs.Entity;
import org.example.ecs.GameSystem;
import org.example.ecs.World;
import org.example.world.WorldConstants;

// Integrates dropped items' Position from their Velocity. The player's MovementSystem only advances
// player-controlled entities, so item drops need their own integration step before CollisionSystem
// (which expects Position to already be advanced and back-computes the previous cell). Items keep the
// gravity applied by PhysicsSystem; horizontal motion is damped once they rest on the ground so they
// quickly come to a stop instead of sliding.
public final class ItemMotionSystem implements GameSystem {

    @Override
    public void update(World world, float dt) {
        for (int eid : world.query(ItemEntity.class, Position.class, Velocity.class)) {
            Entity   item = new Entity(eid);
            Position pos  = world.get(item, Position.class).orElseThrow();
            Velocity vel  = world.get(item, Velocity.class).orElseThrow();

            Velocity damped = applyGroundFriction(world, item, vel);
            world.add(item, damped);
            world.add(item, new Position(
                    pos.x() + damped.x() * dt,
                    pos.y() + damped.y() * dt,
                    pos.z() + damped.z() * dt));
        }
    }

    private static Velocity applyGroundFriction(World world, Entity item, Velocity vel) {
        if (!world.has(item, Grounded.class)) return vel;
        float keep = 1f - WorldConstants.ITEM_GROUND_FRICTION;
        return new Velocity(vel.x() * keep, vel.y(), vel.z() * keep);
    }
}
