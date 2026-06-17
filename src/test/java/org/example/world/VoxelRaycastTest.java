package org.example.world;

import org.example.world.VoxelRaycast.RaycastHit;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class VoxelRaycastTest {

    private static final float REACH = 5f;

    // In-memory ChunkView: a set of solid cell coordinates, no GL or ECS needed.
    private static final class SolidSet implements ChunkView {
        private final Set<Long> solids = new HashSet<>();

        SolidSet solid(int x, int y, int z) {
            solids.add(key(x, y, z));
            return this;
        }

        @Override
        public boolean isSolid(int wx, int wy, int wz) {
            return solids.contains(key(wx, wy, wz));
        }

        private static long key(int x, int y, int z) {
            return ((long) (x & 0x1FFFFF) << 42) | ((long) (y & 0x1FFFFF) << 21) | (z & 0x1FFFFF);
        }
    }

    @Test
    void straightRayHitsSolidBlockAtCorrectCoordinates() {
        ChunkView view = new SolidSet().solid(2, 60, 2);

        Optional<RaycastHit> hit = VoxelRaycast.cast(2.5f, 60.5f, 6.5f, 0f, 0f, -1f, REACH, view);

        assertTrue(hit.isPresent());
        assertEquals(2,  hit.get().x());
        assertEquals(60, hit.get().y());
        assertEquals(2,  hit.get().z());
    }

    @Test
    void emptyWhenRayTravelsThroughTheVoid() {
        ChunkView view = new SolidSet();

        Optional<RaycastHit> hit = VoxelRaycast.cast(2.5f, 60.5f, 6.5f, 0f, 0f, -1f, REACH, view);

        assertTrue(hit.isEmpty());
    }

    @Test
    void emptyWhenSolidBlockIsBeyondReach() {
        ChunkView view = new SolidSet().solid(2, 60, 2);

        Optional<RaycastHit> hit = VoxelRaycast.cast(2.5f, 60.5f, 30.5f, 0f, 0f, -1f, REACH, view);

        assertTrue(hit.isEmpty());
    }

    @Test
    void hitsBlockJustWithinReachButNotJustBeyond() {
        ChunkView view = new SolidSet().solid(0, 0, 0);

        // Origin 5.4 blocks away along -z: the far face of the block is at z=1, just within reach.
        Optional<RaycastHit> within = VoxelRaycast.cast(0.5f, 0.5f, 5.9f, 0f, 0f, -1f, REACH, view);
        assertTrue(within.isPresent());

        Optional<RaycastHit> beyond = VoxelRaycast.cast(0.5f, 0.5f, 6.5f, 0f, 0f, -1f, REACH, view);
        assertTrue(beyond.isEmpty());
    }

    @Test
    void lookingDownReportsTopFaceNormal() {
        ChunkView view = new SolidSet().solid(0, 0, 0);

        Optional<RaycastHit> hit = VoxelRaycast.cast(0.5f, 3.5f, 0.5f, 0f, -1f, 0f, REACH, view);

        assertTrue(hit.isPresent());
        assertEquals(0,  hit.get().faceX());
        assertEquals(1,  hit.get().faceY());
        assertEquals(0,  hit.get().faceZ());
    }

    @Test
    void lookingAlongPositiveZReportsNegativeZFaceNormal() {
        ChunkView view = new SolidSet().solid(0, 0, 4);

        Optional<RaycastHit> hit = VoxelRaycast.cast(0.5f, 0.5f, 0.5f, 0f, 0f, 1f, REACH, view);

        assertTrue(hit.isPresent());
        assertEquals(0,  hit.get().faceX());
        assertEquals(0,  hit.get().faceY());
        assertEquals(-1, hit.get().faceZ());
    }

    @Test
    void lookingAlongNegativeXReportsPositiveXFaceNormal() {
        ChunkView view = new SolidSet().solid(-4, 0, 0);

        Optional<RaycastHit> hit = VoxelRaycast.cast(0.5f, 0.5f, 0.5f, -1f, 0f, 0f, REACH, view);

        assertTrue(hit.isPresent());
        assertEquals(1, hit.get().faceX());
        assertEquals(0, hit.get().faceY());
        assertEquals(0, hit.get().faceZ());
    }
}
