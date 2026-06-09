package org.example.systems;

import org.example.components.ChunkComponent;
import org.example.components.ChunkGenerated;
import org.example.components.VoxelChunkData;
import org.example.ecs.Entity;
import org.example.ecs.GameSystem;
import org.example.ecs.World;
import org.example.worldgen.GenerationPipeline;

// ECS adapter over the world-generation subsystem: finds chunks that still need terrain and runs
// the generation pipeline on them. All generation logic lives in org.example.worldgen.
public final class WorldGenSystem implements GameSystem {

    private final GenerationPipeline pipeline;

    public WorldGenSystem(long seed) {
        this.pipeline = GenerationPipeline.overworld(seed);
    }

    @Override
    public void update(World world, float dt) {
        for (int eid : world.query(VoxelChunkData.class, ChunkComponent.class)) {
            Entity entity = new Entity(eid);
            if (world.has(entity, ChunkGenerated.class)) continue;
            ChunkComponent chunk = world.get(entity, ChunkComponent.class).orElseThrow();
            VoxelChunkData data  = world.get(entity, VoxelChunkData.class).orElseThrow();
            pipeline.generate(data, chunk.x(), chunk.z());
            world.add(entity, new ChunkGenerated());
        }
    }
}
