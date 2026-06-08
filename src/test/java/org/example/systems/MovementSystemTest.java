package org.example.systems;

import org.example.components.PlayerInput;
import org.example.components.Position;
import org.example.components.Rotation;
import org.example.ecs.Entity;
import org.example.ecs.World;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MovementSystemTest {

    private World world;
    private Entity player;
    private MovementSystem system;

    @BeforeEach
    void setUp() {
        world  = new World();
        player = world.create();
        world.add(player, new Position(0f, 0f, 0f));
        world.add(player, new Rotation(0f, 0f));
        system = new MovementSystem();
    }

    private void input(boolean fwd, boolean bwd, boolean left, boolean right, boolean jump, float dx, float dy) {
        world.add(player, new PlayerInput(fwd, bwd, left, right, jump, dx, dy));
    }

    @Test
    void noInputLeavesPositionUnchanged() {
        input(false, false, false, false, false, 0f, 0f);
        system.update(world, 1.0f);
        Position pos = world.get(player, Position.class).orElseThrow();
        assertEquals(0f, pos.x(), 1e-5f);
        assertEquals(0f, pos.y(), 1e-5f);
        assertEquals(0f, pos.z(), 1e-5f);
    }

    @Test
    void forwardAtYawZeroMovesNegativeZ() {
        // yaw=0 → forward = (sin0, 0, -cos0) = (0, 0, -1)
        input(true, false, false, false, false, 0f, 0f);
        system.update(world, 1.0f);
        Position pos = world.get(player, Position.class).orElseThrow();
        assertEquals(0f,   pos.x(), 1e-4f);
        assertEquals(0f,   pos.y(), 1e-4f);
        assertEquals(-50f, pos.z(), 1e-4f);  // MOVE_SPEED * dt = 50 * 1
    }

    @Test
    void backwardMovesOppositeToForward() {
        input(false, true, false, false, false, 0f, 0f);
        system.update(world, 1.0f);
        Position pos = world.get(player, Position.class).orElseThrow();
        assertEquals(50f, pos.z(), 1e-4f);
    }

    @Test
    void jumpMovesUp() {
        input(false, false, false, false, true, 0f, 0f);
        system.update(world, 1.0f);
        Position pos = world.get(player, Position.class).orElseThrow();
        assertEquals(50f, pos.y(), 1e-4f);
    }

    @Test
    void diagonalMovementIsNormalized() {
        // Forward + strafe right must give same total distance as straight movement
        input(true, false, false, true, false, 0f, 0f);
        system.update(world, 1.0f);
        Position pos = world.get(player, Position.class).orElseThrow();
        float dist = (float) Math.sqrt(pos.x() * pos.x() + pos.y() * pos.y() + pos.z() * pos.z());
        assertEquals(50f, dist, 1e-4f);
    }

    @Test
    void mouseLookIncreasesYawOnPositiveDeltaX() {
        input(false, false, false, false, false, 10f, 0f);
        system.update(world, 0.016f);
        Rotation rot = world.get(player, Rotation.class).orElseThrow();
        // yaw += mouseDeltaX * sensitivity(0.1) = 1.0°
        assertEquals(1.0f, rot.yaw(), 1e-4f);
    }

    @Test
    void pitchClampedAtPositiveMax() {
        world.add(player, new Rotation(0f, 80f));
        input(false, false, false, false, false, 0f, -200f);  // large upward mouse
        system.update(world, 0.016f);
        Rotation rot = world.get(player, Rotation.class).orElseThrow();
        assertEquals(89f, rot.pitch(), 1e-4f);
    }

    @Test
    void pitchClampedAtNegativeMax() {
        world.add(player, new Rotation(0f, -80f));
        input(false, false, false, false, false, 0f, 200f);  // large downward mouse
        system.update(world, 0.016f);
        Rotation rot = world.get(player, Rotation.class).orElseThrow();
        assertEquals(-89f, rot.pitch(), 1e-4f);
    }

    @Test
    void entityWithoutPlayerInputIsIgnored() {
        // No PlayerInput component added — position must stay unchanged
        system.update(world, 1.0f);
        Position pos = world.get(player, Position.class).orElseThrow();
        assertEquals(0f, pos.x(), 1e-5f);
    }
}
