package org.example.world;

import java.util.Optional;

/**
 * Grid A* pathfinder for mob navigation. Designed to be pure and unit-testable:
 * it reads world data only through a {@link ChunkView}-compatible solid-block predicate.
 *
 * <p>Walkability rule: a cell (x, y, z) is walkable when:
 * <ul>
 *   <li>the cell itself is air (not solid)</li>
 *   <li>the cell above it (y+1) is also air (2-block headroom for the mob)</li>
 *   <li>the cell below it (y-1) is solid (the mob must stand on something)</li>
 * </ul>
 * One-block step-ups and step-downs are included as neighbours.
 *
 * <p>Performance constraints (per STEP-31 spec):
 * <ul>
 *   <li>No {@code HashMap} in the hot loop — open/closed sets are parallel {@code int[]}
 *       direct-address tables keyed by local coordinate offsets from the start node.</li>
 *   <li>Node budget: the search terminates after expanding
 *       {@link WorldConstants#PATHFINDER_NODE_BUDGET} nodes.</li>
 * </ul>
 *
 * <p>Returns only the <em>next-step waypoint</em> (the first node after start on the
 * best path) so callers steer toward a single point and recompute periodically.
 */
public final class PathFinder {

    private PathFinder() {}

    // Size of the direct-address table side: coordinates are offset from start so
    // each axis ranges [-maxDist, +maxDist] → side = maxDist*2+1.
    private static final int SIDE = WorldConstants.PATHFINDER_MAX_DISTANCE * 2 + 1;
    private static final int AREA = SIDE * SIDE * SIDE;
    // Sentinel for unvisited nodes in the g-score table.
    private static final int UNVISITED = Integer.MAX_VALUE;
    // No-parent sentinel in the parent table.
    private static final int NO_PARENT = -1;

    // --- Public API ---

    /**
     * Finds the next waypoint from start toward goal.
     *
     * @return world-space XZ centre + Y floor of the first step block, or empty if
     *         unreachable / budget exceeded
     */
    public static Optional<float[]> nextWaypoint(int startX, int startY, int startZ,
                                                  int goalX,  int goalY,  int goalZ,
                                                  ChunkView solid) {
        if (startX == goalX && startY == goalY && startZ == goalZ) {
            return Optional.of(blockCentre(goalX, goalY, goalZ));
        }

        int maxDist = WorldConstants.PATHFINDER_MAX_DISTANCE;
        if (chebyshev(startX, startY, startZ, goalX, goalY, goalZ) > maxDist) {
            return Optional.empty();
        }

        int[] gScore = new int[AREA];
        int[] parent = new int[AREA];
        java.util.Arrays.fill(gScore, UNVISITED);
        java.util.Arrays.fill(parent, NO_PARENT);

        int startIdx = localIndex(0, 0, 0);
        gScore[startIdx] = 0;

        // Min-heap backed by a flat int array. Each entry packs (f-score | local-index)
        // into a long so the heap orders by f and we retrieve the index on pop.
        // Max heap entries = PATHFINDER_NODE_BUDGET * max_neighbours + 1.
        int maxHeap = WorldConstants.PATHFINDER_NODE_BUDGET * 17 + 1;
        long[] heap  = new long[maxHeap];
        int   heapSz = 0;

        int startF = chebyshev(startX, startY, startZ, goalX, goalY, goalZ);
        heap[heapSz] = packEntry(startF, startIdx);
        siftUp(heap, heapSz++);

        int expanded = 0;

        while (heapSz > 0 && expanded < WorldConstants.PATHFINDER_NODE_BUDGET) {
            long top     = heap[0];
            heap[0]      = heap[--heapSz];
            if (heapSz > 0) siftDown(heap, 0, heapSz);

            int current  = entryIndex(top);
            int[] cOff   = localCoord(current);
            int wx = startX + cOff[0];
            int wy = startY + cOff[1];
            int wz = startZ + cOff[2];

            if (wx == goalX && wy == goalY && wz == goalZ) {
                return reconstructWaypoint(parent, startIdx, current,
                                           startX, startY, startZ);
            }

            int gCur = gScore[current];
            expanded++;

            int[][] nbs = neighbours(wx, wy, wz, solid);
            for (int[] nb : nbs) {
                int nx = nb[0], ny = nb[1], nz = nb[2];
                int ddx = nx - startX, ddy = ny - startY, ddz = nz - startZ;
                if (Math.abs(ddx) > maxDist || Math.abs(ddy) > maxDist
                        || Math.abs(ddz) > maxDist) continue;

                int nIdx = localIndex(ddx, ddy, ddz);
                int gNew = gCur + moveCost(wx, wy, wz, nx, ny, nz);

                if (gNew >= gScore[nIdx]) continue;
                gScore[nIdx] = gNew;
                parent[nIdx] = current;

                if (heapSz < maxHeap) {
                    int fNew = gNew + chebyshev(nx, ny, nz, goalX, goalY, goalZ);
                    heap[heapSz] = packEntry(fNew, nIdx);
                    siftUp(heap, heapSz++);
                }
            }
        }

        // Goal not reached: return best partial step toward goal, or empty.
        return bestPartialWaypoint(gScore, parent, startIdx,
                                   startX, startY, startZ, goalX, goalY, goalZ);
    }

    // --- Walkability & neighbours ---

    /**
     * The set of walkable neighbours of block (wx, wy, wz).
     * Includes flat moves, one-block step-ups, and one-block step-downs.
     * Package-private for testing.
     */
    static int[][] neighbours(int wx, int wy, int wz, ChunkView solid) {
        int[] dx = { 1, -1, 0, 0, 1,  1, -1, -1 };
        int[] dz = { 0,  0, 1,-1, 1, -1,  1, -1 };

        // At most 8 flat + 8 step-up + 8 step-down = 24 candidates.
        int[][] buf  = new int[24][3];
        int     cnt  = 0;

        for (int i = 0; i < 8; i++) {
            int nx = wx + dx[i];
            int nz = wz + dz[i];

            if (isWalkable(nx, wy, nz, solid)) {
                buf[cnt++] = new int[]{ nx, wy, nz };
            } else if (isWalkable(nx, wy + 1, nz, solid)) {
                // Step up: clear the wall at wy AND wy+1 must have headroom (checked inside isWalkable).
                // The extra condition: the wall block at wy must NOT block the step (air at wx,wy+1,wz pair).
                if (!solid.isSolid(wx, wy + 1, wz)) {
                    buf[cnt++] = new int[]{ nx, wy + 1, nz };
                }
            }
            // Step down: target cell is at wy-1.
            if (isWalkable(nx, wy - 1, nz, solid)) {
                buf[cnt++] = new int[]{ nx, wy - 1, nz };
            }
        }

        int[][] result = new int[cnt][3];
        System.arraycopy(buf, 0, result, 0, cnt);
        return result;
    }

    /**
     * A cell is walkable when: the cell is air, cell+1 is air (headroom), cell-1 is solid (floor).
     * Package-private for testing.
     */
    static boolean isWalkable(int wx, int wy, int wz, ChunkView solid) {
        return !solid.isSolid(wx, wy, wz)
            && !solid.isSolid(wx, wy + 1, wz)
            &&  solid.isSolid(wx, wy - 1, wz);
    }

    // --- Heuristic ---

    /**
     * Chebyshev distance: the admissible heuristic for 8-direction grid movement.
     * Package-private for testing.
     */
    static int chebyshev(int ax, int ay, int az, int bx, int by, int bz) {
        return Math.max(Math.abs(ax - bx), Math.max(Math.abs(ay - by), Math.abs(az - bz)));
    }

    // Diagonal horizontal moves cost 141 (≈ √2 × 100); cardinal cost 100; vertical adds 50.
    private static int moveCost(int ax, int ay, int az, int bx, int by, int bz) {
        boolean diagonal = (Math.abs(ax - bx) + Math.abs(az - bz) == 2);
        return (diagonal ? 141 : 100) + Math.abs(ay - by) * 50;
    }

    // --- Path reconstruction ---

    private static Optional<float[]> reconstructWaypoint(int[] parent, int startIdx,
                                                          int goalIdx, int sx, int sy, int sz) {
        // Walk back from goal until we find the node whose parent is start.
        int cursor = goalIdx;
        while (parent[cursor] != NO_PARENT && parent[cursor] != startIdx) {
            cursor = parent[cursor];
        }
        // cursor is now the first step after start (or goal itself if one-step path).
        int[] off = localCoord(cursor);
        return Optional.of(blockCentre(sx + off[0], sy + off[1], sz + off[2]));
    }

    // When goal was not reached, return the first step toward the explored node that is
    // closest to the goal. If no explored node has a parent (empty search), return empty.
    private static Optional<float[]> bestPartialWaypoint(int[] gScore, int[] parent,
                                                          int startIdx, int sx, int sy, int sz,
                                                          int gx, int gy, int gz) {
        int bestIdx  = NO_PARENT;
        int bestDist = Integer.MAX_VALUE;

        for (int idx = 0; idx < AREA; idx++) {
            if (gScore[idx] == UNVISITED || parent[idx] == NO_PARENT) continue;
            int[] c  = localCoord(idx);
            int   wx = sx + c[0], wy = sy + c[1], wz = sz + c[2];
            int   d  = chebyshev(wx, wy, wz, gx, gy, gz);
            if (d < bestDist) { bestDist = d; bestIdx = idx; }
        }

        if (bestIdx == NO_PARENT) return Optional.empty();
        return reconstructWaypoint(parent, startIdx, bestIdx, sx, sy, sz);
    }

    // --- Coordinate helpers (no HashMap) ---

    // offset in [-maxDist, +maxDist] → flat index in [0, AREA).
    private static int localIndex(int dx, int dy, int dz) {
        int half = SIDE / 2;
        return (dx + half) + (dz + half) * SIDE + (dy + half) * SIDE * SIDE;
    }

    // flat index → offset triple [dx, dy, dz] from start.
    private static int[] localCoord(int idx) {
        int half  = SIDE / 2;
        int layer = idx / (SIDE * SIDE);
        int rem   = idx % (SIDE * SIDE);
        int row   = rem / SIDE;
        int col   = rem % SIDE;
        return new int[]{ col - half, layer - half, row - half };
    }

    private static float[] blockCentre(int wx, int wy, int wz) {
        return new float[]{ wx + 0.5f, wy, wz + 0.5f };
    }

    // --- Primitive min-heap on long[] (f packed in high 32 bits, index in low 32 bits) ---

    private static long packEntry(int f, int idx) {
        return ((long) f << 32) | (idx & 0xFFFFFFFFL);
    }

    private static int entryIndex(long entry) {
        return (int) (entry & 0xFFFFFFFFL);
    }

    private static int entryF(long entry) {
        return (int) (entry >>> 32);
    }

    private static void siftUp(long[] heap, int idx) {
        while (idx > 0) {
            int p = (idx - 1) / 2;
            if (entryF(heap[p]) <= entryF(heap[idx])) break;
            long tmp = heap[p]; heap[p] = heap[idx]; heap[idx] = tmp;
            idx = p;
        }
    }

    private static void siftDown(long[] heap, int idx, int size) {
        while (true) {
            int smallest = idx;
            int left  = 2 * idx + 1;
            int right  = 2 * idx + 2;
            if (left  < size && entryF(heap[left])  < entryF(heap[smallest])) smallest = left;
            if (right < size && entryF(heap[right]) < entryF(heap[smallest])) smallest = right;
            if (smallest == idx) break;
            long tmp = heap[smallest]; heap[smallest] = heap[idx]; heap[idx] = tmp;
            idx = smallest;
        }
    }
}
