package org.example.systems;

import org.example.components.ChunkComponent;
import org.example.components.ChunkDirty;
import org.example.components.VoxelChunkData;
import org.example.ecs.Entity;
import org.example.ecs.GameSystem;
import org.example.ecs.World;
import org.example.world.FluidGrid;
import org.example.world.FluidLogic;
import org.example.world.WorldConstants;

import java.util.HashMap;
import java.util.Map;

/**
 * Simulation-side fluid update system. Maintains a queue of world-space cell positions
 * (encoded as {@code long} via {@link #encodePos}) that need re-evaluation each tick.
 *
 * <p>Only queued cells are visited — no global world sweep. Neighbours modified during
 * evaluation are automatically re-enqueued. The queue is bounded by
 * {@link WorldConstants#MAX_FLUID_UPDATES_PER_TICK}; surplus work carries to the next tick.
 *
 * <p>Water ticks every {@link WorldConstants#WATER_TICK_INTERVAL} simulation steps; lava
 * ticks every {@link WorldConstants#LAVA_TICK_INTERVAL} steps (heavier fluid, slower flow).
 *
 * <p>Chunk dirty-marking is done via {@link ChunkDirty} so the existing
 * {@link ChunkStreamingSystem} remesh path is used without modification.
 */
public final class FluidSystem implements GameSystem {

    // Pending fluid cells, position-encoded as long. Using a simple long[] ring to avoid HashMap.
    // The array doubles as both the queue and the "pending" set via a visited bitset is impractical
    // at world scale; instead we allow duplicate enqueues and skip already-air cells on dequeue.
    private long[] queue = new long[WorldConstants.MAX_FLUID_UPDATES_PER_TICK * 4];
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

        boolean waterTick = (tickCounter % WorldConstants.WATER_TICK_INTERVAL) == 0;
        boolean lavaTick  = (tickCounter % WorldConstants.LAVA_TICK_INTERVAL)  == 0;
        if (!waterTick && !lavaTick) return;

        Map<Long, Integer>        chunkEntities = buildEntityMap(world);
        Map<Long, VoxelChunkData> chunkData     = buildDataMap(world, chunkEntities);
        if (chunkData.isEmpty()) return;

        FluidGrid grid = new WorldFluidGrid(chunkData);

        // Seed the queue with all fluid cells that are already loaded (first tick bootstrap).
        if (size == 0) {
            seedFromWorld(chunkData);
        }

        int processed = 0;
        while (size > 0 && processed < WorldConstants.MAX_FLUID_UPDATES_PER_TICK) {
            long pos = dequeue();
            int wx = decodeX(pos), wy = decodeY(pos), wz = decodeZ(pos);
            byte block = grid.getBlock(wx, wy, wz);

            if (!shouldTickFluid(block, waterTick, lavaTick)) continue;

            FluidLogic.evaluateCell(grid, wx, wy, wz, (nx, ny, nz) -> {
                // Only enqueue if the cell is within the loaded region.
                if (chunkData.containsKey(chunkKey(nx, nz))) {
                    enqueue(encodePos(nx, ny, nz));
                }
            });
            processed++;
            markChunkDirty(world, wx, wz, chunkEntities);
        }
    }

    // -----------------------------------------------------------------------
    // Public API: let other systems (e.g. BlockInteractionSystem) seed updates
    // -----------------------------------------------------------------------

    /**
     * Enqueues a world-space cell for fluid re-evaluation on the next eligible tick.
     * Call this whenever a block is placed or removed adjacent to a fluid.
     */
    public void scheduleUpdate(int wx, int wy, int wz) {
        enqueue(encodePos(wx, wy, wz));
    }

    // -----------------------------------------------------------------------
    // Queue (circular long[])
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
    // Position encoding: pack (wx, wy, wz) into a single long
    // wx and wz fit in 24 bits (±8M blocks); wy fits in 9 bits (0..511).
    // Layout: [wx:24][wy:9][wz:24] → 57 bits total (within 64-bit long).
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

    private void seedFromWorld(Map<Long, VoxelChunkData> chunkData) {
        int s = WorldConstants.CHUNK_SIZE_XZ;
        for (Map.Entry<Long, VoxelChunkData> entry : chunkData.entrySet()) {
            long key = entry.getKey();
            int cx = (int) (key >> 32);
            int cz = (int) key;
            VoxelChunkData data = entry.getValue();
            for (int lx = 0; lx < s; lx++) {
                for (int lz = 0; lz < s; lz++) {
                    for (int y = 0; y < WorldConstants.WORLD_HEIGHT; y++) {
                        if (FluidLogic.isFluid(data.get(lx, y, lz))) {
                            enqueue(encodePos(cx * s + lx, y, cz * s + lz));
                        }
                    }
                }
            }
        }
    }

    private static void markChunkDirty(World world, int wx, int wz,
                                       Map<Long, Integer> chunkEntities) {
        int s = WorldConstants.CHUNK_SIZE_XZ;
        long key = chunkKey(Math.floorDiv(wx, s), Math.floorDiv(wz, s));
        Integer eid = chunkEntities.get(key);
        if (eid != null) world.add(new Entity(eid), new ChunkDirty());
    }

    private static boolean shouldTickFluid(byte block, boolean waterTick, boolean lavaTick) {
        if (FluidLogic.isWater(block)) return waterTick;
        if (FluidLogic.isLava(block))  return lavaTick;
        return false;
    }

    static long chunkKey(int cx, int cz) {
        return ((long) cx << 32) | (cz & 0xFFFFFFFFL);
    }

    // -----------------------------------------------------------------------
    // WorldFluidGrid: adapts the chunk maps for FluidLogic
    // -----------------------------------------------------------------------

    private static final class WorldFluidGrid implements FluidGrid {

        private final Map<Long, VoxelChunkData> chunkData;

        WorldFluidGrid(Map<Long, VoxelChunkData> chunkData) {
            this.chunkData = chunkData;
        }

        @Override
        public byte getBlock(int wx, int wy, int wz) {
            if (wy < 0 || wy >= WorldConstants.WORLD_HEIGHT) return WorldConstants.BLOCK_AIR;
            int s   = WorldConstants.CHUNK_SIZE_XZ;
            int cx  = Math.floorDiv(wx, s);
            int cz  = Math.floorDiv(wz, s);
            VoxelChunkData data = chunkData.get(chunkKey(cx, cz));
            if (data == null) return WorldConstants.BLOCK_AIR;
            return data.get(wx - cx * s, wy, wz - cz * s);
        }

        @Override
        public void setBlock(int wx, int wy, int wz, byte blockId) {
            if (wy < 0 || wy >= WorldConstants.WORLD_HEIGHT) return;
            int s   = WorldConstants.CHUNK_SIZE_XZ;
            int cx  = Math.floorDiv(wx, s);
            int cz  = Math.floorDiv(wz, s);
            VoxelChunkData data = chunkData.get(chunkKey(cx, cz));
            if (data == null) return;
            data.set(wx - cx * s, wy, wz - cz * s, blockId);
        }
    }
}
