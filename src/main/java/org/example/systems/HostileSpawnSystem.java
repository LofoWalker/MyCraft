package org.example.systems;

import org.example.components.ChunkComponent;
import org.example.components.HostileTag;
import org.example.components.MobType;
import org.example.components.PlayerInput;
import org.example.components.Position;
import org.example.components.TimeOfDay;
import org.example.components.VoxelChunkData;
import org.example.ecs.Entity;
import org.example.ecs.GameSystem;
import org.example.ecs.World;
import org.example.world.HostileSpawnRules;
import org.example.world.Mobs;
import org.example.world.WorldConstants;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Periodically attempts to spawn a hostile mob in a dark spot around the player. Throttled to one
 * attempt per {@link WorldConstants#HOSTILE_SPAWN_INTERVAL}s; eligibility is decided by the pure
 * {@link HostileSpawnRules}. Light is approximated by the day/night cycle (surface darkness is
 * global), so hostiles appear at night and stay capped per area.
 */
public final class HostileSpawnSystem implements GameSystem {

    private static final MobType.Kind[] HOSTILE_KINDS = {
            MobType.Kind.ZOMBIE, MobType.Kind.SKELETON, MobType.Kind.SPIDER, MobType.Kind.CREEPER
    };
    // Daylight reads as bright; night reads as dark enough for hostiles.
    private static final int DAY_LIGHT  = WorldConstants.MAX_LIGHT_LEVEL;
    private static final int NIGHT_LIGHT = 0;

    private float timer;

    @Override
    public void update(World world, float dt) {
        timer += dt;
        if (timer < WorldConstants.HOSTILE_SPAWN_INTERVAL) return;
        timer = 0f;

        Entity player = findPlayer(world);
        if (player == null) return;
        Position ppos = world.get(player, Position.class).orElseThrow();

        int light = ambientLight(findDayFraction(world));
        int population = world.query(HostileTag.class).length;
        Map<Long, VoxelChunkData> chunkMap = buildChunkMap(world);

        ThreadLocalRandom rng = ThreadLocalRandom.current();
        float angle  = rng.nextFloat() * 2f * (float) Math.PI;
        float radius = WorldConstants.MOB_SPAWN_MIN_RADIUS
                + rng.nextFloat() * (WorldConstants.MOB_SPAWN_MAX_RADIUS - WorldConstants.MOB_SPAWN_MIN_RADIUS);
        int cx = floor(ppos.x() + (float) Math.cos(angle) * radius);
        int cz = floor(ppos.z() + (float) Math.sin(angle) * radius);

        int standY = findStandingY(cx, cz, floor(ppos.y()), chunkMap);
        if (standY < 0) return;

        double dist = distance(ppos, cx + 0.5, standY, cz + 0.5);
        if (!HostileSpawnRules.canSpawn(light, dist, population)) return;

        MobType.Kind kind = HOSTILE_KINDS[rng.nextInt(HOSTILE_KINDS.length)];
        Mobs.spawnHostile(world, kind, cx + 0.5f, standY, cz + 0.5f);
    }

    private static int ambientLight(float dayFraction) {
        // dayFraction in [0, 0.5) is daytime (bright); the night half is dark.
        return dayFraction < 0.5f ? DAY_LIGHT : NIGHT_LIGHT;
    }

    // Scans a band around the player's height for a solid block with two air cells above it.
    // Returns the standing Y (feet), or -1 if no suitable surface is found in the band.
    private static int findStandingY(int cx, int cz, int aroundY, Map<Long, VoxelChunkData> chunkMap) {
        int top = Math.min(aroundY + 16, WorldConstants.WORLD_HEIGHT - 3);
        int bottom = Math.max(aroundY - 16, 1);
        for (int y = top; y >= bottom; y--) {
            boolean floorSolid = CollisionSystem.isSolid(cx, y, cz, chunkMap);
            boolean feetAir    = !CollisionSystem.isSolid(cx, y + 1, cz, chunkMap);
            boolean headAir    = !CollisionSystem.isSolid(cx, y + 2, cz, chunkMap);
            if (floorSolid && feetAir && headAir) return y + 1;
        }
        return -1;
    }

    private static double distance(Position a, double bx, double by, double bz) {
        double dx = a.x() - bx, dy = a.y() - by, dz = a.z() - bz;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static Entity findPlayer(World world) {
        int[] players = world.query(PlayerInput.class, Position.class);
        return players.length == 0 ? null : new Entity(players[0]);
    }

    private static float findDayFraction(World world) {
        int[] clocks = world.query(TimeOfDay.class);
        if (clocks.length == 0) return 0f;
        return world.get(new Entity(clocks[0]), TimeOfDay.class).map(TimeOfDay::dayFraction).orElse(0f);
    }

    private static Map<Long, VoxelChunkData> buildChunkMap(World world) {
        Map<Long, VoxelChunkData> map = new HashMap<>();
        for (int eid : world.query(ChunkComponent.class, VoxelChunkData.class)) {
            Entity e = new Entity(eid);
            ChunkComponent chunk = world.get(e, ChunkComponent.class).orElseThrow();
            map.put(CollisionSystem.chunkKey(chunk.x(), chunk.z()), world.get(e, VoxelChunkData.class).orElseThrow());
        }
        return map;
    }

    private static int floor(float v) {
        return (int) Math.floor(v);
    }
}
