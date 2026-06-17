package org.example.systems;

import org.example.components.BlockBreakProgress;
import org.example.components.CameraComponent;
import org.example.components.ColliderAABB;
import org.example.components.PlayerInput;
import org.example.components.Position;
import org.example.components.Rotation;
import org.example.components.TargetedBlock;
import org.example.components.VoxelChunkData;
import org.example.ecs.Entity;
import org.example.ecs.GameSystem;
import org.example.ecs.World;
import org.example.world.AABBCell;
import org.example.world.BlockType;
import org.example.world.ChunkView;
import org.example.world.VoxelRaycast;
import org.example.world.WorldConstants;

import java.util.Map;
import java.util.Optional;

// Drives block targeting, breaking and placing: every tick it raycasts from the eye along the view
// direction (via the shared VoxelRaycast) and records the looked-at voxel as TargetedBlock for the
// renderer. A fresh left-click accumulates damage on that voxel (harder blocks need more hits); a
// fresh right-click places the selected block against the targeted face, unless the cell is occupied
// or would overlap the player. Both edits go through the shared ChunkVoxelWriter, which marks the
// owning chunk dirty (current or neighbour). Edge-triggered (one action per press); mesh rebuilding
// is owned by ChunkStreamingSystem.
public final class BlockInteractionSystem implements GameSystem {

    // Edge detection: a held mouse button must not repeat its action every frame.
    private boolean breakHeldPreviously;
    private boolean placeHeldPreviously;

    @Override
    public void update(World world, float dt) {
        int[] players = world.query(PlayerInput.class, Position.class, Rotation.class, CameraComponent.class);
        if (players.length == 0) return;

        Entity      player = new Entity(players[0]);
        PlayerInput input  = world.get(player, PlayerInput.class).orElseThrow();
        Position    pos    = world.get(player, Position.class).orElseThrow();
        Rotation    rot    = world.get(player, Rotation.class).orElseThrow();

        ChunkVoxelWriter writer = ChunkVoxelWriter.snapshot(world);
        Optional<VoxelRaycast.RaycastHit> hit = castFromEye(pos, rot, writer.chunkData());

        updateTarget(world, player, hit);
        applyBreak(world, player, input, hit, writer);
        applyPlace(world, player, input, hit, writer);
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
                            Optional<VoxelRaycast.RaycastHit> hit, ChunkVoxelWriter writer) {
        boolean breakNow    = input.breakBlock();
        boolean justPressed = breakNow && !breakHeldPreviously;
        breakHeldPreviously = breakNow;
        if (!justPressed || hit.isEmpty()) return;

        VoxelRaycast.RaycastHit h = hit.get();
        damageBlock(world, player, h.x(), h.y(), h.z(), writer);
    }

    private void applyPlace(World world, Entity player, PlayerInput input,
                            Optional<VoxelRaycast.RaycastHit> hit, ChunkVoxelWriter writer) {
        boolean placeNow    = input.placeBlock();
        boolean justPressed = placeNow && !placeHeldPreviously;
        placeHeldPreviously = placeNow;
        if (!justPressed || hit.isEmpty()) return;

        VoxelRaycast.RaycastHit h = hit.get();
        int cx = h.x() + h.faceX();
        int cy = h.y() + h.faceY();
        int cz = h.z() + h.faceZ();
        if (!canPlaceAt(world, player, cx, cy, cz, writer)) return;

        writer.write(world, cx, cy, cz, selectedBlock(world, player));
    }

    private static boolean canPlaceAt(World world, Entity player, int cx, int cy, int cz,
                                      ChunkVoxelWriter writer) {
        if (writer.blockAt(cx, cy, cz) != WorldConstants.BLOCK_AIR) return false;
        Position     pos = world.get(player, Position.class).orElseThrow();
        ColliderAABB box = world.get(player, ColliderAABB.class).orElseThrow();
        return !AABBCell.playerOverlapsCell(pos, box, cx, cy, cz);
    }

    // Hook for STEP-16's hotbar: the placed block id will come from the player's selected slot.
    private static byte selectedBlock(World world, Entity player) {
        return WorldConstants.BLOCK_STONE;
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

    private void damageBlock(World world, Entity player, int wx, int wy, int wz, ChunkVoxelWriter writer) {
        int damage = accumulatedDamage(world, player, wx, wy, wz);
        int hardness = BlockType.byId(writer.blockAt(wx, wy, wz)).hardness();
        if (damage >= hardness) {
            writer.write(world, wx, wy, wz, WorldConstants.BLOCK_AIR);
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
}
