package org.example.systems;

import org.example.components.*;
import org.example.ecs.Entity;
import org.example.ecs.GameSystem;
import org.example.ecs.World;
import org.example.world.WorldConstants;

import java.util.HashMap;
import java.util.Map;

public final class CollisionSystem implements GameSystem {

    @Override
    public void update(World world, float dt) {
        Map<Long, VoxelChunkData> chunkMap = buildChunkMap(world);

        for (int eid : world.query(Position.class, Velocity.class, ColliderAABB.class)) {
            Entity      entity = new Entity(eid);
            Position    pos    = world.get(entity, Position.class).orElseThrow();
            Velocity    vel    = world.get(entity, Velocity.class).orElseThrow();
            ColliderAABB box   = world.get(entity, ColliderAABB.class).orElseThrow();

            float[] resolved = resolveAxes(pos, vel, box, chunkMap);
            float rx = resolved[0], ry = resolved[1], rz = resolved[2];
            boolean groundedNow = resolved[3] != 0f;

            world.add(entity, new Position(rx, ry, rz));

            float newVx = (resolved[4] != 0f) ? 0f : vel.x();
            float newVy = groundedNow          ? 0f : vel.y();
            float newVz = (resolved[5] != 0f) ? 0f : vel.z();
            world.add(entity, new Velocity(newVx, newVy, newVz));

            if (groundedNow) world.add(entity, new Grounded());
            else             world.remove(entity, Grounded.class);
        }
    }

    // Returns [x, y, z, groundedFlag, blockedX, blockedZ]
    private static float[] resolveAxes(Position pos, Velocity vel,
                                        ColliderAABB box,
                                        Map<Long, VoxelChunkData> chunkMap) {
        float hw = box.width()  / 2f;
        float hd = box.depth()  / 2f;
        float h  = box.height();

        float x = pos.x(), y = pos.y(), z = pos.z();
        boolean groundedNow = false;
        boolean blockedX    = false;
        boolean blockedZ    = false;

        // --- Y axis ---
        int minBX = (int) Math.floor(x - hw);
        int maxBX = (int) Math.floor(x + hw - 1e-4f);
        int minBZ = (int) Math.floor(z - hd);
        int maxBZ = (int) Math.floor(z + hd - 1e-4f);

        if (vel.y() <= 0f) {
            // Falling: check foot level
            int footY = (int) Math.floor(y - 1e-4f);
            for (int bx = minBX; bx <= maxBX && !groundedNow; bx++) {
                for (int bz = minBZ; bz <= maxBZ && !groundedNow; bz++) {
                    if (isSolid(bx, footY, bz, chunkMap)) {
                        float pushTop = footY + 1f;
                        if (y < pushTop) y = pushTop;
                        groundedNow = true;
                    }
                }
            }
        } else {
            // Rising: check head level
            int headY = (int) Math.floor(y + h - 1e-4f);
            boolean hitCeiling = false;
            for (int bx = minBX; bx <= maxBX && !hitCeiling; bx++) {
                for (int bz = minBZ; bz <= maxBZ && !hitCeiling; bz++) {
                    if (isSolid(bx, headY, bz, chunkMap)) {
                        y = headY - h;
                        hitCeiling = true;
                    }
                }
            }
        }

        // --- X axis ---
        int minBY = (int) Math.floor(y + 1e-4f);
        int maxBY = (int) Math.floor(y + h - 1e-4f);
        minBZ     = (int) Math.floor(z - hd);
        maxBZ     = (int) Math.floor(z + hd - 1e-4f);

        if (vel.x() > 0f) {
            int frontX = (int) Math.floor(x + hw - 1e-4f);
            for (int by = minBY; by <= maxBY && !blockedX; by++) {
                for (int bz = minBZ; bz <= maxBZ && !blockedX; bz++) {
                    if (isSolid(frontX, by, bz, chunkMap)) {
                        x = frontX - hw;
                        blockedX = true;
                    }
                }
            }
        } else if (vel.x() < 0f) {
            int backX = (int) Math.floor(x - hw);
            for (int by = minBY; by <= maxBY && !blockedX; by++) {
                for (int bz = minBZ; bz <= maxBZ && !blockedX; bz++) {
                    if (isSolid(backX, by, bz, chunkMap)) {
                        x = backX + 1f + hw;
                        blockedX = true;
                    }
                }
            }
        }

        // --- Z axis ---
        minBX = (int) Math.floor(x - hw);
        maxBX = (int) Math.floor(x + hw - 1e-4f);

        if (vel.z() > 0f) {
            int frontZ = (int) Math.floor(z + hd - 1e-4f);
            for (int by = minBY; by <= maxBY && !blockedZ; by++) {
                for (int bx = minBX; bx <= maxBX && !blockedZ; bx++) {
                    if (isSolid(bx, by, frontZ, chunkMap)) {
                        z = frontZ - hd;
                        blockedZ = true;
                    }
                }
            }
        } else if (vel.z() < 0f) {
            int backZ = (int) Math.floor(z - hd);
            for (int by = minBY; by <= maxBY && !blockedZ; by++) {
                for (int bx = minBX; bx <= maxBX && !blockedZ; bx++) {
                    if (isSolid(bx, by, backZ, chunkMap)) {
                        z = backZ + 1f + hd;
                        blockedZ = true;
                    }
                }
            }
        }

        return new float[]{x, y, z,
                groundedNow ? 1f : 0f,
                blockedX    ? 1f : 0f,
                blockedZ    ? 1f : 0f};
    }

    static boolean isSolid(int wx, int wy, int wz, Map<Long, VoxelChunkData> chunkMap) {
        if (wy < 0 || wy >= WorldConstants.CHUNK_SIZE) return false;
        int cx = Math.floorDiv(wx, WorldConstants.CHUNK_SIZE);
        int cz = Math.floorDiv(wz, WorldConstants.CHUNK_SIZE);
        VoxelChunkData data = chunkMap.get(chunkKey(cx, cz));
        if (data == null) return false;
        int lx = wx - cx * WorldConstants.CHUNK_SIZE;
        int lz = wz - cz * WorldConstants.CHUNK_SIZE;
        return data.get(lx, wy, lz) != WorldConstants.BLOCK_AIR;
    }

    static long chunkKey(int cx, int cz) {
        return ((long) cx << 32) | (cz & 0xFFFFFFFFL);
    }

    private static Map<Long, VoxelChunkData> buildChunkMap(World world) {
        Map<Long, VoxelChunkData> map = new HashMap<>();
        for (int eid : world.query(ChunkComponent.class, VoxelChunkData.class)) {
            Entity         entity = new Entity(eid);
            ChunkComponent chunk  = world.get(entity, ChunkComponent.class).orElseThrow();
            VoxelChunkData data   = world.get(entity, VoxelChunkData.class).orElseThrow();
            map.put(chunkKey(chunk.x(), chunk.z()), data);
        }
        return map;
    }
}
