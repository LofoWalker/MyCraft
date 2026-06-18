package org.example.systems;

import org.example.components.ChunkComponent;
import org.example.components.MobType;
import org.example.components.PlayerInput;
import org.example.components.Position;
import org.example.components.TimeOfDay;
import org.example.components.VoxelChunkData;
import org.example.ecs.Entity;
import org.example.ecs.World;
import org.example.world.Mobs;
import org.example.world.WorldConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

class PassiveSpawnSystemTest {

    private World              world;
    private PassiveSpawnSystem system;

    @BeforeEach
    void setUp() {
        world  = new World();
        system = new PassiveSpawnSystem();
    }

    // --- isDaytime ---

    @Test
    void dayFractionZeroIsDaytime() {
        addClock(0f);
        assertTrue(PassiveSpawnSystem.isDaytime(world));
    }

    @Test
    void dayFractionNoonIsDaytime() {
        addClock(0.25f);
        assertTrue(PassiveSpawnSystem.isDaytime(world));
    }

    @Test
    void dayFractionJustBeforeDuskIsDaytime() {
        addClock(WorldConstants.MOB_SPAWN_DAY_FRACTION_MAX - 0.001f);
        assertTrue(PassiveSpawnSystem.isDaytime(world));
    }

    @Test
    void dayFractionAtDuskIsNight() {
        addClock(WorldConstants.MOB_SPAWN_DAY_FRACTION_MAX);
        assertFalse(PassiveSpawnSystem.isDaytime(world));
    }

    @Test
    void dayFractionMidnightIsNight() {
        addClock(0.75f);
        assertFalse(PassiveSpawnSystem.isDaytime(world));
    }

    @Test
    void noClock_assumesDaytime() {
        // No TimeOfDay entity: isDaytime defaults to true.
        assertTrue(PassiveSpawnSystem.isDaytime(world));
    }

    // --- findGrassSurface ---

    @Test
    void grassSurfaceFoundInColumnWithGrassBlock() {
        Map<Long, VoxelChunkData> chunkMap = singleChunkWithGrassAt(8, 64, 8);
        int y = PassiveSpawnSystem.findGrassSurface(8, 8, chunkMap);
        assertEquals(64, y);
    }

    @Test
    void noGrassReturnsMinus1() {
        Map<Long, VoxelChunkData> chunkMap = emptyChunk();
        int y = PassiveSpawnSystem.findGrassSurface(8, 8, chunkMap);
        assertEquals(-1, y);
    }

    @Test
    void missingChunkReturnsMinus1() {
        int y = PassiveSpawnSystem.findGrassSurface(0, 0, new HashMap<>());
        assertEquals(-1, y);
    }

    @Test
    void grassCoveredByBlockReturnsMinus1() {
        Map<Long, VoxelChunkData> chunkMap = singleChunkWithGrassAt(8, 64, 8);
        // Put a stone block on top of the grass.
        VoxelChunkData data = chunkMap.values().iterator().next();
        data.set(8, 65, 8, WorldConstants.BLOCK_STONE);
        int y = PassiveSpawnSystem.findGrassSurface(8, 8, chunkMap);
        assertEquals(-1, y);
    }

    // --- Population cap ---

    @Test
    void spawnIsRefusedWhenAtCap() {
        // Saturate the population cap.
        for (int i = 0; i < WorldConstants.MAX_PASSIVE_PER_AREA; i++) {
            Mobs.spawn(world, MobType.Kind.COW, i * 2f, 64f, 0f);
        }
        addClock(0f); // daytime
        addPlayer(0f, 64f, 0f);
        addGrassChunk(0, 0);

        int before = world.query(MobType.class).length;
        // Manually tick past the spawn interval threshold (system is throttled so force time).
        tickPastSpawnInterval();

        int after = world.query(MobType.class).length;
        assertEquals(before, after, "No new mobs should spawn when at cap");
    }

    // --- Night spawn refusal ---

    @Test
    void spawnIsRefusedAtNight() {
        addClock(0.75f); // midnight
        addPlayer(0f, 64f, 0f);
        addGrassChunk(0, 0);

        int before = world.query(MobType.class).length;
        tickPastSpawnInterval();
        int after = world.query(MobType.class).length;

        assertEquals(before, after, "No mobs should spawn at night");
    }

    // --- Accepted conditions ---

    @Test
    void spawnSucceedsWhenConditionsValid() {
        addClock(0f); // daytime
        addPlayer(0f, 64f, 0f);
        // Cover a 5x5 area of chunks centred on the player so the random spawn
        // radius (12-48 blocks) reliably finds at least one valid grass column
        // in 8 attempts regardless of the random angle chosen.
        for (int cx = -2; cx <= 2; cx++) {
            for (int cz = -2; cz <= 2; cz++) {
                addGrassChunk(cx, cz);
            }
        }

        int before = world.query(MobType.class).length;
        tickPastSpawnInterval();
        int after = world.query(MobType.class).length;

        assertTrue(after > before,
                   "At least one mob should have spawned under valid conditions");
    }

    // --- Despawn ---

    @Test
    void distantMobIsDespawned() {
        addClock(0f);
        addPlayer(0f, 64f, 0f);

        // Spawn a mob far beyond the despawn radius.
        float farDist = WorldConstants.MOB_DESPAWN_RADIUS + 10f;
        Mobs.spawn(world, MobType.Kind.PIG, farDist, 64f, 0f);

        // Tick past the despawn interval.
        tickPastDespawnInterval();

        assertEquals(0, world.query(MobType.class).length,
                     "Distant mob should have been despawned");
    }

    @Test
    void nearbyMobIsNotDespawned() {
        addClock(0f);
        addPlayer(0f, 64f, 0f);

        Mobs.spawn(world, MobType.Kind.PIG, 5f, 64f, 5f);

        tickPastDespawnInterval();

        assertEquals(1, world.query(MobType.class).length,
                     "Nearby mob should not be despawned");
    }

    // --- helpers ---

    private void addClock(float fraction) {
        Entity clock = world.create();
        world.add(clock, new TimeOfDay(fraction));
    }

    private void addPlayer(float x, float y, float z) {
        Entity player = world.create();
        world.add(player, new Position(x, y, z));
        // Minimal PlayerInput: all false, no mouse delta, no hotbar select.
        world.add(player, new PlayerInput(false, false, false, false, false, false,
                                          0f, 0f, false, false, false, 0,
                                          WorldConstants.NO_HOTBAR_SELECT, false));
    }

    private void addGrassChunk(int cx, int cz) {
        VoxelChunkData data = VoxelChunkData.empty();
        // Lay a grass surface at y=64 across the whole chunk.
        for (int lx = 0; lx < WorldConstants.CHUNK_SIZE_XZ; lx++) {
            for (int lz = 0; lz < WorldConstants.CHUNK_SIZE_XZ; lz++) {
                data.set(lx, 64, lz, WorldConstants.BLOCK_GRASS);
            }
        }
        Entity chunk = world.create();
        world.add(chunk, new ChunkComponent(cx, cz));
        world.add(chunk, data);
    }

    private Map<Long, VoxelChunkData> singleChunkWithGrassAt(int lx, int y, int lz) {
        VoxelChunkData data = VoxelChunkData.empty();
        data.set(lx, y, lz, WorldConstants.BLOCK_GRASS);
        Map<Long, VoxelChunkData> map = new HashMap<>();
        map.put(CollisionSystem.chunkKey(0, 0), data);
        return map;
    }

    private Map<Long, VoxelChunkData> emptyChunk() {
        Map<Long, VoxelChunkData> map = new HashMap<>();
        map.put(CollisionSystem.chunkKey(0, 0), VoxelChunkData.empty());
        return map;
    }

    private void tickPastSpawnInterval() {
        // Tick slightly past the spawn interval so the accumulator fires.
        float target = WorldConstants.MOB_SPAWN_INTERVAL + 0.1f;
        float step   = WorldConstants.MOB_SPAWN_INTERVAL + 0.1f;
        system.update(world, step);
    }

    private void tickPastDespawnInterval() {
        float step = WorldConstants.MOB_DESPAWN_INTERVAL + 0.1f;
        system.update(world, step);
    }
}
