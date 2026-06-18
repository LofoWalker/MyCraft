package org.example.systems;

import org.example.components.AiState;
import org.example.components.AiState.Behaviour;
import org.example.components.ColliderAABB;
import org.example.components.DamageImmunity;
import org.example.components.FleeSource;
import org.example.components.Gravity;
import org.example.components.Grounded;
import org.example.components.Health;
import org.example.components.MobType;
import org.example.components.PathTarget;
import org.example.components.Position;
import org.example.components.Rotation;
import org.example.components.Velocity;
import org.example.ecs.Entity;
import org.example.ecs.World;
import org.example.world.Mobs;
import org.example.world.WorldConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MobAiSystemTest {

    private static final float DT = 1f / 20f;

    private World        world;
    private MobAiSystem  system;

    @BeforeEach
    void setUp() {
        world  = new World();
        system = new MobAiSystem();
    }

    // --- WANDER: velocity points toward PathTarget ---

    @Test
    void wanderVelocityPointsTowardTarget() {
        Entity mob = spawnMob(0f, 64f, 0f);
        world.add(mob, new AiState(Behaviour.WANDER, 5f));
        world.add(mob, new PathTarget(10f, 64f, 0f)); // target is directly east (+X)

        system.update(world, DT);

        Velocity vel = world.get(mob, Velocity.class).orElseThrow();
        assertTrue(vel.x() > 0f, "vx should be positive toward east target");
        assertEquals(0f, vel.z(), 1e-3f, "vz should be ~0 (target is on +X axis)");
    }

    @Test
    void wanderSpeedMatchesWanderConstant() {
        Entity mob = spawnMob(0f, 64f, 0f);
        world.add(mob, new AiState(Behaviour.WANDER, 5f));
        world.add(mob, new PathTarget(10f, 64f, 10f));

        system.update(world, DT);

        Velocity vel = world.get(mob, Velocity.class).orElseThrow();
        float speed = (float) Math.sqrt(vel.x() * vel.x() + vel.z() * vel.z());
        assertEquals(WorldConstants.MOB_WANDER_SPEED, speed, 1e-4f);
    }

    @Test
    void wanderCoastsToStopWhenCloseEnoughToTarget() {
        Entity mob = spawnMob(0f, 64f, 0f);
        world.add(mob, new AiState(Behaviour.WANDER, 5f));
        world.add(mob, new PathTarget(0.3f, 64f, 0.3f)); // within 0.5 block threshold

        system.update(world, DT);

        Velocity vel = world.get(mob, Velocity.class).orElseThrow();
        assertEquals(0f, vel.x(), 1e-4f);
        assertEquals(0f, vel.z(), 1e-4f);
    }

    // --- FLEE: velocity points away from FleeSource ---

    @Test
    void fleeVelocityPointsAwayFromSource() {
        Entity mob = spawnMob(0f, 64f, 0f);
        world.add(mob, new AiState(Behaviour.FLEE, WorldConstants.MOB_FLEE_SECONDS));
        world.add(mob, new FleeSource(-10f, 0f)); // source is west; mob should flee east

        system.update(world, DT);

        Velocity vel = world.get(mob, Velocity.class).orElseThrow();
        assertTrue(vel.x() > 0f, "Should flee east (away from western source)");
    }

    @Test
    void fleeSpeedMatchesFleeConstant() {
        Entity mob = spawnMob(0f, 64f, 0f);
        world.add(mob, new AiState(Behaviour.FLEE, WorldConstants.MOB_FLEE_SECONDS));
        world.add(mob, new FleeSource(-5f, 0f));

        system.update(world, DT);

        Velocity vel = world.get(mob, Velocity.class).orElseThrow();
        float speed = (float) Math.sqrt(vel.x() * vel.x() + vel.z() * vel.z());
        assertEquals(WorldConstants.MOB_FLEE_SPEED, speed, 1e-4f);
    }

    // --- State transitions: IDLE ↔ WANDER ---

    @Test
    void idleTransitionsToWanderWhenTimerExpires() {
        Entity mob = spawnMob(0f, 64f, 0f);
        world.add(mob, new AiState(Behaviour.IDLE, DT * 0.5f)); // timer < DT → will expire

        system.update(world, DT);

        AiState ai = world.get(mob, AiState.class).orElseThrow();
        assertEquals(Behaviour.WANDER, ai.behaviour());
    }

    @Test
    void idleDoesNotTransitionBeforeTimerExpires() {
        Entity mob = spawnMob(0f, 64f, 0f);
        world.add(mob, new AiState(Behaviour.IDLE, 10f)); // long timer

        system.update(world, DT);

        AiState ai = world.get(mob, AiState.class).orElseThrow();
        assertEquals(Behaviour.IDLE, ai.behaviour());
    }

    @Test
    void wanderTransitionsToIdleWhenTimerExpires() {
        Entity mob = spawnMob(0f, 64f, 0f);
        world.add(mob, new AiState(Behaviour.WANDER, DT * 0.5f));
        world.add(mob, new PathTarget(5f, 64f, 5f));

        system.update(world, DT);

        AiState ai = world.get(mob, AiState.class).orElseThrow();
        assertEquals(Behaviour.IDLE, ai.behaviour());
    }

    // --- Flee triggered by damage immunity ---

    @Test
    void damageImmunityTriggersFlee() {
        Entity mob = spawnMob(0f, 64f, 0f);
        world.add(mob, new AiState(Behaviour.IDLE, 5f));
        world.add(mob, new DamageImmunity(WorldConstants.DAMAGE_IMMUNITY_SECONDS));
        // No player entity → flee source will be null; state still transitions to FLEE.
        system.update(world, DT);

        AiState ai = world.get(mob, AiState.class).orElseThrow();
        assertEquals(Behaviour.FLEE, ai.behaviour());
    }

    @Test
    void noDamageImmunityDoesNotForceFlee() {
        Entity mob = spawnMob(0f, 64f, 0f);
        world.add(mob, new AiState(Behaviour.IDLE, 5f));
        world.add(mob, new DamageImmunity(0f)); // not immune

        system.update(world, DT);

        AiState ai = world.get(mob, AiState.class).orElseThrow();
        assertEquals(Behaviour.IDLE, ai.behaviour());
    }

    // --- Pure steering helpers (unit-testable without a full update) ---

    @Test
    void steerTowardPureUnit_velocityHasCorrectDirection() {
        World w = new World();
        Entity mob = w.create();
        w.add(mob, new PathTarget(3f, 64f, 4f)); // dx=3, dz=4 → dist=5, normalised=(0.6,0.8)
        w.add(mob, new Position(0f, 64f, 0f));
        w.add(mob, new ColliderAABB(0.9f, 1.4f, 0.9f));
        Velocity current = new Velocity(0f, -1f, 0f);

        Velocity result = MobAiSystem.steerToward(w, mob, new Position(0f, 64f, 0f),
                                                   current, Map.of(),
                                                   WorldConstants.MOB_WANDER_SPEED);

        float expectedVx = (3f / 5f) * WorldConstants.MOB_WANDER_SPEED;
        float expectedVz = (4f / 5f) * WorldConstants.MOB_WANDER_SPEED;
        assertEquals(expectedVx, result.x(), 1e-4f);
        assertEquals(expectedVz, result.z(), 1e-4f);
    }

    @Test
    void steerFleePureUnit_velocityPointsAway() {
        World w = new World();
        Entity mob = w.create();
        w.add(mob, new FleeSource(0f, 0f)); // source at origin
        Velocity current = new Velocity(0f, 0f, 0f);
        Position pos = new Position(3f, 64f, 4f); // mob is 5 blocks away

        Velocity result = MobAiSystem.steerFlee(w, mob, pos, current,
                                                 WorldConstants.MOB_FLEE_SPEED);

        float expectedVx = (3f / 5f) * WorldConstants.MOB_FLEE_SPEED;
        float expectedVz = (4f / 5f) * WorldConstants.MOB_FLEE_SPEED;
        assertEquals(expectedVx, result.x(), 1e-4f);
        assertEquals(expectedVz, result.z(), 1e-4f);
    }

    // --- Death drops and destroy ---

    @Test
    void deadMobIsDestroyedAndDropsItems() {
        Entity mob = spawnMob(0f, 64f, 0f);
        world.add(mob, new Health(0, 10)); // dead

        system.update(world, DT);

        // Entity should no longer be present in any query.
        assertFalse(world.has(mob, MobType.class), "Dead mob should be destroyed");
        // At least one drop item should exist in the world.
        assertTrue(world.query(org.example.components.ItemEntity.class).length >= 1,
                   "Death should spawn drop items");
    }

    @Test
    void aliveMobIsNotDestroyed() {
        Entity mob = spawnMob(0f, 64f, 0f);
        world.add(mob, new Health(5, 10));
        world.add(mob, new AiState(Behaviour.IDLE, 1f));

        system.update(world, DT);

        assertTrue(world.has(mob, MobType.class), "Living mob should not be destroyed");
    }

    // --- helper ---

    private Entity spawnMob(float x, float y, float z) {
        return Mobs.spawn(world, MobType.Kind.COW, x, y, z);
    }
}
