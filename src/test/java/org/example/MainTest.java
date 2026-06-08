package org.example;

import org.example.components.ChunkComponent;
import org.example.components.Position;
import org.example.components.VoxelChunkData;
import org.example.ecs.World;
import org.example.world.WorldConstants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MainTest {

    private static final int RADIUS = 3;
    private static final int EXPECTED_CHUNKS = (2 * RADIUS + 1) * (2 * RADIUS + 1); // 7×7 = 49

    @Test
    void spawnChunksCreatesCorrectCount() {
        World world = new World();
        Main.spawnChunks(world);
        assertEquals(EXPECTED_CHUNKS, world.query(ChunkComponent.class, VoxelChunkData.class, Position.class).length);
    }

    @Test
    void chunkPositionsMatchGridCoordinates() {
        World world = new World();
        Main.spawnChunks(world);
        int S = WorldConstants.CHUNK_SIZE;

        for (int eid : world.query(ChunkComponent.class, Position.class)) {
            var entity   = new org.example.ecs.Entity(eid);
            var chunk    = world.get(entity, ChunkComponent.class).orElseThrow();
            var position = world.get(entity, Position.class).orElseThrow();
            assertEquals((float) (chunk.x() * S), position.x(), 1e-6f);
            assertEquals(0f,                       position.y(), 1e-6f);
            assertEquals((float) (chunk.z() * S), position.z(), 1e-6f);
        }
    }

    @Test
    void allSpawnedChunksStartEmpty() {
        World world = new World();
        Main.spawnChunks(world);
        int S = WorldConstants.CHUNK_SIZE;

        for (int eid : world.query(VoxelChunkData.class)) {
            var data = world.get(new org.example.ecs.Entity(eid), VoxelChunkData.class).orElseThrow();
            for (int y = 0; y < S; y++)
                for (int z = 0; z < S; z++)
                    for (int x = 0; x < S; x++)
                        assertEquals(WorldConstants.BLOCK_AIR, data.get(x, y, z));
        }
    }
}
