package org.example.systems;

import org.example.components.Flying;
import org.example.components.Gravity;
import org.example.components.Position;
import org.example.components.Velocity;
import org.example.ecs.Entity;
import org.example.ecs.World;
import org.example.world.WorldConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PhysicsSystemTest {

    private World         world;
    private Entity        entity;
    private PhysicsSystem system;

    @BeforeEach
    void setUp() {
        world  = new World();
        entity = world.create();
        world.add(entity, new Velocity(0f, 0f, 0f));
        world.add(entity, new Gravity(WorldConstants.GRAVITY));
        system = new PhysicsSystem();
    }

    @Test
    void gravityReducesVerticalVelocity() {
        system.update(world, 1.0f);
        Velocity vel = world.get(entity, Velocity.class).orElseThrow();
        assertEquals(-WorldConstants.GRAVITY, vel.y(), 1e-4f);
    }

    @Test
    void gravityAccumulatesAcrossFrames() {
        system.update(world, 1.0f);
        system.update(world, 1.0f);
        Velocity vel = world.get(entity, Velocity.class).orElseThrow();
        assertEquals(-WorldConstants.GRAVITY * 2, vel.y(), 1e-4f);
    }

    @Test
    void terminalVelocityIsClamped() {
        world.add(entity, new Velocity(0f, WorldConstants.TERMINAL_VELOCITY - 10f, 0f));
        system.update(world, 1.0f);
        Velocity vel = world.get(entity, Velocity.class).orElseThrow();
        assertEquals(WorldConstants.TERMINAL_VELOCITY, vel.y(), 1e-4f);
    }

    @Test
    void horizontalVelocityIsPreserved() {
        world.add(entity, new Velocity(5f, 0f, -3f));
        system.update(world, 1.0f);
        Velocity vel = world.get(entity, Velocity.class).orElseThrow();
        assertEquals(5f,  vel.x(), 1e-4f);
        assertEquals(-3f, vel.z(), 1e-4f);
    }

    @Test
    void flyingEntitySkipsGravity() {
        world.add(entity, new Flying());
        system.update(world, 1.0f);
        Velocity vel = world.get(entity, Velocity.class).orElseThrow();
        assertEquals(0f, vel.y(), 1e-5f);
    }

    @Test
    void entityWithoutGravityIsUnaffected() {
        Entity noGravEntity = world.create();
        world.add(noGravEntity, new Velocity(0f, 0f, 0f));
        system.update(world, 1.0f);
        Velocity vel = world.get(noGravEntity, Velocity.class).orElseThrow();
        assertEquals(0f, vel.y(), 1e-5f);
    }

    @Test
    void entityWithoutVelocityIsIgnored() {
        Entity noVelEntity = world.create();
        world.add(noVelEntity, new Gravity(WorldConstants.GRAVITY));
        assertDoesNotThrow(() -> system.update(world, 1.0f));
    }

    @Test
    void scaledDtGivesProportionalAcceleration() {
        system.update(world, 0.5f);
        Velocity vel = world.get(entity, Velocity.class).orElseThrow();
        assertEquals(-WorldConstants.GRAVITY * 0.5f, vel.y(), 1e-4f);
    }
}
