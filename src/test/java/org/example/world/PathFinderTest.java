package org.example.world;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Unit tests for the grid A* pathfinder, driven by an in-memory solid-cell set. */
class PathFinderTest {

    // A test ChunkView whose solidity is defined by an explicit set of (x,y,z) cells.
    private static final class GridView implements ChunkView {
        private final Set<Long> solid = new HashSet<>();
        void setSolid(int x, int y, int z) { solid.add(key(x, y, z)); }
        @Override public boolean isSolid(int x, int y, int z) { return solid.contains(key(x, y, z)); }
        private static long key(int x, int y, int z) {
            return ((long) (x & 0x1FFFFF) << 42) | ((long) (y & 0x1FFFFF) << 21) | (z & 0x1FFFFF);
        }
    }

    // Flat floor of solid blocks at y=0 over a square region; everything above is air.
    private static GridView flatFloor(int radius) {
        GridView v = new GridView();
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                v.setSolid(x, 0, z);
            }
        }
        return v;
    }

    @Test
    void straightPathStepsTowardGoal() {
        GridView v = flatFloor(8);
        Optional<float[]> wp = PathFinder.nextWaypoint(0, 1, 0, 4, 1, 0, v);

        assertTrue(wp.isPresent(), "a path should exist across flat ground");
        assertTrue(wp.get()[0] > 0.5f, "first waypoint should move toward +x (the goal)");
    }

    @Test
    void routesAroundAWall() {
        GridView v = flatFloor(8);
        // A short wall blocking the straight line at x=2 (z=0), two blocks tall.
        v.setSolid(2, 1, 0);
        v.setSolid(2, 2, 0);

        Optional<float[]> wp = PathFinder.nextWaypoint(0, 1, 0, 4, 1, 0, v);
        assertTrue(wp.isPresent(), "pathfinder should route around the wall");
    }

    @Test
    void unreachableBeyondMaxDistanceReturnsEmpty() {
        GridView v = flatFloor(4);
        int farGoal = WorldConstants.PATHFINDER_MAX_DISTANCE + 8;
        Optional<float[]> wp = PathFinder.nextWaypoint(0, 1, 0, farGoal, 1, 0, v);
        assertTrue(wp.isEmpty(), "goal beyond the max search distance is unreachable");
    }

    @Test
    void searchIsDeterministic() {
        GridView v = flatFloor(8);
        float[] a = PathFinder.nextWaypoint(0, 1, 0, 5, 1, 3, v).orElseThrow();
        float[] b = PathFinder.nextWaypoint(0, 1, 0, 5, 1, 3, v).orElseThrow();
        assertArrayEquals(a, b, "identical queries must yield identical waypoints");
    }
}
