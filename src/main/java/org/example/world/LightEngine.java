package org.example.world;

import org.example.components.VoxelChunkData;

/**
 * Per-chunk light bake: computes a skylight and a blocklight level (0..{@link WorldConstants#MAX_LIGHT_LEVEL})
 * for every cell and packs them into one byte per cell (high nibble = skylight, low nibble = blocklight),
 * using the SAME flat indexing as {@link VoxelChunkData}. The effective brightness of a cell is
 * {@code max(skylight, blocklight)} ({@link #effectiveLevel(byte)}).
 *
 * <p>Both kinds are a breadth-first flood-fill that drops one level per block of travel and stops at
 * opaque (solid) blocks. Skylight is seeded by casting straight down each column from the sky at full
 * level until it meets the first opaque block; blocklight is seeded by each emitter cell
 * ({@link BlockType#lightEmission()} {@code > 0}).
 *
 * <p>Pure CPU, no OpenGL — run on the chunk virtual-thread workers and unit-tested in isolation.
 *
 * <h2>Inter-chunk borders (limitation)</h2>
 * Each chunk is lit in isolation: neighbours are not consulted. Out-of-bounds horizontal neighbours
 * are treated as open sky (light is allowed to leave but nothing flows back in), so light does not
 * bleed correctly across chunk seams — a torch near a chunk edge lights only its own chunk's side,
 * and a wall flush against a seam can leave a one-block-too-dark stripe on the far chunk until a
 * later cross-chunk lighting pass (a future step) is added.
 *
 * <h2>Allocation profile</h2>
 * The BFS uses a single reused primitive {@code int[]} ring of cell indices plus a same-size
 * {@code byte[]} of the level each entry carries — no {@code HashMap}/{@code List}/boxing in the
 * loop. The ring is sized to the chunk cell count and only ever doubled (never per cell); in steady
 * state a cell is enqueued only when its stored level strictly rises, so re-enqueues are few.
 */
public final class LightEngine {

    private LightEngine() {}

    private static final int SKY_SHIFT  = 4;
    private static final int NIBBLE_MASK = 0x0F;
    private static final int LEVEL_DROP_PER_BLOCK = 1;

    // Neighbour offsets (dx,dy,dz) for the 6 axis directions.
    private static final int[][] NEIGHBORS = {
        { 1, 0, 0}, {-1, 0, 0},
        { 0, 1, 0}, { 0,-1, 0},
        { 0, 0, 1}, { 0, 0,-1},
    };

    /** Bakes the packed light field for one chunk. The returned array shares VoxelChunkData indexing. */
    public static byte[] computeLight(VoxelChunkData data) {
        int sx = WorldConstants.CHUNK_SIZE_XZ;
        int h  = WorldConstants.WORLD_HEIGHT;
        byte[] light = new byte[sx * sx * h];
        IndexQueue queue = new IndexQueue(sx * sx * h);

        seedSkylight(data, light, queue);
        propagate(data, light, queue, true);
        seedBlocklight(data, light, queue);
        propagate(data, light, queue, false);
        return light;
    }

    public static int skylight(byte packed)  { return (packed >> SKY_SHIFT) & NIBBLE_MASK; }
    public static int blocklight(byte packed) { return packed & NIBBLE_MASK; }
    public static int effectiveLevel(byte packed) { return Math.max(skylight(packed), blocklight(packed)); }

    private static void seedSkylight(VoxelChunkData data, byte[] light, IndexQueue queue) {
        int sx  = WorldConstants.CHUNK_SIZE_XZ;
        int top = WorldConstants.WORLD_HEIGHT - 1;
        int max = WorldConstants.MAX_LIGHT_LEVEL;
        for (int z = 0; z < sx; z++) {
            for (int x = 0; x < sx; x++) {
                for (int y = top; y >= 0 && isTransparent(data, x, y, z); y--) {
                    int idx = index(x, y, z);
                    setSkylight(light, idx, max);
                    queue.push(idx, max);
                }
            }
        }
    }

    private static void seedBlocklight(VoxelChunkData data, byte[] light, IndexQueue queue) {
        int sx = WorldConstants.CHUNK_SIZE_XZ;
        int h  = WorldConstants.WORLD_HEIGHT;
        for (int y = 0; y < h; y++)
            for (int z = 0; z < sx; z++)
                for (int x = 0; x < sx; x++) {
                    int emission = BlockType.byId(data.get(x, y, z)).lightEmission();
                    if (emission <= 0) continue;
                    int idx = index(x, y, z);
                    setBlocklight(light, idx, emission);
                    queue.push(idx, emission);
                }
    }

    // Shared flood-fill: drains the queue, spreading the carried level (minus one per block) into each
    // transparent neighbour whose stored level is lower. `sky` selects which nibble is written/read.
    private static void propagate(VoxelChunkData data, byte[] light, IndexQueue queue, boolean sky) {
        int sx = WorldConstants.CHUNK_SIZE_XZ;
        while (!queue.isEmpty()) {
            int idx   = queue.popIndex();
            int level = queue.popLevel();
            int next  = level - LEVEL_DROP_PER_BLOCK;
            if (next <= 0) continue;
            int x = idx % sx;
            int z = (idx / sx) % sx;
            int y = idx / (sx * sx);
            spreadToNeighbours(data, light, queue, x, y, z, next, sky);
        }
    }

    private static void spreadToNeighbours(VoxelChunkData data, byte[] light, IndexQueue queue,
                                           int x, int y, int z, int next, boolean sky) {
        for (int[] d : NEIGHBORS) {
            int nx = x + d[0], ny = y + d[1], nz = z + d[2];
            if (!inBounds(nx, ny, nz) || !isTransparent(data, nx, ny, nz)) continue;
            int nIdx = index(nx, ny, nz);
            int current = sky ? skylight(light[nIdx]) : blocklight(light[nIdx]);
            if (current >= next) continue;
            if (sky) setSkylight(light, nIdx, next); else setBlocklight(light, nIdx, next);
            queue.push(nIdx, next);
        }
    }

    private static void setSkylight(byte[] light, int idx, int level) {
        light[idx] = (byte) ((light[idx] & NIBBLE_MASK) | (level << SKY_SHIFT));
    }

    private static void setBlocklight(byte[] light, int idx, int level) {
        light[idx] = (byte) ((light[idx] & (NIBBLE_MASK << SKY_SHIFT)) | level);
    }

    private static boolean isTransparent(VoxelChunkData data, int x, int y, int z) {
        return !BlockType.byId(data.get(x, y, z)).solid();
    }

    private static boolean inBounds(int x, int y, int z) {
        int sx = WorldConstants.CHUNK_SIZE_XZ;
        return x >= 0 && x < sx && z >= 0 && z < sx && y >= 0 && y < WorldConstants.WORLD_HEIGHT;
    }

    private static int index(int x, int y, int z) {
        int sx = WorldConstants.CHUNK_SIZE_XZ;
        return x + z * sx + y * sx * sx;
    }

    // Reused primitive FIFO: parallel int (cell index) / byte (carried level) arrays with head/tail
    // cursors. Grows by doubling only when full — never allocates per enqueued cell.
    private static final class IndexQueue {
        private int[]  indices;
        private byte[] levels;
        private int head;
        private int tail;

        IndexQueue(int capacity) {
            indices = new int[Math.max(capacity, 1)];
            levels  = new byte[Math.max(capacity, 1)];
        }

        boolean isEmpty() { return head == tail; }

        void push(int idx, int level) {
            if (tail == indices.length) compactOrGrow();
            indices[tail] = idx;
            levels[tail]  = (byte) level;
            tail++;
        }

        int popIndex() { return indices[head]; }

        int popLevel() { return levels[head++]; }

        // Reclaim the drained prefix in place before paying for a larger backing array.
        private void compactOrGrow() {
            if (head > 0) {
                int live = tail - head;
                System.arraycopy(indices, head, indices, 0, live);
                System.arraycopy(levels,  head, levels,  0, live);
                head = 0;
                tail = live;
                return;
            }
            indices = java.util.Arrays.copyOf(indices, indices.length * 2);
            levels  = java.util.Arrays.copyOf(levels,  levels.length * 2);
        }
    }
}
