package org.example.systems;

import org.example.components.PlayerInput;
import org.example.components.Position;
import org.example.components.Rotation;
import org.example.ecs.Entity;
import org.example.ecs.GameSystem;
import org.example.ecs.World;

public final class MovementSystem implements GameSystem {

    private static final float MOVE_SPEED  = 5.0f;
    private static final float SENSITIVITY = 0.1f;
    private static final float MAX_PITCH   = 89.0f;

    @Override
    public void update(World world, float dt) {
        for (int eid : world.query(Position.class, Rotation.class, PlayerInput.class)) {
            Entity      entity = new Entity(eid);
            Position    pos    = world.get(entity, Position.class).orElseThrow();
            Rotation    rot    = world.get(entity, Rotation.class).orElseThrow();
            PlayerInput input  = world.get(entity, PlayerInput.class).orElseThrow();

            Rotation newRot = applyMouseLook(rot, input);
            world.add(entity, newRot);
            world.add(entity, applyMovement(pos, newRot, input, dt));
        }
    }

    private static Rotation applyMouseLook(Rotation rot, PlayerInput input) {
        float newYaw   = rot.yaw()   + input.mouseDeltaX() * SENSITIVITY;
        float newPitch = Math.clamp(
                rot.pitch() - input.mouseDeltaY() * SENSITIVITY,
                -MAX_PITCH, MAX_PITCH);
        return new Rotation(newYaw, newPitch);
    }

    private static Position applyMovement(Position pos, Rotation rot, PlayerInput input, float dt) {
        float yawRad   = (float) Math.toRadians(rot.yaw());
        float pitchRad = (float) Math.toRadians(rot.pitch());
        float cosPitch = (float) Math.cos(pitchRad);

        // Camera forward (pitch-aware, for true noclip)
        float fwdX  = (float)  Math.sin(yawRad) * cosPitch;
        float fwdY  = (float)  Math.sin(pitchRad);
        float fwdZ  = (float) -Math.cos(yawRad) * cosPitch;
        // Right = perpendicular to yaw, horizontal only
        float rightX = (float)  Math.cos(yawRad);
        float rightZ = (float)  Math.sin(yawRad);

        float dx = 0, dy = 0, dz = 0;
        if (input.forward())     { dx += fwdX; dy += fwdY; dz += fwdZ; }
        if (input.backward())    { dx -= fwdX; dy -= fwdY; dz -= fwdZ; }
        if (input.strafeLeft())  { dx -= rightX; dz -= rightZ; }
        if (input.strafeRight()) { dx += rightX; dz += rightZ; }
        if (input.jump())        { dy += 1; }

        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len > 0) { dx /= len; dy /= len; dz /= len; }

        return new Position(
                pos.x() + dx * MOVE_SPEED * dt,
                pos.y() + dy * MOVE_SPEED * dt,
                pos.z() + dz * MOVE_SPEED * dt);
    }
}
