package org.example.systems;

import org.example.components.Grounded;
import org.example.components.PlayerInput;
import org.example.components.Position;
import org.example.components.Rotation;
import org.example.components.Velocity;
import org.example.ecs.Entity;
import org.example.ecs.GameSystem;
import org.example.ecs.World;
import org.example.world.WorldConstants;

public final class MovementSystem implements GameSystem {

    private static final float MOVE_SPEED  = 6.0f;
    private static final float SENSITIVITY = 0.1f;
    private static final float MAX_PITCH   = 89.0f;

    private boolean prevJump = false;

    @Override
    public void update(World world, float dt) {
        for (int eid : world.query(Position.class, Velocity.class, Rotation.class, PlayerInput.class)) {
            Entity      entity = new Entity(eid);
            Position    pos    = world.get(entity, Position.class).orElseThrow();
            Velocity    vel    = world.get(entity, Velocity.class).orElseThrow();
            Rotation    rot    = world.get(entity, Rotation.class).orElseThrow();
            PlayerInput input  = world.get(entity, PlayerInput.class).orElseThrow();

            Rotation newRot = applyMouseLook(rot, input);
            world.add(entity, newRot);

            boolean grounded = world.has(entity, Grounded.class);
            Velocity newVel  = computeVelocity(vel, newRot, input, grounded);
            prevJump = input.jump();
            world.add(entity, newVel);

            world.add(entity, new Position(
                    pos.x() + newVel.x() * dt,
                    pos.y() + newVel.y() * dt,
                    pos.z() + newVel.z() * dt));
        }
    }

    private Velocity computeVelocity(Velocity vel, Rotation rot, PlayerInput input, boolean grounded) {
        float yawRad = (float) Math.toRadians(rot.yaw());
        float fwdX   = (float)  Math.sin(yawRad);
        float fwdZ   = (float) -Math.cos(yawRad);
        float rightX = (float)  Math.cos(yawRad);
        float rightZ = (float)  Math.sin(yawRad);

        float dx = 0, dz = 0;
        if (input.forward())     { dx += fwdX; dz += fwdZ; }
        if (input.backward())    { dx -= fwdX; dz -= fwdZ; }
        if (input.strafeLeft())  { dx -= rightX; dz -= rightZ; }
        if (input.strafeRight()) { dx += rightX; dz += rightZ; }

        float hLen = (float) Math.sqrt(dx * dx + dz * dz);
        float vx   = hLen > 0 ? (dx / hLen) * MOVE_SPEED : 0f;
        float vz   = hLen > 0 ? (dz / hLen) * MOVE_SPEED : 0f;

        // Rising-edge jump: only fires on first press and only when standing on ground
        float vy = vel.y();
        if (input.jump() && !prevJump && grounded) {
            vy = WorldConstants.JUMP_IMPULSE;
        }

        return new Velocity(vx, vy, vz);
    }

    private static Rotation applyMouseLook(Rotation rot, PlayerInput input) {
        float newYaw   = rot.yaw()   + input.mouseDeltaX() * SENSITIVITY;
        float newPitch = Math.clamp(
                rot.pitch() - input.mouseDeltaY() * SENSITIVITY,
                -MAX_PITCH, MAX_PITCH);
        return new Rotation(newYaw, newPitch);
    }
}
