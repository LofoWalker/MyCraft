package org.example.systems;

import org.example.components.Flying;
import org.example.components.Grounded;
import org.example.components.PlayerInput;
import org.example.components.Position;
import org.example.components.Rotation;
import org.example.components.Velocity;
import org.example.ecs.Entity;
import org.example.ecs.World;
import org.example.world.WorldConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MovementSystemTest {

    private World          world;
    private Entity         player;
    private MovementSystem system;

    @BeforeEach
    void setUp() {
        world  = new World();
        player = world.create();
        world.add(player, new Position(0f, 0f, 0f));
        world.add(player, new Rotation(0f, 0f));
        world.add(player, new Velocity(0f, 0f, 0f));
        system = new MovementSystem();
    }

    private void input(boolean fwd, boolean bwd, boolean left, boolean right, boolean jump, float dx, float dy) {
        world.add(player, new PlayerInput(fwd, bwd, left, right, jump, false, dx, dy, false, false));
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
        // yaw=0 → forward = (sin0, -cos0) = (0, -1) horizontal only
        input(true, false, false, false, false, 0f, 0f);
        system.update(world, 1.0f);
        Position pos = world.get(player, Position.class).orElseThrow();
        assertEquals(0f,   pos.x(), 1e-4f);
        assertEquals(0f,   pos.y(), 1e-4f);
        assertEquals(-5f, pos.z(), 1e-4f);  // MOVE_SPEED * dt = 50 * 1
    }

    @Test
    void backwardMovesOppositeToForward() {
        input(false, true, false, false, false, 0f, 0f);
        system.update(world, 1.0f);
        Position pos = world.get(player, Position.class).orElseThrow();
        assertEquals(5f, pos.z(), 1e-4f);
    }

    @Test
    void jumpAppliesImpulseOnFirstPressWhenGrounded() {
        world.add(player, new Grounded());
        input(false, false, false, false, true, 0f, 0f);
        system.update(world, 1.0f);
        Position pos = world.get(player, Position.class).orElseThrow();
        assertEquals(WorldConstants.JUMP_IMPULSE * 1.0f, pos.y(), 1e-4f);
    }

    @Test
    void jumpDoesNothingWhenNotGrounded() {
        // No Grounded component — jump key pressed but player is airborne
        input(false, false, false, false, true, 0f, 0f);
        system.update(world, 1.0f);
        Position pos = world.get(player, Position.class).orElseThrow();
        assertEquals(0f, pos.y(), 1e-4f);
    }

    @Test
    void jumpNotRetriggeredWhileHeld() {
        world.add(player, new Grounded());
        input(false, false, false, false, true, 0f, 0f);
        system.update(world, 1.0f);
        // Second frame: jump still held, prevJump=true → no new impulse; vy = JUMP_IMPULSE from Velocity store
        input(false, false, false, false, true, 0f, 0f);
        system.update(world, 1.0f);
        Position pos = world.get(player, Position.class).orElseThrow();
        assertEquals(WorldConstants.JUMP_IMPULSE * 2.0f, pos.y(), 1e-4f);
    }

    @Test
    void diagonalMovementIsNormalized() {
        // Forward + strafe right must give same total distance as straight movement
        input(true, false, false, true, false, 0f, 0f);
        system.update(world, 1.0f);
        Position pos = world.get(player, Position.class).orElseThrow();
        float dist = (float) Math.sqrt(pos.x() * pos.x() + pos.z() * pos.z());
        assertEquals(5f, dist, 1e-4f);
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
        input(false, false, false, false, false, 0f, -200f);
        system.update(world, 0.016f);
        Rotation rot = world.get(player, Rotation.class).orElseThrow();
        assertEquals(89f, rot.pitch(), 1e-4f);
    }

    @Test
    void pitchClampedAtNegativeMax() {
        world.add(player, new Rotation(0f, -80f));
        input(false, false, false, false, false, 0f, 200f);
        system.update(world, 0.016f);
        Rotation rot = world.get(player, Rotation.class).orElseThrow();
        assertEquals(-89f, rot.pitch(), 1e-4f);
    }

    @Test
    void flyingAscendsWhileSpaceHeld() {
        world.add(player, new Flying());
        input(false, false, false, false, true, 0f, 0f);
        system.update(world, 1.0f);
        Position pos = world.get(player, Position.class).orElseThrow();
        assertEquals(WorldConstants.FLY_VERTICAL_SPEED, pos.y(), 1e-4f);
    }

    @Test
    void flyingDescendsWhileControlHeld() {
        world.add(player, new Flying());
        world.add(player, new PlayerInput(false, false, false, false, false, true, 0f, 0f, false, false));
        system.update(world, 1.0f);
        Position pos = world.get(player, Position.class).orElseThrow();
        assertEquals(-WorldConstants.FLY_VERTICAL_SPEED, pos.y(), 1e-4f);
    }

    @Test
    void flyingHoversAndDiscardsFallingVelocity() {
        world.add(player, new Velocity(0f, -30f, 0f));
        world.add(player, new Flying());
        input(false, false, false, false, false, 0f, 0f);
        system.update(world, 1.0f);
        Position pos = world.get(player, Position.class).orElseThrow();
        assertEquals(0f, pos.y(), 1e-4f);
    }

    @Test
    void entityWithoutPlayerInputIsIgnored() {
        system.update(world, 1.0f);
        Position pos = world.get(player, Position.class).orElseThrow();
        assertEquals(0f, pos.x(), 1e-5f);
        assertEquals(0f, pos.y(), 1e-5f);
        assertEquals(0f, pos.z(), 1e-5f);
    }
}
