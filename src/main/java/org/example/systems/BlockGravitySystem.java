package org.example.systems;

import org.example.components.ChunkComponent;
import org.example.components.ChunkDirty;
import org.example.components.VoxelChunkData;
import org.example.ecs.Entity;
import org.example.ecs.GameSystem;
import org.example.ecs.World;
import org.example.world.BlockGravityRules;
import org.example.world.FluidGrid;
import org.example.world.WorldConstants;

import java.util.HashMap;
import java.util.Map;

/**
 * Moves gravity-affected blocks (sand, gravel) downward one cell per eligible tick until
 * they come to rest on a solid surface. Uses the same update-queue model as
 * {@link FluidSystem}: no global sweep, only queued cells are evaluated.
 *
 * <p>Neighbours above the moved block are re-enqueued automatically so chained columns
 * collapse in O(column-height) ticks instead of one big pass.
 *
 * <p>Other systems (e.g. {@link BlockInteractionSystem}) seed the queue by calling
 * {@link #scheduleUpdate} after any block edit near a gravity-affected block.
 */
public final class BlockGravitySystem implements GameSystem {

    private long[] queue = new long[256];
    private int head = 0;
    private int tail = 0;
    private int size = 0;

    private int tickCounter = 0;

    // -----------------------------------------------------------------------
    // GameSystem
    // -----------------------------------------------------------------------

    @Override
    public void update(World world, float dt) {
        tickCounter++;
        if (tickCounter % WorldConstants.GRAVITY_TICK_INTERVAL != 0) return;

        Map<Long, Integer>        chunkEntities = buildEntityMap(world);
        Map<Long, VoxelChunkData> chunkData     = buildDataMap(world, chunkEntities);
        if (chunkData.isEmpty()) return;

        FluidGrid grid = new GravityGrid(chunkData);

        int processed = 0;
        while (size > 0 && processed < WorldConstants.MAX_FLUID_UPDATES_PER_TICK) {
            long pos = dequeue();
            int wx = decodeX(pos), wy = decodeY(pos), wz = decodeZ(pos);
            if (!BlockGravityRules.shouldFall(grid, wx, wy, wz)) continue;

            BlockGravityRules.stepFall(grid, wx, wy, wz);
            processed++;
            markChunkDirty(world, wx, wz, chunkEntities);
            // Re-queue the new position (block fell to wy-1) and the cell above (may now fall too).
            enqueueIfLoaded(wx, wy - 1, wz, chunkData);
            enqueueIfLoaded(wx, wy + 1, wz, chunkData);
        }
    }

    // -----------------------------------------------------------------------
    // Public API — let other systems seed gravity updates
    // -----------------------------------------------------------------------

    /**
     * Enqueues a world-space cell for gravity evaluation on the next eligible tick.
     * Call this when a block is placed or removed and a neighbouring cell above may be
     * a gravity block that just lost its support.
     */
    public void scheduleUpdate(int wx, int wy, int wz) {
        enqueue(encodePos(wx, wy, wz));
    }

    // -----------------------------------------------------------------------
    // Queue
    // -----------------------------------------------------------------------

    private void enqueue(long pos) {
        if (size >= queue.length) growQueue();
        queue[tail] = pos;
        tail = (tail + 1) % queue.length;
        size++;
    }

    private long dequeue() {
        long pos = queue[head];
        head = (head + 1) % queue.length;
        size--;
        return pos;
    }

    private void growQueue() {
        long[] bigger = new long[queue.length * 2];
        for (int i = 0; i < size; i++) {
            bigger[i] = queue[(head + i) % queue.length];
        }
        queue = bigger;
        head = 0;
        tail = size;
    }

    // -----------------------------------------------------------------------
    // Position encoding (matches FluidSystem layout)
    // -----------------------------------------------------------------------

    static long encodePos(int wx, int wy, int wz) {
        return ((long) wx << 33) | ((long) (wy & 0x1FF) << 24) | (wz & 0xFFFFFF);
    }

    static int decodeX(long pos) { return (int) (pos >> 33); }
    static int decodeY(long pos) { return (int) ((pos >> 24) & 0x1FF); }
    static int decodeZ(long pos) { return (int) (pos & 0xFFFFFF); }

    // -----------------------------------------------------------------------
    // World helpers
    // -----------------------------------------------------------------------

    private static Map<Long, Integer> buildEntityMap(World world) {
        Map<Long, Integer> map = new HashMap<>();
        for (int eid : world.query(ChunkComponent.class, VoxelChunkData.class)) {
            ChunkComponent chunk = world.get(new Entity(eid), ChunkComponent.class).orElseThrow();
            map.put(chunkKey(chunk.x(), chunk.z()), eid);
        }
        return map;
    }

    private static Map<Long, VoxelChunkData> buildDataMap(World world,
                                                          Map<Long, Integer> entities) {
        Map<Long, VoxelChunkData> map = new HashMap<>();
        for (Map.Entry<Long, Integer> entry : entities.entrySet()) {
            VoxelChunkData data = world.get(new Entity(entry.getValue()), VoxelChunkData.class)
                                       .orElseThrow();
            map.put(entry.getKey(), data);
        }
        return map;
    }

    private void enqueueIfLoaded(int wx, int wy, int wz, Map<Long, VoxelChunkData> chunkData) {
        int s = WorldConstants.CHUNK_SIZE_XZ;
        if (wy < 0 || wy >= WorldConstants.WORLD_HEIGHT) return;
        long key = chunkKey(Math.floorDiv(wx, s), Math.floorDiv(wz, s));
        if (chunkData.containsKey(key)) enqueue(encodePos(wx, wy, wz));
    }

    private static void markChunkDirty(World world, int wx, int wz,
                                       Map<Long, Integer> chunkEntities) {
        int s = WorldConstants.CHUNK_SIZE_XZ;
        long key = chunkKey(Math.floorDiv(wx, s), Math.floorDiv(wz, s));
        Integer eid = chunkEntities.get(key);
        if (eid != null) world.add(new Entity(eid), new ChunkDirty());
    }

    static long chunkKey(int cx, int cz) {
        return ((long) cx << 32) | (cz & 0xFFFFFFFFL);
    }

    // -----------------------------------------------------------------------
    // GravityGrid: adapts the loaded chunk data for BlockGravityRules
    // -----------------------------------------------------------------------

    private static final class GravityGrid implements FluidGrid {

        private final Map<Long, VoxelChunkData> chunkData;

        GravityGrid(Map<Long, VoxelChunkData> chunkData) {
            this.chunkData = chunkData;
        }

        @Override
        public byte getBlock(int wx, int wy, int wz) {
            if (wy < 0 || wy >= WorldConstants.WORLD_HEIGHT) return WorldConstants.BLOCK_AIR;
            int s  = WorldConstants.CHUNK_SIZE_XZ;
            int cx = Math.floorDiv(wx, s);
            int cz = Math.floorDiv(wz, s);
            VoxelChunkData data = chunkData.get(chunkKey(cx, cz));
            if (data == null) return WorldConstants.BLOCK_AIR;
            return data.get(wx - cx * s, wy, wz - cz * s);
        }

        @Override
        public void setBlock(int wx, int wy, int wz, byte blockId) {
            if (wy < 0 || wy >= WorldConstants.WORLD_HEIGHT) return;
            int s  = WorldConstants.CHUNK_SIZE_XZ;
            int cx = Math.floorDiv(wx, s);
            int cz = Math.floorDiv(wz, s);
            VoxelChunkData data = chunkData.get(chunkKey(cx, cz));
            if (data == null) return;
            data.set(wx - cx * s, wy, wz - cz * s, blockId);
        }
    }
}
