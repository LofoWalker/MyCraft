package org.example.systems;

import org.example.components.ChunkComponent;
import org.example.components.ChunkDirty;
import org.example.components.VoxelChunkData;
import org.example.ecs.Entity;
import org.example.ecs.World;
import org.example.world.WorldConstants;

import java.util.HashMap;
import java.util.Map;

// Shared world-space voxel writer used by both breaking and placing. Resolves the chunk that owns a
// world block, writes the local cell, and flags that chunk dirty so the mesh is rebuilt — handling
// the chunk-wall case where neighbouring blocks live in a different chunk entity. No-op when the
// target chunk is not currently loaded, so edits never escape the streamed region.
final class ChunkVoxelWriter {

    private final Map<Long, Integer>        chunkEntities;
    private final Map<Long, VoxelChunkData> chunkData;

    private ChunkVoxelWriter(Map<Long, Integer> chunkEntities, Map<Long, VoxelChunkData> chunkData) {
        this.chunkEntities = chunkEntities;
        this.chunkData     = chunkData;
    }

    static ChunkVoxelWriter snapshot(World world) {
        Map<Long, Integer>        entities = new HashMap<>();
        Map<Long, VoxelChunkData> data     = new HashMap<>();
        for (int eid : world.query(ChunkComponent.class, VoxelChunkData.class)) {
            Entity         entity = new Entity(eid);
            ChunkComponent chunk  = world.get(entity, ChunkComponent.class).orElseThrow();
            long key = CollisionSystem.chunkKey(chunk.x(), chunk.z());
            entities.put(key, eid);
            data.put(key, world.get(entity, VoxelChunkData.class).orElseThrow());
        }
        return new ChunkVoxelWriter(entities, data);
    }

    Map<Long, VoxelChunkData> chunkData() {
        return chunkData;
    }

    byte blockAt(int wx, int wy, int wz) {
        if (wy < 0 || wy >= WorldConstants.WORLD_HEIGHT) return WorldConstants.BLOCK_AIR;
        int s   = WorldConstants.CHUNK_SIZE_XZ;
        int cx  = Math.floorDiv(wx, s);
        int cz  = Math.floorDiv(wz, s);
        VoxelChunkData data = chunkData.get(CollisionSystem.chunkKey(cx, cz));
        if (data == null) return WorldConstants.BLOCK_AIR;
        return data.get(wx - cx * s, wy, wz - cz * s);
    }

    // Writes blockId at the world cell and marks its owning chunk dirty. No-op if out of the loaded
    // region or outside the vertical world bounds.
    void write(World world, int wx, int wy, int wz, byte blockId) {
        if (wy < 0 || wy >= WorldConstants.WORLD_HEIGHT) return;
        int s   = WorldConstants.CHUNK_SIZE_XZ;
        int cx  = Math.floorDiv(wx, s);
        int cz  = Math.floorDiv(wz, s);
        long key = CollisionSystem.chunkKey(cx, cz);
        Integer entityId = chunkEntities.get(key);
        VoxelChunkData data = chunkData.get(key);
        if (entityId == null || data == null) return;

        data.set(wx - cx * s, wy, wz - cz * s, blockId);
        world.add(new Entity(entityId), new ChunkDirty());
    }
}
