package org.example.worldgen;

import org.example.components.VoxelChunkData;
import org.example.worldgen.stage.CaveStage;
import org.example.worldgen.stage.GenerationStage;
import org.example.worldgen.stage.TerrainStage;
import org.example.worldgen.stage.TreeStage;
import org.example.worldgen.stage.WaterSettleStage;

import java.util.List;

// Single entry point of the world-generation subsystem: runs the ordered generation stages over a
// chunk's voxel data. The same pipeline is used by the chunk-streaming workers and the ECS world
// generator, so every chunk passes through exactly the same stages in the same order.
public final class GenerationPipeline {

    private final List<GenerationStage> stages;

    private GenerationPipeline(List<GenerationStage> stages) {
        this.stages = stages;
    }

    public static GenerationPipeline overworld(long seed) {
        TerrainShape shape = new TerrainShape(seed);
        return new GenerationPipeline(List.of(
                new TerrainStage(shape),
                new CaveStage(seed),
                new WaterSettleStage(),
                new TreeStage(shape, seed)));
    }

    public void generate(VoxelChunkData data, int chunkX, int chunkZ) {
        for (GenerationStage stage : stages) {
            stage.apply(data, chunkX, chunkZ);
        }
    }
}
