package org.example.systems;

import org.example.components.AiState;
import org.example.components.AiState.Behaviour;
import org.example.components.ChunkComponent;
import org.example.components.ColliderAABB;
import org.example.components.DamageImmunity;
import org.example.components.FleeSource;
import org.example.components.Grounded;
import org.example.components.Health;
import org.example.components.MobType;
import org.example.components.PathTarget;
import org.example.components.PlayerInput;
import org.example.components.Position;
import org.example.components.Velocity;
import org.example.components.VoxelChunkData;
import org.example.ecs.Entity;
import org.example.ecs.GameSystem;
import org.example.ecs.World;
import org.example.world.MobDrops;
import org.example.world.WorldConstants;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * State-machine AI for passive mobs: IDLE → WANDER → IDLE, with FLEE triggered by damage.
 * Death (Health <= 0) spawns drops and destroys the entity.
 *
 * <p>Steering is pure: given a mob's position and target, it computes a horizontal velocity
 * vector and writes it back via a new Velocity. The vertical component (gravity/jumping) is
 * preserved from the previous tick so PhysicsSystem and CollisionSystem keep owning it.
 */
public final class MobAiSystem implements GameSystem {

    @Override
    public void update(World world, float dt) {
        Map<Long, VoxelChunkData> chunkMap = buildChunkMap(world);
        Position playerPos = findPlayerPosition(world);

        for (int eid : world.query(MobType.class, AiState.class, Position.class, Velocity.class)) {
            Entity mob = new Entity(eid);
            if (handleDeath(world, mob)) continue;
            tickMob(world, mob, dt, chunkMap, playerPos);
        }
    }

    // Returns true when the mob died and was destroyed (caller must skip further updates).
    private static boolean handleDeath(World world, Entity mob) {
        Health health = world.get(mob, Health.class).orElse(null);
        if (health == null || health.current() > 0) return false;

        MobType type = world.get(mob, MobType.class).orElseThrow();
        Position pos  = world.get(mob, Position.class).orElseThrow();
        for (int dropId : MobDrops.dropIds(type.kind())) {
            ItemDrops.spawnAt(world, pos.x(), pos.y(), pos.z(), dropId);
        }
        world.destroy(mob);
        return true;
    }

    private static void tickMob(World world, Entity mob, float dt,
                                 Map<Long, VoxelChunkData> chunkMap, Position playerPos) {
        AiState  ai  = world.get(mob, AiState.class).orElseThrow();
        Position pos = world.get(mob, Position.class).orElseThrow();
        Velocity vel = world.get(mob, Velocity.class).orElseThrow();

        boolean justHit = isJustHit(world, mob);
        AiState nextAi  = transitionState(world, mob, ai, pos, dt, justHit, playerPos);
        Velocity nextVel = steer(world, mob, nextAi, pos, vel, chunkMap);

        world.add(mob, nextAi);
        world.add(mob, nextVel);
    }

    // --- State transitions ---

    static AiState transitionState(World world, Entity mob, AiState ai, Position pos,
                                    float dt, boolean justHit, Position playerPos) {
        if (justHit) {
            recordFleeSource(world, mob, playerPos);
            return new AiState(Behaviour.FLEE, WorldConstants.MOB_FLEE_SECONDS);
        }
        return switch (ai.behaviour()) {
            case IDLE   -> tickIdle(world, mob, ai, pos, dt);
            case WANDER -> tickWander(ai, dt);
            case FLEE   -> tickFlee(world, mob, ai, dt);
            case CHASE  -> ai; // CHASE reserved for hostile mobs (STEP-31)
        };
    }

    private static AiState tickIdle(World world, Entity mob, AiState ai, Position pos, float dt) {
        float remaining = ai.timer() - dt;
        if (remaining > 0f) return new AiState(Behaviour.IDLE, remaining);
        // Timer expired: pick a wander target and start wandering.
        chooseWanderTarget(world, mob, pos);
        float wanderDuration = WorldConstants.MOB_WANDER_MAX_SECONDS;
        return new AiState(Behaviour.WANDER, wanderDuration);
    }

    private static AiState tickWander(AiState ai, float dt) {
        float remaining = ai.timer() - dt;
        if (remaining <= 0f) {
            float idleDuration = randomIdleDuration();
            return new AiState(Behaviour.IDLE, idleDuration);
        }
        return new AiState(Behaviour.WANDER, remaining);
    }

    private static AiState tickFlee(World world, Entity mob, AiState ai, float dt) {
        float remaining = ai.timer() - dt;
        if (remaining <= 0f) {
            world.remove(mob, FleeSource.class);
            float idleDuration = randomIdleDuration();
            return new AiState(Behaviour.IDLE, idleDuration);
        }
        return new AiState(Behaviour.FLEE, remaining);
    }

    // --- Steering (pure: input pos/target → output velocity) ---

    static Velocity steer(World world, Entity mob, AiState ai, Position pos,
                           Velocity currentVel, Map<Long, VoxelChunkData> chunkMap) {
        return switch (ai.behaviour()) {
            case WANDER -> steerToward(world, mob, pos, currentVel, chunkMap,
                                       WorldConstants.MOB_WANDER_SPEED);
            case FLEE   -> steerFlee(world, mob, pos, currentVel, WorldConstants.MOB_FLEE_SPEED);
            default     -> coast(currentVel); // IDLE and CHASE: no horizontal input
        };
    }

    /**
     * Returns a velocity pointing from pos toward the PathTarget at the given speed.
     * If the mob is blocked (wall), grants a 1-block jump to climb over it.
     * Pure math — testable without OpenGL.
     */
    static Velocity steerToward(World world, Entity mob, Position pos, Velocity currentVel,
                                 Map<Long, VoxelChunkData> chunkMap, float speed) {
        PathTarget target = world.get(mob, PathTarget.class).orElse(null);
        if (target == null) return coast(currentVel);

        float dx = target.x() - pos.x();
        float dz = target.z() - pos.z();
        float dist = (float) Math.sqrt(dx * dx + dz * dz);
        if (dist < 0.5f) return coast(currentVel); // close enough, drift to stop

        float vx = (dx / dist) * speed;
        float vz = (dz / dist) * speed;
        float vy = maybeJump(world, mob, pos, currentVel, chunkMap, vx, vz);
        return new Velocity(vx, vy, vz);
    }

    /**
     * Returns a velocity pointing directly away from the stored FleeSource.
     * Pure math — testable without OpenGL.
     */
    static Velocity steerFlee(World world, Entity mob, Position pos,
                               Velocity currentVel, float speed) {
        FleeSource source = world.get(mob, FleeSource.class).orElse(null);
        if (source == null) return coast(currentVel);

        float dx = pos.x() - source.x();
        float dz = pos.z() - source.z();
        float dist = (float) Math.sqrt(dx * dx + dz * dz);
        if (dist < 0.01f) {
            // Exactly on top of source: flee in a random direction.
            float angle = ThreadLocalRandom.current().nextFloat() * 2f * (float) Math.PI;
            return new Velocity((float) Math.cos(angle) * speed, currentVel.y(),
                                (float) Math.sin(angle) * speed);
        }
        return new Velocity((dx / dist) * speed, currentVel.y(), (dz / dist) * speed);
    }

    // When a mob is grounded and its intended horizontal move is blocked, give it a small jump.
    private static float maybeJump(World world, Entity mob, Position pos, Velocity currentVel,
                                    Map<Long, VoxelChunkData> chunkMap, float vx, float vz) {
        if (!world.has(mob, Grounded.class)) return currentVel.y();
        ColliderAABB box = world.get(mob, ColliderAABB.class).orElse(null);
        if (box == null) return currentVel.y();

        float checkX = pos.x() + vx * WorldConstants.MOB_OBSTACLE_CHECK_DIST;
        float checkZ = pos.z() + vz * WorldConstants.MOB_OBSTACLE_CHECK_DIST;
        int bx = (int) Math.floor(checkX);
        int by = (int) Math.floor(pos.y());
        int bz = (int) Math.floor(checkZ);
        if (CollisionSystem.isSolid(bx, by + 1, bz, chunkMap)) {
            return WorldConstants.MOB_JUMP_IMPULSE;
        }
        return currentVel.y();
    }

    // Horizontal components decay naturally; vertical kept so physics owns gravity.
    private static Velocity coast(Velocity vel) {
        return new Velocity(0f, vel.y(), 0f);
    }

    // --- Helpers ---

    private static boolean isJustHit(World world, Entity mob) {
        return world.get(mob, DamageImmunity.class)
                    .map(d -> d.seconds() > 0f)
                    .orElse(false);
    }

    private static void recordFleeSource(World world, Entity mob, Position playerPos) {
        if (playerPos != null) {
            world.add(mob, new FleeSource(playerPos.x(), playerPos.z()));
        }
    }

    private static void chooseWanderTarget(World world, Entity mob, Position pos) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        float angle = rng.nextFloat() * 2f * (float) Math.PI;
        float radius = rng.nextFloat() * WorldConstants.MOB_WANDER_RADIUS;
        float tx = pos.x() + (float) Math.cos(angle) * radius;
        float tz = pos.z() + (float) Math.sin(angle) * radius;
        world.add(mob, new PathTarget(tx, pos.y(), tz));
    }

    private static float randomIdleDuration() {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        float range = WorldConstants.MOB_IDLE_MAX_SECONDS - WorldConstants.MOB_IDLE_MIN_SECONDS;
        return WorldConstants.MOB_IDLE_MIN_SECONDS + rng.nextFloat() * range;
    }

    private static Position findPlayerPosition(World world) {
        int[] players = world.query(PlayerInput.class, Position.class);
        if (players.length == 0) return null;
        return world.get(new Entity(players[0]), Position.class).orElse(null);
    }

    private static Map<Long, VoxelChunkData> buildChunkMap(World world) {
        Map<Long, VoxelChunkData> map = new HashMap<>();
        for (int eid : world.query(ChunkComponent.class, VoxelChunkData.class)) {
            Entity         e     = new Entity(eid);
            ChunkComponent chunk = world.get(e, ChunkComponent.class).orElseThrow();
            VoxelChunkData data  = world.get(e, VoxelChunkData.class).orElseThrow();
            map.put(CollisionSystem.chunkKey(chunk.x(), chunk.z()), data);
        }
        return map;
    }
}
