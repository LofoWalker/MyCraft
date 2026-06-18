package org.example.world;

import org.example.components.AiState;
import org.example.components.ColliderAABB;
import org.example.components.Gravity;
import org.example.components.Health;
import org.example.components.MobType;
import org.example.components.PathTarget;
import org.example.components.Position;
import org.example.components.Rotation;
import org.example.components.Velocity;
import org.example.ecs.Entity;
import org.example.ecs.World;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MobsTest {

    private World world;

    @BeforeEach
    void setUp() {
        world = new World();
    }

    @Test
    void spawnReturnsMobWithMobType() {
        Entity mob = Mobs.spawn(world, MobType.Kind.COW, 10f, 64f, 10f);
        MobType type = world.get(mob, MobType.class).orElseThrow();
        assertEquals(MobType.Kind.COW, type.kind());
    }

    @Test
    void spawnPlacesAtCorrectPosition() {
        Entity mob = Mobs.spawn(world, MobType.Kind.PIG, 5f, 72f, 8f);
        Position pos = world.get(mob, Position.class).orElseThrow();
        assertEquals(5f,  pos.x(), 1e-5f);
        assertEquals(72f, pos.y(), 1e-5f);
        assertEquals(8f,  pos.z(), 1e-5f);
    }

    @ParameterizedTest
    @EnumSource(MobType.Kind.class)
    void spawnAssemblesAllRequiredComponents(MobType.Kind kind) {
        Entity mob = Mobs.spawn(world, kind, 0f, 64f, 0f);
        assertTrue(world.has(mob, MobType.class),     "MobType missing");
        assertTrue(world.has(mob, Position.class),    "Position missing");
        assertTrue(world.has(mob, Velocity.class),    "Velocity missing");
        assertTrue(world.has(mob, Gravity.class),     "Gravity missing");
        assertTrue(world.has(mob, ColliderAABB.class),"ColliderAABB missing");
        assertTrue(world.has(mob, Rotation.class),    "Rotation missing");
        assertTrue(world.has(mob, Health.class),      "Health missing");
        assertTrue(world.has(mob, AiState.class),     "AiState missing");
        assertTrue(world.has(mob, PathTarget.class),  "PathTarget missing");
    }

    @ParameterizedTest
    @EnumSource(MobType.Kind.class)
    void colliderMatchesMobTypeSize(MobType.Kind kind) {
        Entity mob = Mobs.spawn(world, kind, 0f, 64f, 0f);
        ColliderAABB col = world.get(mob, ColliderAABB.class).orElseThrow();
        assertEquals(kind.width(),  col.width(),  1e-5f);
        assertEquals(kind.height(), col.height(), 1e-5f);
        assertEquals(kind.width(),  col.depth(),  1e-5f);
    }

    @ParameterizedTest
    @EnumSource(MobType.Kind.class)
    void healthMatchesMobTypeMaxHealth(MobType.Kind kind) {
        Entity mob = Mobs.spawn(world, kind, 0f, 64f, 0f);
        Health health = world.get(mob, Health.class).orElseThrow();
        assertEquals(kind.maxHealth(), health.current());
        assertEquals(kind.maxHealth(), health.max());
    }

    @Test
    void initialVelocityIsZero() {
        Entity mob = Mobs.spawn(world, MobType.Kind.ZOMBIE, 0f, 64f, 0f);
        Velocity vel = world.get(mob, Velocity.class).orElseThrow();
        assertEquals(0f, vel.x(), 1e-5f);
        assertEquals(0f, vel.y(), 1e-5f);
        assertEquals(0f, vel.z(), 1e-5f);
    }

    @Test
    void initialAiStateIsIdle() {
        Entity mob = Mobs.spawn(world, MobType.Kind.CHICKEN, 0f, 64f, 0f);
        AiState ai = world.get(mob, AiState.class).orElseThrow();
        assertEquals(AiState.Behaviour.IDLE, ai.behaviour());
    }

    @Test
    void spawnTwoMobsCreatesDistinctEntities() {
        Entity a = Mobs.spawn(world, MobType.Kind.COW,  0f, 64f, 0f);
        Entity b = Mobs.spawn(world, MobType.Kind.SHEEP, 5f, 64f, 5f);
        assertNotEquals(a, b);
        assertEquals(MobType.Kind.COW,  world.get(a, MobType.class).orElseThrow().kind());
        assertEquals(MobType.Kind.SHEEP, world.get(b, MobType.class).orElseThrow().kind());
    }
}
