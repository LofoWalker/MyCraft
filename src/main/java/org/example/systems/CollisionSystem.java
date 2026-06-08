package org.example.systems;

import org.example.components.*;
import org.example.ecs.Entity;
import org.example.ecs.GameSystem;
import org.example.ecs.World;
import org.example.world.WorldConstants;

import java.util.HashMap;
import java.util.Map;

public final class CollisionSystem implements GameSystem {

    private static final float EPS = 1e-4f;

    @Override
    public void update(World world, float dt) {
        Map<Long, VoxelChunkData> chunkMap = buildChunkMap(world);

        for (int eid : world.query(Position.class, Velocity.class, ColliderAABB.class)) {
            Entity      entity = new Entity(eid);
            Position    pos    = world.get(entity, Position.class).orElseThrow();
            Velocity    vel    = world.get(entity, Velocity.class).orElseThrow();
            ColliderAABB box   = world.get(entity, ColliderAABB.class).orElseThrow();

            float[] resolved = resolveAxes(pos, vel, box, chunkMap, dt);
            float rx = resolved[0], ry = resolved[1], rz = resolved[2];
            boolean groundedNow = resolved[3] != 0f;
            boolean blockedY    = resolved[6] != 0f;

            world.add(entity, new Position(rx, ry, rz));

            float newVx = (resolved[4] != 0f) ? 0f : vel.x();
            float newVy = (groundedNow || blockedY) ? 0f : vel.y();
            float newVz = (resolved[5] != 0f) ? 0f : vel.z();
            world.add(entity, new Velocity(newVx, newVy, newVz));

            if (groundedNow) world.add(entity, new Grounded());
            else             world.remove(entity, Grounded.class);
        }
    }

    // Returns [x, y, z, groundedFlag, blockedX, blockedZ, blockedY]
    private static float[] resolveAxes(Position pos, Velocity vel,
                                        ColliderAABB box,
                                         Map<Long, VoxelChunkData> chunkMap,
                                         float dt) {
        float hw = box.width()  / 2f;
        float hd = box.depth()  / 2f;
        float h  = box.height();

        float x = pos.x(), y = pos.y(), z = pos.z();
        float prevX = x - vel.x() * dt;
        float prevY = y - vel.y() * dt;
        float prevZ = z - vel.z() * dt;
        boolean groundedNow = false;
        boolean blockedY    = false;
        boolean blockedX    = false;
        boolean blockedZ    = false;

        // --- Y axis ---
        int minBX = (int) Math.floor(x - hw);
        int maxBX = (int) Math.floor(x + hw - EPS);
        int minBZ = (int) Math.floor(z - hd);
        int maxBZ = (int) Math.floor(z + hd - EPS);

        if (vel.y() <= 0f) {
            int startFootY = (int) Math.floor(prevY - EPS);
            int endFootY   = (int) Math.floor(y - EPS);
            for (int footY = startFootY; footY >= endFootY && !groundedNow; footY--) {
                for (int bx = minBX; bx <= maxBX && !groundedNow; bx++) {
                    for (int bz = minBZ; bz <= maxBZ && !groundedNow; bz++) {
                        if (isSolid(bx, footY, bz, chunkMap)) {
                            y = prevY;
                            groundedNow = true;
                            blockedY = true;
                        }
                    }
                }
            }
        } else {
            int startHeadY = (int) Math.floor(prevY + h - EPS);
            int endHeadY   = (int) Math.floor(y + h - EPS);
            boolean hitCeiling = false;
            int firstHeadY = startHeadY;
            for (int headY = firstHeadY; headY <= endHeadY && !hitCeiling; headY++) {
                for (int bx = minBX; bx <= maxBX && !hitCeiling; bx++) {
                    for (int bz = minBZ; bz <= maxBZ && !hitCeiling; bz++) {
                        if (isSolid(bx, headY, bz, chunkMap)) {
                            y = prevY;
                            hitCeiling = true;
                            blockedY = true;
                        }
                    }
                }
            }
        }

        // --- X axis ---
        int minBY = (int) Math.floor(y + EPS);
        int maxBY = (int) Math.floor(y + h - EPS);
        minBZ     = (int) Math.floor(z - hd);
        maxBZ     = (int) Math.floor(z + hd - EPS);

        if (vel.x() > 0f) {
            int startFrontX = (int) Math.floor(prevX + hw - EPS);
            int endFrontX   = (int) Math.floor(x + hw - EPS);
            int firstFrontX = startFrontX;
            for (int frontX = firstFrontX; frontX <= endFrontX && !blockedX; frontX++) {
                for (int by = minBY; by <= maxBY && !blockedX; by++) {
                    for (int bz = minBZ; bz <= maxBZ && !blockedX; bz++) {
                        if (isSolid(frontX, by, bz, chunkMap)) {
                            x = prevX;
                            blockedX = true;
                        }
                    }
                }
            }
        } else if (vel.x() < 0f) {
            int startBackX = (int) Math.floor(prevX - hw);
            int endBackX   = (int) Math.floor(x - hw);
            int firstBackX = startBackX;
            for (int backX = firstBackX; backX >= endBackX && !blockedX; backX--) {
                for (int by = minBY; by <= maxBY && !blockedX; by++) {
                    for (int bz = minBZ; bz <= maxBZ && !blockedX; bz++) {
                        if (isSolid(backX, by, bz, chunkMap)) {
                            x = prevX;
                            blockedX = true;
                        }
                    }
                }
            }
        }

        // --- Z axis ---
        minBX = (int) Math.floor(x - hw);
        maxBX = (int) Math.floor(x + hw - EPS);

        if (vel.z() > 0f) {
            int startFrontZ = (int) Math.floor(prevZ + hd - EPS);
            int endFrontZ   = (int) Math.floor(z + hd - EPS);
            int firstFrontZ = startFrontZ;
            for (int frontZ = firstFrontZ; frontZ <= endFrontZ && !blockedZ; frontZ++) {
                for (int by = minBY; by <= maxBY && !blockedZ; by++) {
                    for (int bx = minBX; bx <= maxBX && !blockedZ; bx++) {
                        if (isSolid(bx, by, frontZ, chunkMap)) {
                            z = prevZ;
                            blockedZ = true;
                        }
                    }
                }
            }
        } else if (vel.z() < 0f) {
            int startBackZ = (int) Math.floor(prevZ - hd);
            int endBackZ   = (int) Math.floor(z - hd);
            int firstBackZ = startBackZ;
            for (int backZ = firstBackZ; backZ >= endBackZ && !blockedZ; backZ--) {
                for (int by = minBY; by <= maxBY && !blockedZ; by++) {
                    for (int bx = minBX; bx <= maxBX && !blockedZ; bx++) {
                        if (isSolid(bx, by, backZ, chunkMap)) {
                            z = prevZ;
                            blockedZ = true;
                        }
                    }
                }
            }
        }

        return new float[]{x, y, z,
                groundedNow ? 1f : 0f,
                blockedX    ? 1f : 0f,
                blockedZ    ? 1f : 0f,
                blockedY    ? 1f : 0f};
    }

    static boolean isSolid(int wx, int wy, int wz, Map<Long, VoxelChunkData> chunkMap) {
        if (wy < 0 || wy >= WorldConstants.WORLD_HEIGHT) return false;
        int cx = Math.floorDiv(wx, WorldConstants.CHUNK_SIZE_XZ);
        int cz = Math.floorDiv(wz, WorldConstants.CHUNK_SIZE_XZ);
        VoxelChunkData data = chunkMap.get(chunkKey(cx, cz));
        if (data == null) return false;
        int lx = wx - cx * WorldConstants.CHUNK_SIZE_XZ;
        int lz = wz - cz * WorldConstants.CHUNK_SIZE_XZ;
        byte block = data.get(lx, wy, lz);
        return block != WorldConstants.BLOCK_AIR && block != WorldConstants.BLOCK_WATER;
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
