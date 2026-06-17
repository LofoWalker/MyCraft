package org.example.systems;

import org.example.components.*;
import org.example.ecs.Entity;
import org.example.ecs.World;
import org.example.world.WorldConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HealthSystemTest {

    private static final float DT = 1f / 60f;
    private static final float SPAWN_X = 8f;
    private static final float SPAWN_Y = 70f;
    private static final float SPAWN_Z = 8f;

    private World        world;
    private Entity       player;
    private HealthSystem system;

    @BeforeEach
    void setUp() {
        world  = new World();
        system = new HealthSystem();
        player = world.create();
        world.add(player, new Health(WorldConstants.MAX_HEALTH, WorldConstants.MAX_HEALTH));
        world.add(player, new Position(SPAWN_X, SPAWN_Y, SPAWN_Z));
        world.add(player, new Velocity(0f, 0f, 0f));
        world.add(player, new Breath(WorldConstants.BREATH_SECONDS));
        world.add(player, new DamageImmunity(0f));
        world.add(player, new DamageTimers(0f, 0f, 0f));
        world.add(player, new SpawnPoint(SPAWN_X, SPAWN_Y, SPAWN_Z));
    }

    // --- helpers ---

    private void fillWaterAroundEye() {
        VoxelChunkData data = VoxelChunkData.empty();
        for (int x = 0; x < WorldConstants.CHUNK_SIZE_XZ; x++)
            for (int z = 0; z < WorldConstants.CHUNK_SIZE_XZ; z++)
                for (int y = 0; y < WorldConstants.WORLD_HEIGHT; y++)
                    data.set(x, y, z, WorldConstants.BLOCK_WATER);
        Entity chunk = world.create();
        world.add(chunk, new ChunkComponent(0, 0));
        world.add(chunk, data);
    }

    private int health() {
        return world.get(player, Health.class).orElseThrow().current();
    }

    private void advance(float seconds) {
        int steps = Math.round(seconds / DT);
        for (int i = 0; i < steps; i++) system.update(world, DT);
    }

    // --- drowning ---

    @Test
    void breathDrainsUnderwater() {
        fillWaterAroundEye();
        system.update(world, DT);
        assertTrue(world.get(player, Breath.class).orElseThrow().air() < WorldConstants.BREATH_SECONDS);
    }

    @Test
    void noDrownDamageBeforeBreathRunsOut() {
        fillWaterAroundEye();
        advance(WorldConstants.BREATH_SECONDS - 1f);
        assertEquals(WorldConstants.MAX_HEALTH, health());
    }

    @Test
    void drowningRemovesHealthAfterBreathRunsOut() {
        fillWaterAroundEye();
        advance(WorldConstants.BREATH_SECONDS + WorldConstants.DROWN_INTERVAL + 0.1f);
        assertTrue(health() < WorldConstants.MAX_HEALTH);
    }

    @Test
    void breathRefillsInAir() {
        world.add(player, new Breath(2f));
        system.update(world, DT);
        assertTrue(world.get(player, Breath.class).orElseThrow().air() > 2f);
    }

    // --- i-frames ---

    @Test
    void immunityPreventsBackToBackDrownTicks() {
        fillWaterAroundEye();
        // Out of breath, with a drown interval about to elapse.
        world.add(player, new Breath(0f));
        world.add(player, new DamageTimers(0f, WorldConstants.DROWN_INTERVAL, 0f));
        int before = health();

        // First tick: interval elapses → one drown hit lands and grants i-frames.
        system.update(world, DT);
        assertEquals(before - WorldConstants.DROWN_DAMAGE, health());

        // Force another interval to elapse immediately; still inside the i-frame window → no second hit.
        world.add(player, new DamageTimers(0f, WorldConstants.DROWN_INTERVAL, 0f));
        system.update(world, DT);
        assertEquals(before - WorldConstants.DROWN_DAMAGE, health());
    }

    @Test
    void immunityDecaysOverTime() {
        world.add(player, new DamageImmunity(WorldConstants.DAMAGE_IMMUNITY_SECONDS));
        system.update(world, DT);
        float remaining = world.get(player, DamageImmunity.class).orElseThrow().seconds();
        assertTrue(remaining < WorldConstants.DAMAGE_IMMUNITY_SECONDS);
    }

    // --- fall damage ---

    @Test
    void hardLandingHurts() {
        // Tick 1: airborne, falling fast → impact speed captured.
        world.add(player, new Velocity(0f, -(WorldConstants.SAFE_FALL_SPEED + 20f), 0f));
        system.update(world, DT);
        // Tick 2: grounded (collision would have set this) with zeroed vy → damage applied.
        world.add(player, new Grounded());
        world.add(player, new Velocity(0f, 0f, 0f));
        system.update(world, DT);
        assertTrue(health() < WorldConstants.MAX_HEALTH);
    }

    @Test
    void gentleLandingDoesNotHurt() {
        world.add(player, new Velocity(0f, -(WorldConstants.SAFE_FALL_SPEED - 2f), 0f));
        system.update(world, DT);
        world.add(player, new Grounded());
        world.add(player, new Velocity(0f, 0f, 0f));
        system.update(world, DT);
        assertEquals(WorldConstants.MAX_HEALTH, health());
    }

    // --- death & respawn ---

    @Test
    void deathRespawnsAtSpawnWithFullHealth() {
        // Move the player off-spawn and bring them near death, then land at a lethal speed. A single
        // fall hit exceeds the remaining health, so death (and respawn) is deterministic and does not
        // depend on regen/drown timing.
        world.add(player, new Health(2, WorldConstants.MAX_HEALTH));
        world.add(player, new Position(10f, 70f, 10f));
        world.add(player, new Velocity(0f, -(WorldConstants.SAFE_FALL_SPEED + 100f), 0f));
        system.update(world, DT);                       // airborne: capture impact speed
        world.add(player, new Grounded());              // collision grounds the player on landing
        world.add(player, new Velocity(0f, 0f, 0f));
        system.update(world, DT);                       // landing: lethal fall damage -> respawn

        assertEquals(WorldConstants.MAX_HEALTH, health());
        Position pos = world.get(player, Position.class).orElseThrow();
        assertEquals(SPAWN_X, pos.x(), 1e-4f);
        assertEquals(SPAWN_Y, pos.y(), 1e-4f);
        assertEquals(SPAWN_Z, pos.z(), 1e-4f);
        Velocity vel = world.get(player, Velocity.class).orElseThrow();
        assertEquals(0f, vel.y(), 1e-5f);
        assertEquals(WorldConstants.BREATH_SECONDS,
                world.get(player, Breath.class).orElseThrow().air(), 1e-4f);
    }

    // --- regen ownership (STEP-25) ---

    // Health regen moved to HungerSystem (so it can be gated on / funded by hunger). HealthSystem no
    // longer heals on its own; it only keeps the post-damage delay clock that HungerSystem reads.
    @Test
    void healthSystemDoesNotRegenerateOnItsOwn() {
        world.add(player, new Health(WorldConstants.MAX_HEALTH - 4, WorldConstants.MAX_HEALTH));
        advance(WorldConstants.REGEN_DELAY_SECONDS + WorldConstants.REGEN_INTERVAL + 0.1f);
        assertEquals(WorldConstants.MAX_HEALTH - 4, health());
    }

    @Test
    void sinceDamageTimerAdvancesWhenUnharmed() {
        advance(1f);
        assertTrue(world.get(player, DamageTimers.class).orElseThrow().sinceDamage() > 0f);
    }
}
