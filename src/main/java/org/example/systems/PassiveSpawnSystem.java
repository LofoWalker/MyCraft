package org.example.systems;

import org.example.components.ChunkComponent;
import org.example.components.MobType;
import org.example.components.PlayerInput;
import org.example.components.Position;
import org.example.components.TimeOfDay;
import org.example.components.VoxelChunkData;
import org.example.ecs.Entity;
import org.example.ecs.GameSystem;
import org.example.ecs.World;
import org.example.world.Mobs;
import org.example.world.WorldConstants;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Throttled system that periodically spawns small packs of passive mobs on grass blocks
 * exposed to the sky, during daytime only, within loaded chunks around the player.
 * Also despawns mobs that have wandered beyond the load radius.
 *
 * <p>All heavy work (surface scan, population count) is done at most once per
 * MOB_SPAWN_INTERVAL seconds; no HashMap or per-frame world sweep.
 */
public final class PassiveSpawnSystem implements GameSystem {

    private float spawnAccum  = 0f;
    private float despawnAccum = 0f;

    @Override
    public void update(World world, float dt) {
        spawnAccum  += dt;
        despawnAccum += dt;

        if (despawnAccum >= WorldConstants.MOB_DESPAWN_INTERVAL) {
            despawnAccum = 0f;
            despawnDistant(world);
        }

        if (spawnAccum >= WorldConstants.MOB_SPAWN_INTERVAL) {
            spawnAccum = 0f;
            trySpawnPass(world);
        }
    }

    // --- Spawn pass ---

    private static void trySpawnPass(World world) {
        if (!isDaytime(world)) return;

        Position playerPos = findPlayerPosition(world);
        if (playerPos == null) return;

        Map<Long, VoxelChunkData> chunkMap = buildChunkMap(world);
        if (chunkMap.isEmpty()) return;

        if (passiveMobCount(world) >= WorldConstants.MAX_PASSIVE_PER_AREA) return;

        spawnPack(world, playerPos, chunkMap);
    }

    private static void spawnPack(World world, Position playerPos,
                                   Map<Long, VoxelChunkData> chunkMap) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        int count = WorldConstants.MOB_PACK_MIN
                + rng.nextInt(WorldConstants.MOB_PACK_MAX - WorldConstants.MOB_PACK_MIN + 1);

        MobType.Kind kind = pickPassiveKind(rng);

        for (int i = 0; i < count; i++) {
            float[] spawn = findSpawnSurface(rng, playerPos, chunkMap);
            if (spawn == null) return;
            if (passiveMobCount(world) >= WorldConstants.MAX_PASSIVE_PER_AREA) return;
            Mobs.spawn(world, kind, spawn[0], spawn[1], spawn[2]);
        }
    }

    /**
     * Tries a limited number of random XZ candidates around the player and returns
     * the first valid grass surface, or null if none found.
     * Package-private so PassiveSpawnSystemTest can drive it directly.
     */
    static float[] findSpawnSurface(ThreadLocalRandom rng, Position playerPos,
                                     Map<Long, VoxelChunkData> chunkMap) {
        for (int attempt = 0; attempt < 8; attempt++) {
            float angle  = rng.nextFloat() * 2f * (float) Math.PI;
            float radius = WorldConstants.MOB_SPAWN_MIN_RADIUS
                    + rng.nextFloat() * (WorldConstants.MOB_SPAWN_MAX_RADIUS
                                         - WorldConstants.MOB_SPAWN_MIN_RADIUS);
            float cx = playerPos.x() + (float) Math.cos(angle) * radius;
            float cz = playerPos.z() + (float) Math.sin(angle) * radius;

            int surfaceY = findGrassSurface((int) Math.floor(cx), (int) Math.floor(cz), chunkMap);
            if (surfaceY < 0) continue;
            return new float[]{ cx, surfaceY + 1f, cz };
        }
        return null;
    }

    /**
     * Scans downward from the sky for the topmost grass block exposed to the sky.
     * Returns the grass block Y, or -1 if no grass found in this column.
     * Package-private for testability.
     */
    static int findGrassSurface(int wx, int wz, Map<Long, VoxelChunkData> chunkMap) {
        int cx = Math.floorDiv(wx, WorldConstants.CHUNK_SIZE_XZ);
        int cz = Math.floorDiv(wz, WorldConstants.CHUNK_SIZE_XZ);
        VoxelChunkData data = chunkMap.get(CollisionSystem.chunkKey(cx, cz));
        if (data == null) return -1;

        int lx = wx - cx * WorldConstants.CHUNK_SIZE_XZ;
        int lz = wz - cz * WorldConstants.CHUNK_SIZE_XZ;

        // Scan from the top down; stop at y=1 so there is always a y+1 to check.
        for (int y = WorldConstants.WORLD_HEIGHT - 2; y >= 1; y--) {
            byte block     = data.get(lx, y,     lz);
            byte blockAbove = data.get(lx, y + 1, lz);
            if (block == WorldConstants.BLOCK_GRASS && blockAbove == WorldConstants.BLOCK_AIR) {
                return y;
            }
        }
        return -1;
    }

    // --- Despawn pass ---

    private static void despawnDistant(World world) {
        Position playerPos = findPlayerPosition(world);
        if (playerPos == null) return;

        float radiusSq = WorldConstants.MOB_DESPAWN_RADIUS * WorldConstants.MOB_DESPAWN_RADIUS;

        // Collect ids first to avoid mutating the query result during iteration.
        int[] mobs = world.query(MobType.class, Position.class);
        for (int eid : mobs) {
            Entity mob = new Entity(eid);
            Position pos = world.get(mob, Position.class).orElse(null);
            if (pos == null) continue;
            float dx = pos.x() - playerPos.x();
            float dz = pos.z() - playerPos.z();
            if (dx * dx + dz * dz > radiusSq) {
                world.destroy(mob);
            }
        }
    }

    // --- Helpers ---

    /**
     * Daytime is defined as dayFraction in [0, MOB_SPAWN_DAY_FRACTION_MAX).
     * Package-private for direct testing.
     */
    static boolean isDaytime(World world) {
        int[] clocks = world.query(TimeOfDay.class);
        if (clocks.length == 0) return true; // no clock entity → assume day
        float frac = world.get(new Entity(clocks[0]), TimeOfDay.class)
                          .map(TimeOfDay::dayFraction)
                          .orElse(0f);
        return frac < WorldConstants.MOB_SPAWN_DAY_FRACTION_MAX;
    }

    private static int passiveMobCount(World world) {
        return world.query(MobType.class).length;
    }

    private static Position findPlayerPosition(World world) {
        int[] players = world.query(PlayerInput.class, Position.class);
        if (players.length == 0) return null;
        return world.get(new Entity(players[0]), Position.class).orElse(null);
    }

    private static MobType.Kind pickPassiveKind(ThreadLocalRandom rng) {
        MobType.Kind[] passiveKinds = {
            MobType.Kind.COW, MobType.Kind.PIG,
            MobType.Kind.SHEEP, MobType.Kind.CHICKEN
        };
        return passiveKinds[rng.nextInt(passiveKinds.length)];
    }

    private static Map<Long, VoxelChunkData> buildChunkMap(World world) {
        Map<Long, VoxelChunkData> map = new HashMap<>();
        for (int eid : world.query(ChunkComponent.class, VoxelChunkData.class)) {
            Entity e = new Entity(eid);
            ChunkComponent chunk = world.get(e, ChunkComponent.class).orElseThrow();
            VoxelChunkData data  = world.get(e, VoxelChunkData.class).orElseThrow();
            map.put(CollisionSystem.chunkKey(chunk.x(), chunk.z()), data);
        }
        return map;
    }
}
