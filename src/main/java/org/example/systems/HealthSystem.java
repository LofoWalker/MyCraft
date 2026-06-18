package org.example.systems;

import org.example.components.*;
import org.example.ecs.Entity;
import org.example.ecs.GameSystem;
import org.example.ecs.World;
import org.example.world.FallDamage;
import org.example.world.WorldConstants;

import java.util.HashMap;
import java.util.Map;

/**
 * Applies fall and drowning damage, runs i-frames, keeps the post-damage delay clock, and respawns the
 * player on death. Passive health regen was moved to {@link HungerSystem} (STEP-25) so it can be gated
 * on and funded by hunger; HealthSystem no longer heals. Scheduled BEFORE {@link CollisionSystem}: the
 * fall-impact speed must be
 * read from {@link Velocity} while it still carries the real downward speed, because CollisionSystem
 * zeroes Velocity.y the moment it grounds the entity. The largest observed downward speed is stashed
 * in {@link FallSpeed} so that the landing it produces (detected via {@link Grounded} the next tick)
 * can be converted to damage by the pure {@link FallDamage} math.
 *
 * <p>Lava contact damage is out of scope (no BLOCK_LAVA yet) — see the TODO in WorldConstants.
 */
public final class HealthSystem implements GameSystem {

    @Override
    public void update(World world, float dt) {
        Map<Long, VoxelChunkData> chunkMap = buildChunkMap(world);
        for (int eid : world.query(Health.class, Position.class, Velocity.class)) {
            Entity entity = new Entity(eid);
            decayImmunity(world, entity, dt);
            int damage = fallDamage(world, entity)
                       + drownDamage(world, entity, chunkMap, dt);
            applyDamage(world, entity, damage);
            advanceSinceDamage(world, entity, dt, damage > 0);
            if (isDead(world, entity)) respawn(world, entity);
        }
    }

    // --- Fall damage ---

    private static int fallDamage(World world, Entity entity) {
        float impact = recordFall(world, entity);
        if (!world.has(entity, Grounded.class) || impact <= 0f) return 0;
        clearFall(world, entity);
        return FallDamage.fromImpactSpeed(impact);
    }

    // Tracks the peak downward speed while airborne and returns the speed pending a landing. Capturing
    // here (before CollisionSystem) preserves the real impact speed; once grounded the value is held
    // until consumed by fallDamage on the landing tick.
    private static float recordFall(World world, Entity entity) {
        float stored = world.get(entity, FallSpeed.class).map(FallSpeed::speed).orElse(0f);
        if (world.has(entity, Grounded.class)) return stored;
        float falling = -world.get(entity, Velocity.class).orElseThrow().y();
        float peak = Math.max(stored, Math.max(falling, 0f));
        world.add(entity, new FallSpeed(peak));
        return peak;
    }

    private static void clearFall(World world, Entity entity) {
        world.add(entity, new FallSpeed(0f));
    }

    // --- Drowning ---

    private static int drownDamage(World world, Entity entity,
                                   Map<Long, VoxelChunkData> chunkMap, float dt) {
        boolean submerged = headUnderwater(world, entity, chunkMap);
        float air = breath(world, entity);
        if (!submerged) {
            world.add(entity, new Breath(refill(air, dt)));
            return drainDrownAccum(world, entity);
        }
        float remaining = air - dt;
        world.add(entity, new Breath(Math.max(remaining, 0f)));
        if (remaining > 0f) return 0;
        return tickDrown(world, entity, dt);
    }

    // One drown hit per elapsed interval at most; i-frames (set by applyDamage) gate it further, so a
    // single submerged stretch loses one point per DROWN_INTERVAL rather than draining every tick.
    private static int tickDrown(World world, Entity entity, float dt) {
        DamageTimers timers = timers(world, entity);
        float accum = timers.drownAccum() + dt;
        if (accum < WorldConstants.DROWN_INTERVAL) {
            world.add(entity, withDrownAccum(timers, accum));
            return 0;
        }
        world.add(entity, withDrownAccum(timers, accum - WorldConstants.DROWN_INTERVAL));
        return WorldConstants.DROWN_DAMAGE;
    }

    private static int drainDrownAccum(World world, Entity entity) {
        DamageTimers timers = timers(world, entity);
        if (timers.drownAccum() != 0f) world.add(entity, withDrownAccum(timers, 0f));
        return 0;
    }

    private static float refill(float air, float dt) {
        return Math.min(air + WorldConstants.BREATH_REFILL_RATE * dt, WorldConstants.BREATH_SECONDS);
    }

    private static boolean headUnderwater(World world, Entity entity,
                                          Map<Long, VoxelChunkData> chunkMap) {
        Position pos = world.get(entity, Position.class).orElseThrow();
        int wx = (int) Math.floor(pos.x());
        int wy = (int) Math.floor(pos.y() + WorldConstants.PLAYER_EYE_HEIGHT);
        int wz = (int) Math.floor(pos.z());
        return org.example.world.FluidLogic.isWater(blockAt(wx, wy, wz, chunkMap));
    }

    // --- Damage / immunity / post-damage clock (regen lives in HungerSystem) ---

    private static void applyDamage(World world, Entity entity, int damage) {
        if (damage <= 0 || immune(world, entity)) return;
        Health health = world.get(entity, Health.class).orElseThrow();
        world.add(entity, new Health(health.current() - damage, health.max()));
        world.add(entity, new DamageImmunity(WorldConstants.DAMAGE_IMMUNITY_SECONDS));
        DamageTimers timers = timers(world, entity);
        world.add(entity, new DamageTimers(0f, timers.drownAccum(), 0f));
    }

    private static void decayImmunity(World world, Entity entity, float dt) {
        float seconds = world.get(entity, DamageImmunity.class).map(DamageImmunity::seconds).orElse(0f);
        world.add(entity, new DamageImmunity(Math.max(seconds - dt, 0f)));
    }

    private static boolean immune(World world, Entity entity) {
        return world.get(entity, DamageImmunity.class).map(DamageImmunity::seconds).orElse(0f) > 0f;
    }

    // Advances the post-damage delay timer that HungerSystem reads to gate hunger-funded regen. Health
    // regen itself is OWNED by HungerSystem (see its class doc); HealthSystem only keeps the clock.
    private static void advanceSinceDamage(World world, Entity entity, float dt, boolean tookDamage) {
        DamageTimers timers = timers(world, entity);
        float since = tookDamage ? 0f : timers.sinceDamage() + dt;
        world.add(entity, new DamageTimers(since, timers.drownAccum(), timers.regenAccum()));
    }

    // --- Death & respawn ---

    private static boolean isDead(World world, Entity entity) {
        return world.get(entity, Health.class).orElseThrow().current() <= 0;
    }

    private static void respawn(World world, Entity entity) {
        SpawnPoint spawn = world.get(entity, SpawnPoint.class).orElseThrow();
        world.add(entity, new Position(spawn.x(), spawn.y(), spawn.z()));
        world.add(entity, new Velocity(0f, 0f, 0f));
        world.add(entity, new Health(WorldConstants.MAX_HEALTH, WorldConstants.MAX_HEALTH));
        world.add(entity, new Breath(WorldConstants.BREATH_SECONDS));
        world.add(entity, new FallSpeed(0f));
        world.add(entity, new DamageTimers(0f, 0f, 0f));
        // Restore hunger on respawn only when the entity actually tracks it, so non-player entities
        // with Health (if any) are unaffected and HungerSystem keeps sole ownership of the bar.
        if (world.has(entity, Hunger.class)) {
            world.add(entity, new Hunger(WorldConstants.MAX_FOOD, WorldConstants.MAX_FOOD));
            world.add(entity, new HungerTimers(0f, 0f, 0f));
        }
    }

    // --- Component accessors with sane defaults ---

    private static float breath(World world, Entity entity) {
        return world.get(entity, Breath.class).map(Breath::air).orElse(WorldConstants.BREATH_SECONDS);
    }

    private static DamageTimers timers(World world, Entity entity) {
        return world.get(entity, DamageTimers.class).orElse(new DamageTimers(0f, 0f, 0f));
    }

    private static DamageTimers withDrownAccum(DamageTimers timers, float drownAccum) {
        return new DamageTimers(timers.sinceDamage(), drownAccum, timers.regenAccum());
    }

    // --- Block query (mirrors CollisionSystem's chunk-map lookup, but returns the raw block id) ---

    private static byte blockAt(int wx, int wy, int wz, Map<Long, VoxelChunkData> chunkMap) {
        if (wy < 0 || wy >= WorldConstants.WORLD_HEIGHT) return WorldConstants.BLOCK_AIR;
        int cx = Math.floorDiv(wx, WorldConstants.CHUNK_SIZE_XZ);
        int cz = Math.floorDiv(wz, WorldConstants.CHUNK_SIZE_XZ);
        VoxelChunkData data = chunkMap.get(CollisionSystem.chunkKey(cx, cz));
        if (data == null) return WorldConstants.BLOCK_AIR;
        int lx = wx - cx * WorldConstants.CHUNK_SIZE_XZ;
        int lz = wz - cz * WorldConstants.CHUNK_SIZE_XZ;
        return data.get(lx, wy, lz);
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
