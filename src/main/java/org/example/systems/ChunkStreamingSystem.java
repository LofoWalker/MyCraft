package org.example.systems;

import org.example.components.ChunkComponent;
import org.example.components.ChunkDirty;
import org.example.components.ChunkMeshComponent;
import org.example.components.PlayerInput;
import org.example.components.Position;
import org.example.components.VoxelChunkData;
import org.example.ecs.Entity;
import org.example.ecs.GameSystem;
import org.example.ecs.World;
import org.example.render.Mesh;
import org.example.systems.ChunkMeshingSystem.ChunkGeometry;
import org.example.world.WorldConstants;
import org.example.worldgen.GenerationPipeline;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ChunkStreamingSystem implements GameSystem, AutoCloseable {

    private record ChunkResult(long key, int entityId, VoxelChunkData data, ChunkGeometry geometry) {}

    private final GenerationPipeline generation;
    private final ExecutorService workers = Executors.newVirtualThreadPerTaskExecutor();
    private final ConcurrentLinkedQueue<ChunkResult> ready = new ConcurrentLinkedQueue<>();

    // Authoritative registry of streamed chunks; only mutated on the main thread.
    private final Map<Long, Integer> loadedEntities  = new HashMap<>();
    private final Map<Integer, Mesh> opaqueMeshes     = new HashMap<>();
    private final Map<Integer, Mesh> waterMeshes       = new HashMap<>();

    public ChunkStreamingSystem(long seed) {
        this.generation = GenerationPipeline.overworld(seed);
    }

    @Override
    public void update(World world, float dt) {
        playerChunk(world).ifPresent(pc -> {
            loadMissingChunks(world, pc[0], pc[1]);
            unloadDistantChunks(world, pc[0], pc[1]);
        });
        applyReadyChunks(world);
        remeshDirtyChunks(world);
    }

    // Block edits (BlockInteractionSystem) mutate voxel data in place and flag the chunk dirty.
    // Rebuild the mesh here on the main thread, where this system owns the GL mesh lifecycle.
    private void remeshDirtyChunks(World world) {
        for (int eid : world.query(ChunkDirty.class, VoxelChunkData.class)) {
            Entity entity = new Entity(eid);
            VoxelChunkData data = world.get(entity, VoxelChunkData.class).orElseThrow();
            ChunkGeometry geometry = ChunkMeshingSystem.buildGeometry(data);
            closeMeshes(eid);
            uploadMeshes(world, entity, geometry);
            world.remove(entity, ChunkDirty.class);
        }
    }

    // Uploads opaque + (optional) water geometry to GL and registers both for lifecycle cleanup.
    private void uploadMeshes(World world, Entity entity, ChunkGeometry geometry) {
        int eid = entity.id();
        Mesh opaque = Mesh.create(geometry.opaque().vertices(), geometry.opaque().indices());
        opaqueMeshes.put(eid, opaque);
        Mesh water = null;
        if (geometry.water().indices().length > 0) {
            water = Mesh.create(geometry.water().vertices(), geometry.water().indices());
            waterMeshes.put(eid, water);
        }
        world.add(entity, ChunkMeshComponent.of(opaque, water));
    }

    private void closeMeshes(int entityId) {
        Mesh opaque = opaqueMeshes.remove(entityId);
        if (opaque != null) opaque.close();
        Mesh water = waterMeshes.remove(entityId);
        if (water != null) water.close();
    }

    private Optional<int[]> playerChunk(World world) {
        int[] players = world.query(Position.class, PlayerInput.class);
        if (players.length == 0) return Optional.empty();
        Position p = world.get(new Entity(players[0]), Position.class).orElseThrow();
        return Optional.of(new int[]{ worldToChunk(p.x()), worldToChunk(p.z()) });
    }

    private void loadMissingChunks(World world, int playerCx, int playerCz) {
        int r = WorldConstants.CHUNK_LOAD_RADIUS;
        for (int cx = playerCx - r; cx <= playerCx + r; cx++) {
            for (int cz = playerCz - r; cz <= playerCz + r; cz++) {
                if (!loadedEntities.containsKey(CollisionSystem.chunkKey(cx, cz))) {
                    spawnChunk(world, cx, cz);
                }
            }
        }
    }

    private void spawnChunk(World world, int cx, int cz) {
        int s = WorldConstants.CHUNK_SIZE_XZ;
        Entity entity = world.create();
        world.add(entity, new ChunkComponent(cx, cz));
        world.add(entity, new Position((float) (cx * s), 0f, (float) (cz * s)));
        long key = CollisionSystem.chunkKey(cx, cz);
        loadedEntities.put(key, entity.id());
        submitGeneration(key, entity.id(), cx, cz);
    }

    // Generation + geometry are pure CPU work; they never touch the World or OpenGL.
    private void submitGeneration(long key, int entityId, int cx, int cz) {
        workers.submit(() -> {
            VoxelChunkData data = VoxelChunkData.empty();
            generation.generate(data, cx, cz);
            ChunkGeometry geometry = ChunkMeshingSystem.buildGeometry(data);
            ready.add(new ChunkResult(key, entityId, data, geometry));
        });
    }

    private void unloadDistantChunks(World world, int playerCx, int playerCz) {
        int r = WorldConstants.CHUNK_UNLOAD_RADIUS;
        Iterator<Map.Entry<Long, Integer>> it = loadedEntities.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Long, Integer> entry = it.next();
            if (shouldUnload(entry.getKey(), playerCx, playerCz, r)) {
                unloadChunk(world, entry.getValue());
                it.remove();
            }
        }
    }

    private void unloadChunk(World world, int entityId) {
        closeMeshes(entityId);
        world.destroy(new Entity(entityId));
    }

    // OpenGL is single-threaded: meshes are uploaded here, throttled to bound per-frame spikes.
    private void applyReadyChunks(World world) {
        int budget = WorldConstants.MAX_CHUNK_UPLOADS_PER_FRAME;
        while (budget > 0) {
            ChunkResult result = ready.poll();
            if (result == null) break;
            if (isStale(result)) continue;
            applyChunk(world, result);
            budget--;
        }
    }

    // A chunk unloaded (or recycled) before its result arrived must not be applied.
    private boolean isStale(ChunkResult result) {
        Integer current = loadedEntities.get(result.key());
        return current == null || current != result.entityId();
    }

    private void applyChunk(World world, ChunkResult result) {
        Entity entity = new Entity(result.entityId());
        world.add(entity, result.data());
        uploadMeshes(world, entity, result.geometry());
    }

    @Override
    public void close() {
        workers.shutdownNow();
        opaqueMeshes.values().forEach(Mesh::close);
        waterMeshes.values().forEach(Mesh::close);
        opaqueMeshes.clear();
        waterMeshes.clear();
        loadedEntities.clear();
    }

    static int worldToChunk(float worldCoord) {
        return Math.floorDiv((int) Math.floor(worldCoord), WorldConstants.CHUNK_SIZE_XZ);
    }

    static boolean shouldUnload(long key, int playerCx, int playerCz, int unloadRadius) {
        return chebyshev(keyX(key), keyZ(key), playerCx, playerCz) > unloadRadius;
    }

    static int chebyshev(int cx, int cz, int playerCx, int playerCz) {
        return Math.max(Math.abs(cx - playerCx), Math.abs(cz - playerCz));
    }

    static int keyX(long key) { return (int) (key >> 32); }

    static int keyZ(long key) { return (int) key; }
}
