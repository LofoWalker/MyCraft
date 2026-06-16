package org.example.systems;

import org.example.components.BlockBreakProgress;
import org.example.components.CameraComponent;
import org.example.components.ChunkComponent;
import org.example.components.ChunkDirty;
import org.example.components.PlayerInput;
import org.example.components.Position;
import org.example.components.Rotation;
import org.example.components.VoxelChunkData;
import org.example.ecs.Entity;
import org.example.ecs.GameSystem;
import org.example.ecs.World;
import org.example.world.BlockType;
import org.example.world.WorldConstants;

import java.util.HashMap;
import java.util.Map;

// Lets the player break the block they look at: casts a ray from the eye along the view direction
// and accumulates damage on the first solid voxel within reach. A block breaks only once the damage
// dealt reaches its hardness, so harder blocks need more hits; switching target resets progress.
// Edge-triggered (one hit per press). Mesh rebuilding is owned by ChunkStreamingSystem.
public final class BlockInteractionSystem implements GameSystem {

    // Edge detection: a held mouse button must not break a block every frame.
    private boolean breakHeldPreviously;

    @Override
    public void update(World world, float dt) {
        int[] players = world.query(PlayerInput.class, Position.class, Rotation.class, CameraComponent.class);
        if (players.length == 0) return;

        Entity      player = new Entity(players[0]);
        PlayerInput input  = world.get(player, PlayerInput.class).orElseThrow();
        boolean breakNow   = input.breakBlock();
        boolean justPressed = breakNow && !breakHeldPreviously;
        breakHeldPreviously = breakNow;
        if (!justPressed) return;

        Position pos = world.get(player, Position.class).orElseThrow();
        Rotation rot = world.get(player, Rotation.class).orElseThrow();
        hitTargetedBlock(world, player, pos, rot);
    }

    private void hitTargetedBlock(World world, Entity player, Position pos, Rotation rot) {
        Map<Long, Integer>        chunkEntities = new HashMap<>();
        Map<Long, VoxelChunkData> chunkData     = buildChunkMaps(world, chunkEntities);

        float yawRad   = (float) Math.toRadians(rot.yaw());
        float pitchRad = (float) Math.toRadians(rot.pitch());
        float cosPitch = (float) Math.cos(pitchRad);
        float dirX = (float)  (Math.sin(yawRad) * cosPitch);
        float dirY = (float)   Math.sin(pitchRad);
        float dirZ = (float) (-Math.cos(yawRad) * cosPitch);

        float eyeX = pos.x();
        float eyeY = pos.y() + WorldConstants.PLAYER_EYE_HEIGHT;
        float eyeZ = pos.z();

        int[] hit = raycastSolid(eyeX, eyeY, eyeZ, dirX, dirY, dirZ, WorldConstants.PLAYER_REACH, chunkData);
        if (hit == null) return;
        damageBlock(world, player, hit[0], hit[1], hit[2], chunkEntities, chunkData);
    }

    private void damageBlock(World world, Entity player, int wx, int wy, int wz,
                             Map<Long, Integer> chunkEntities, Map<Long, VoxelChunkData> chunkData) {
        int damage = accumulatedDamage(world, player, wx, wy, wz);
        int hardness = BlockType.byId(blockAt(wx, wy, wz, chunkData)).hardness();
        if (damage >= hardness) {
            removeBlock(world, wx, wy, wz, chunkEntities, chunkData);
            world.remove(player, BlockBreakProgress.class);
        } else {
            world.add(player, new BlockBreakProgress(wx, wy, wz, damage));
        }
    }

    // Continues the in-progress break if the same voxel is still targeted, otherwise starts fresh.
    private static int accumulatedDamage(World world, Entity player, int wx, int wy, int wz) {
        int previous = world.get(player, BlockBreakProgress.class)
                .filter(p -> p.targets(wx, wy, wz))
                .map(BlockBreakProgress::damage)
                .orElse(0);
        return previous + WorldConstants.BARE_HAND_DAMAGE;
    }

    private static byte blockAt(int wx, int wy, int wz, Map<Long, VoxelChunkData> chunkData) {
        int s  = WorldConstants.CHUNK_SIZE_XZ;
        int cx = Math.floorDiv(wx, s);
        int cz = Math.floorDiv(wz, s);
        return chunkData.get(CollisionSystem.chunkKey(cx, cz)).get(wx - cx * s, wy, wz - cz * s);
    }

    private static void removeBlock(World world, int wx, int wy, int wz,
                                    Map<Long, Integer> chunkEntities, Map<Long, VoxelChunkData> chunkData) {
        int s   = WorldConstants.CHUNK_SIZE_XZ;
        int cx  = Math.floorDiv(wx, s);
        int cz  = Math.floorDiv(wz, s);
        long key = CollisionSystem.chunkKey(cx, cz);
        Integer entityId = chunkEntities.get(key);
        VoxelChunkData data = chunkData.get(key);
        if (entityId == null || data == null) return;

        data.set(wx - cx * s, wy, wz - cz * s, WorldConstants.BLOCK_AIR);
        world.add(new Entity(entityId), new ChunkDirty());
    }

    // Amanatides & Woo voxel traversal: walks integer cells along the ray, returning the first
    // solid voxel within reach, or null if the ray reaches nothing.
    static int[] raycastSolid(float ox, float oy, float oz, float dx, float dy, float dz,
                              float reach, Map<Long, VoxelChunkData> chunkData) {
        int ix = (int) Math.floor(ox);
        int iy = (int) Math.floor(oy);
        int iz = (int) Math.floor(oz);
        int stepX = signum(dx), stepY = signum(dy), stepZ = signum(dz);
        float tDeltaX = dx != 0 ? Math.abs(1f / dx) : Float.POSITIVE_INFINITY;
        float tDeltaY = dy != 0 ? Math.abs(1f / dy) : Float.POSITIVE_INFINITY;
        float tDeltaZ = dz != 0 ? Math.abs(1f / dz) : Float.POSITIVE_INFINITY;
        float tMaxX = boundaryDistance(ox, dx, stepX);
        float tMaxY = boundaryDistance(oy, dy, stepY);
        float tMaxZ = boundaryDistance(oz, dz, stepZ);

        float traveled = 0f;
        while (traveled <= reach) {
            if (CollisionSystem.isSolid(ix, iy, iz, chunkData)) return new int[]{ix, iy, iz};
            if (tMaxX < tMaxY && tMaxX < tMaxZ) { ix += stepX; traveled = tMaxX; tMaxX += tDeltaX; }
            else if (tMaxY < tMaxZ)             { iy += stepY; traveled = tMaxY; tMaxY += tDeltaY; }
            else                                { iz += stepZ; traveled = tMaxZ; tMaxZ += tDeltaZ; }
        }
        return null;
    }

    private static int signum(float v) {
        return v > 0 ? 1 : (v < 0 ? -1 : 0);
    }

    // Distance along the ray from the origin to the first voxel boundary it crosses on this axis.
    private static float boundaryDistance(float origin, float dir, int step) {
        if (step == 0) return Float.POSITIVE_INFINITY;
        float cell = (float) Math.floor(origin);
        float nextBoundary = step > 0 ? cell + 1f : cell;
        return (nextBoundary - origin) / dir;
    }

    private static Map<Long, VoxelChunkData> buildChunkMaps(World world, Map<Long, Integer> entitiesOut) {
        Map<Long, VoxelChunkData> data = new HashMap<>();
        for (int eid : world.query(ChunkComponent.class, VoxelChunkData.class)) {
            Entity         entity = new Entity(eid);
            ChunkComponent chunk  = world.get(entity, ChunkComponent.class).orElseThrow();
            long key = CollisionSystem.chunkKey(chunk.x(), chunk.z());
            data.put(key, world.get(entity, VoxelChunkData.class).orElseThrow());
            entitiesOut.put(key, eid);
        }
        return data;
    }
}
