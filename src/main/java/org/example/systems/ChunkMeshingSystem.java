package org.example.systems;

import org.example.components.ChunkComponent;
import org.example.components.ChunkMeshComponent;
import org.example.components.VoxelChunkData;
import org.example.ecs.Entity;
import org.example.ecs.GameSystem;
import org.example.ecs.World;
import org.example.render.Mesh;
import org.example.world.WorldConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ChunkMeshingSystem implements GameSystem, AutoCloseable {

    // Vertex offsets (x,y,z) for 4 CCW vertices per face — derived from Mesh.buildCubeVertices()
    private static final float[][] FACE_OFFSETS = {
        { 0,0,1,  1,0,1,  1,1,1,  0,1,1 }, // Front  (z+)
        { 1,0,0,  0,0,0,  0,1,0,  1,1,0 }, // Back   (z-)
        { 0,1,1,  1,1,1,  1,1,0,  0,1,0 }, // Top    (y+)
        { 0,0,0,  1,0,0,  1,0,1,  0,0,1 }, // Bottom (y-)
        { 1,0,1,  1,0,0,  1,1,0,  1,1,1 }, // Right  (x+)
        { 0,0,0,  0,0,1,  0,1,1,  0,1,0 }, // Left   (x-)
    };

    private static final int FACE_TOP          = 2;
    private static final int FLOATS_PER_VERTEX = 6;

    private final List<Mesh> generatedMeshes = new ArrayList<>();

    @Override
    public void update(World world, float dt) {
        for (int eid : world.query(VoxelChunkData.class, ChunkComponent.class)) {
            Entity entity = new Entity(eid);
            if (world.has(entity, ChunkMeshComponent.class)) continue;
            VoxelChunkData data = world.get(entity, VoxelChunkData.class).orElseThrow();
            Mesh mesh = buildMesh(data);
            generatedMeshes.add(mesh);
            world.add(entity, new ChunkMeshComponent(mesh));
        }
    }

    private static Mesh buildMesh(VoxelChunkData data) {
        Geometry geo = buildGeometry(data);
        return Mesh.create(geo.vertices(), geo.indices());
    }

    // Package-private: pure data transform — testable without GL context
    static Geometry buildGeometry(VoxelChunkData data) {
        int S = WorldConstants.CHUNK_SIZE;
        MeshBuilder builder = new MeshBuilder(S * S * S);
        for (int y = 0; y < S; y++)
            for (int z = 0; z < S; z++)
                for (int x = 0; x < S; x++) {
                    byte block = data.get(x, y, z);
                    if (block != WorldConstants.BLOCK_AIR) appendBlock(builder, x, y, z, block);
                }
        return builder.toGeometry();
    }

    private static void appendBlock(MeshBuilder builder, int x, int y, int z, byte block) {
        for (int face = 0; face < 6; face++) {
            builder.addFace(x, y, z, face, blockFaceColor(block, face));
        }
    }

    private static float[] blockFaceColor(byte block, int face) {
        return switch (block) {
            case WorldConstants.BLOCK_STONE -> new float[]{ 0.50f, 0.50f, 0.50f };
            case WorldConstants.BLOCK_DIRT  -> new float[]{ 0.55f, 0.35f, 0.15f };
            case WorldConstants.BLOCK_GRASS -> (face == FACE_TOP)
                    ? new float[]{ 0.35f, 0.70f, 0.25f }
                    : new float[]{ 0.55f, 0.35f, 0.15f };
            default -> new float[]{ 1.0f, 0.0f, 1.0f }; // magenta = unknown block type
        };
    }

    @Override
    public void close() {
        generatedMeshes.forEach(Mesh::close);
        generatedMeshes.clear();
    }

    record Geometry(float[] vertices, int[] indices) {}

    private static final class MeshBuilder {
        private final float[] vertices;
        private final int[]   indices;
        private int vOffset = 0;
        private int iOffset = 0;

        MeshBuilder(int maxBlocks) {
            vertices = new float[maxBlocks * 6 * 4 * FLOATS_PER_VERTEX];
            indices  = new int[maxBlocks * 6 * 6];
        }

        void addFace(int bx, int by, int bz, int face, float[] color) {
            float[] off = FACE_OFFSETS[face];
            int baseVertex = vOffset / FLOATS_PER_VERTEX;
            for (int v = 0; v < 4; v++) {
                vertices[vOffset++] = bx + off[v * 3];
                vertices[vOffset++] = by + off[v * 3 + 1];
                vertices[vOffset++] = bz + off[v * 3 + 2];
                vertices[vOffset++] = color[0];
                vertices[vOffset++] = color[1];
                vertices[vOffset++] = color[2];
            }
            indices[iOffset++] = baseVertex;
            indices[iOffset++] = baseVertex + 1;
            indices[iOffset++] = baseVertex + 2;
            indices[iOffset++] = baseVertex + 2;
            indices[iOffset++] = baseVertex + 3;
            indices[iOffset++] = baseVertex;
        }

        Geometry toGeometry() {
            return new Geometry(
                Arrays.copyOf(vertices, vOffset),
                Arrays.copyOf(indices,  iOffset)
            );
        }
    }
}
