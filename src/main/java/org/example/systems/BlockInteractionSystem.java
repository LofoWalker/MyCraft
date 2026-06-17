package org.example.systems;

import org.example.components.BlockBreakProgress;
import org.example.components.CameraComponent;
import org.example.components.ChunkComponent;
import org.example.components.ChunkDirty;
import org.example.components.PlayerInput;
import org.example.components.Position;
import org.example.components.Rotation;
import org.example.components.TargetedBlock;
import org.example.components.VoxelChunkData;
import org.example.ecs.Entity;
import org.example.ecs.GameSystem;
import org.example.ecs.World;
import org.example.world.BlockType;
import org.example.world.ChunkView;
import org.example.world.VoxelRaycast;
import org.example.world.WorldConstants;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

// Drives block targeting and breaking: every tick it raycasts from the eye along the view direction
// (via the shared VoxelRaycast) and records the looked-at voxel as TargetedBlock for the renderer.
// On a fresh break press it accumulates damage on that voxel; a block breaks only once the damage
// reaches its hardness, so harder blocks need more hits and switching target resets progress.
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
        Position    pos    = world.get(player, Position.class).orElseThrow();
        Rotation    rot    = world.get(player, Rotation.class).orElseThrow();

        Map<Long, Integer>        chunkEntities = new HashMap<>();
        Map<Long, VoxelChunkData> chunkData     = buildChunkMaps(world, chunkEntities);
        Optional<VoxelRaycast.RaycastHit> hit   = castFromEye(pos, rot, chunkData);

        updateTarget(world, player, hit);
        applyBreak(world, player, input, hit, chunkEntities, chunkData);
    }

    private void updateTarget(World world, Entity player, Optional<VoxelRaycast.RaycastHit> hit) {
        if (hit.isPresent()) {
            VoxelRaycast.RaycastHit h = hit.get();
            world.add(player, new TargetedBlock(h.x(), h.y(), h.z(), h.faceX(), h.faceY(), h.faceZ()));
        } else {
            world.remove(player, TargetedBlock.class);
        }
    }

    private void applyBreak(World world, Entity player, PlayerInput input,
                            Optional<VoxelRaycast.RaycastHit> hit,
                            Map<Long, Integer> chunkEntities, Map<Long, VoxelChunkData> chunkData) {
        boolean breakNow    = input.breakBlock();
        boolean justPressed = breakNow && !breakHeldPreviously;
        breakHeldPreviously = breakNow;
        if (!justPressed || hit.isEmpty()) return;

        VoxelRaycast.RaycastHit h = hit.get();
        damageBlock(world, player, h.x(), h.y(), h.z(), chunkEntities, chunkData);
    }

    private static Optional<VoxelRaycast.RaycastHit> castFromEye(Position pos, Rotation rot,
                                                                 Map<Long, VoxelChunkData> chunkData) {
        float yawRad   = (float) Math.toRadians(rot.yaw());
        float pitchRad = (float) Math.toRadians(rot.pitch());
        float cosPitch = (float) Math.cos(pitchRad);
        float dirX = (float)  (Math.sin(yawRad) * cosPitch);
        float dirY = (float)   Math.sin(pitchRad);
        float dirZ = (float) (-Math.cos(yawRad) * cosPitch);

        float eyeX = pos.x();
        float eyeY = pos.y() + WorldConstants.PLAYER_EYE_HEIGHT;
        float eyeZ = pos.z();

        ChunkView view = (wx, wy, wz) -> CollisionSystem.isSolid(wx, wy, wz, chunkData);
        return VoxelRaycast.cast(eyeX, eyeY, eyeZ, dirX, dirY, dirZ, WorldConstants.PLAYER_REACH, view);
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
