package org.example.systems;

import org.example.components.BlockBreakProgress;
import org.example.components.CameraComponent;
import org.example.components.ColliderAABB;
import org.example.components.Hotbar;
import org.example.components.Inventory;
import org.example.components.InventoryScreen;
import org.example.components.ItemStack;
import org.example.components.PlayerInput;
import org.example.components.Position;
import org.example.components.Rotation;
import org.example.components.TargetedBlock;
import org.example.components.VoxelChunkData;
import org.example.ecs.Entity;
import org.example.ecs.GameSystem;
import org.example.ecs.World;
import org.example.world.AABBCell;
import org.example.world.BlockDrops;
import org.example.world.BlockType;
import org.example.world.ChunkView;
import org.example.world.Inventories;
import org.example.world.ItemRegistry;
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

        // While the inventory screen is open, block interaction is suspended (no break/place).
        if (world.has(player, InventoryScreen.class)) {
            breakHeldPreviously = false;
            placeHeldPreviously = false;
            return;
        }

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

        // Right-clicking a crafting table opens the 3×3 inventory screen instead of placing.
        VoxelRaycast.RaycastHit h = hit.get();
        byte targetBlock = writer.blockAt(h.x(), h.y(), h.z());
        if (targetBlock == WorldConstants.BLOCK_CRAFTING_TABLE) {
            world.add(player, InventoryScreen.open(true));
            return;
        }

        int activeSlot = activeHotbarSlot(world, player);
        ItemStack held = heldStack(world, player, activeSlot);
        if (held.isEmpty()) return;          // empty slot -> nothing to place
        if (!isPlaceableBlock(held)) return; // food / tool / non-block item -> never placed as a block

        int cx = h.x() + h.faceX();
        int cy = h.y() + h.faceY();
        int cz = h.z() + h.faceZ();
        if (!canPlaceAt(world, player, cx, cy, cz, writer)) return;

        writer.write(world, cx, cy, cz, (byte) held.itemId());
        consumeOne(world, player, activeSlot);
    }

    // Only real block ids (1..MAX_BLOCK_ID) can be placed. AIR is a no-op and food/tool ids live above
    // the block range, so eating or wielding a tool never places a phantom block.
    private static boolean isPlaceableBlock(ItemStack held) {
        int id = held.itemId();
        return id > WorldConstants.BLOCK_AIR && id <= WorldConstants.MAX_BLOCK_ID;
    }

    private static boolean canPlaceAt(World world, Entity player, int cx, int cy, int cz,
                                      ChunkVoxelWriter writer) {
        if (writer.blockAt(cx, cy, cz) != WorldConstants.BLOCK_AIR) return false;
        Position     pos = world.get(player, Position.class).orElseThrow();
        ColliderAABB box = world.get(player, ColliderAABB.class).orElseThrow();
        return !AABBCell.playerOverlapsCell(pos, box, cx, cy, cz);
    }

    // The item in the active hotbar slot is what gets placed. Without an Inventory/Hotbar the player
    // holds nothing (ItemStack.EMPTY), so placement becomes a no-op.
    private static ItemStack heldStack(World world, Entity player, int activeSlot) {
        return world.get(player, Inventory.class)
                .map(inv -> Inventories.get(inv, activeSlot))
                .orElse(ItemStack.EMPTY);
    }

    private static int activeHotbarSlot(World world, Entity player) {
        return world.get(player, Hotbar.class).map(Hotbar::selectedSlot).orElse(0);
    }

    private static void consumeOne(World world, Entity player, int activeSlot) {
        world.get(player, Inventory.class)
                .ifPresent(inv -> world.add(player, Inventories.removeOne(inv, activeSlot)));
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
        int activeSlot    = activeHotbarSlot(world, player);
        ItemStack held    = heldStack(world, player, activeSlot);
        byte broken       = writer.blockAt(wx, wy, wz);
        BlockType blockType = BlockType.byId(broken);
        int damage        = previousDamage(world, player, wx, wy, wz)
                            + ItemRegistry.damagePerHit(blockType, held.itemId());

        if (damage >= blockType.hardness()) {
            writer.write(world, wx, wy, wz, WorldConstants.BLOCK_AIR);
            spawnDropIfEarned(world, wx, wy, wz, blockType, held.itemId());
            world.remove(player, BlockBreakProgress.class);
            wearDownTool(world, player, activeSlot, held);
        } else {
            world.add(player, new BlockBreakProgress(wx, wy, wz, damage));
        }
    }

    // Returns accumulated damage from a previous in-progress break on the same voxel, or 0 if none.
    private static int previousDamage(World world, Entity player, int wx, int wy, int wz) {
        return world.get(player, BlockBreakProgress.class)
                .filter(p -> p.targets(wx, wy, wz))
                .map(BlockBreakProgress::damage)
                .orElse(0);
    }

    private static void spawnDropIfEarned(World world, int wx, int wy, int wz,
                                          BlockType blockType, int heldItemId) {
        int dropId = BlockDrops.dropItemId(blockType, heldItemId);
        if (dropId != BlockDrops.NO_DROP) {
            ItemDrops.spawn(world, wx, wy, wz, (byte) dropId);
        }
    }

    private static void wearDownTool(World world, Entity player, int activeSlot, ItemStack held) {
        if (!ItemRegistry.isTool(held.itemId())) return;
        world.get(player, Inventory.class)
                .ifPresent(inv -> world.add(player, Inventories.decrementDurability(inv, activeSlot)));
    }
}
