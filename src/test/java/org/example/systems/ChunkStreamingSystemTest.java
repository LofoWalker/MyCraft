package org.example.systems;

import org.example.world.WorldConstants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChunkStreamingSystemTest {

    private static final int S = WorldConstants.CHUNK_SIZE;

    @Test
    void worldToChunkMapsCoordinatesInsideAChunkToTheSameIndex() {
        assertEquals(0, ChunkStreamingSystem.worldToChunk(0f));
        assertEquals(0, ChunkStreamingSystem.worldToChunk(S - 1f));
        assertEquals(1, ChunkStreamingSystem.worldToChunk((float) S));
    }

    @Test
    void worldToChunkFloorsTowardsNegativeInfinity() {
        assertEquals(-1, ChunkStreamingSystem.worldToChunk(-1f));
        assertEquals(-1, ChunkStreamingSystem.worldToChunk(-(float) S));
        assertEquals(-2, ChunkStreamingSystem.worldToChunk(-(float) S - 1f));
    }

    @Test
    void chebyshevIsTheLargerAxisDistance() {
        assertEquals(0, ChunkStreamingSystem.chebyshev(2, 2, 2, 2));
        assertEquals(3, ChunkStreamingSystem.chebyshev(5, 1, 2, 0));
        assertEquals(4, ChunkStreamingSystem.chebyshev(-2, 0, 2, 0));
    }

    @Test
    void chunkKeyRoundTripsThroughDecode() {
        long key = CollisionSystem.chunkKey(-7, 13);
        assertEquals(-7, ChunkStreamingSystem.keyX(key));
        assertEquals(13, ChunkStreamingSystem.keyZ(key));
    }

    @Test
    void chunkInsideUnloadRadiusIsKept() {
        long key = CollisionSystem.chunkKey(3, 0);
        assertFalse(ChunkStreamingSystem.shouldUnload(key, 0, 0, 5));
    }

    @Test
    void chunkBeyondUnloadRadiusIsDropped() {
        long key = CollisionSystem.chunkKey(6, 0);
        assertTrue(ChunkStreamingSystem.shouldUnload(key, 0, 0, 5));
    }
}
