package org.example.systems;

import org.example.components.ChunkComponent;
import org.example.components.ChunkDirty;
import org.example.components.ChunkLight;
import org.example.components.VoxelChunkData;
import org.example.ecs.Entity;
import org.example.ecs.GameSystem;
import org.example.ecs.World;
import org.example.world.LightEngine;
import org.example.world.RandomTickRules;
import org.example.world.WorldConstants;

import java.util.Random;

/**
 * Random tick system: for every loaded chunk, samples {@link WorldConstants#RANDOM_TICKS_PER_CHUNK}
 * random cells per simulation tick and applies {@link RandomTickRules}.
 *
 * <p>The RNG is seeded by {@code chunkKey ^ tickCounter} so tests can reproduce the same sequence
 * deterministically. No global sweep: cost is O(loadedChunks × RANDOM_TICKS_PER_CHUNK) per tick,
 * a small constant regardless of world size.
 *
 * <p>Changes are written directly into the chunk's {@link VoxelChunkData} and the chunk is flagged
 * {@link ChunkDirty} so the existing remesh path handles the visual update.
 */
public final class RandomTickSystem implements GameSystem {

    private long tickCounter = 0;

    @Override
    public void update(World world, float dt) {
        tickCounter++;
        for (int eid : world.query(ChunkComponent.class, VoxelChunkData.class)) {
            Entity         entity = new Entity(eid);
            ChunkComponent chunk  = world.get(entity, ChunkComponent.class).orElseThrow();
            VoxelChunkData data   = world.get(entity, VoxelChunkData.class).orElseThrow();
            ChunkLight     light  = world.get(entity, ChunkLight.class).orElse(ChunkLight.empty());
            tickChunk(world, entity, chunk, data, light);
        }
    }

    private void tickChunk(World world, Entity entity, ChunkComponent chunk,
                           VoxelChunkData data, ChunkLight light) {
        int s    = WorldConstants.CHUNK_SIZE_XZ;
        int h    = WorldConstants.WORLD_HEIGHT;
        int size = s * s * h;
        // Seed combines chunk coordinates and tick count for deterministic but varied sequences.
        long seed = ((long) chunk.x() * 0x9E3779B97F4A7C15L)
                  ^ ((long) chunk.z() * 0x6C62272E07BB0142L)
                  ^ tickCounter;
        Random rng = new Random(seed);
        boolean changed = false;

        for (int i = 0; i < WorldConstants.RANDOM_TICKS_PER_CHUNK; i++) {
            int idx = rng.nextInt(size);
            int lx  = idx % s;
            int lz  = (idx / s) % s;
            int ly  = idx / (s * s);
            changed |= applyRules(data, light, lx, ly, lz, s);
        }
        if (changed) world.add(entity, new ChunkDirty());
    }

    private static boolean applyRules(VoxelChunkData data, ChunkLight light,
                                      int lx, int ly, int lz, int s) {
        byte current = data.get(lx, ly, lz);
        byte above   = (ly + 1 < WorldConstants.WORLD_HEIGHT)
                       ? data.get(lx, ly + 1, lz)
                       : WorldConstants.BLOCK_AIR;
        int skylight = LightEngine.skylight(light.light()[lx + lz * s + ly * s * s]);
        boolean hasGrassNeighbour = hasGrassNeighbour(data, lx, ly, lz, s);

        byte result = RandomTickRules.evaluate(current, above, skylight, hasGrassNeighbour);
        if (result == current) return false;
        data.set(lx, ly, lz, result);
        return true;
    }

    private static boolean hasGrassNeighbour(VoxelChunkData data, int lx, int ly, int lz, int s) {
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] d : dirs) {
            int nx = lx + d[0];
            int nz = lz + d[1];
            if (nx < 0 || nx >= s || nz < 0 || nz >= s) continue;
            if (data.get(nx, ly, nz) == WorldConstants.BLOCK_GRASS) return true;
        }
        return false;
    }
}
