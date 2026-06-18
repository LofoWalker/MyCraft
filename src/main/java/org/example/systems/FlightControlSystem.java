package org.example.systems;

import org.example.components.Flying;
import org.example.components.PlayerInput;
import org.example.ecs.Entity;
import org.example.ecs.GameSystem;
import org.example.ecs.World;
import org.example.world.WorldConstants;

import static org.example.systems.GameModeQuery.isCreative;

public final class FlightControlSystem implements GameSystem {

    private boolean prevJump   = false;
    private float   sinceLastTap = Float.MAX_VALUE;

    @Override
    public void update(World world, float dt) {
        sinceLastTap += dt;
        for (int eid : world.query(PlayerInput.class)) {
            Entity entity = new Entity(eid);
            // Creative mode: flying is always on. Ensure the Flying component is present and skip
            // the double-tap toggle so the player cannot accidentally ground themselves.
            if (isCreative(world, entity)) {
                if (!world.has(entity, Flying.class)) world.add(entity, new Flying());
                continue;
            }
            PlayerInput input  = world.get(entity, PlayerInput.class).orElseThrow();
            boolean risingEdge = input.jump() && !prevJump;
            if (risingEdge) registerTap(world, entity);
            prevJump = input.jump();
        }
    }

    private void registerTap(World world, Entity entity) {
        if (sinceLastTap <= WorldConstants.DOUBLE_TAP_WINDOW_SECONDS) {
            toggleFlying(world, entity);
            sinceLastTap = Float.MAX_VALUE; // consume the pair so a third quick tap starts fresh
        } else {
            sinceLastTap = 0f;
        }
    }

    private static void toggleFlying(World world, Entity entity) {
        if (world.has(entity, Flying.class)) {
            world.remove(entity, Flying.class);
        } else {
            world.add(entity, new Flying());
        }
    }
}
