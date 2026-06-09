package org.example.systems;

import org.example.components.ChunkComponent;
import org.example.components.ChunkMeshComponent;
import org.example.components.VoxelChunkData;
import org.example.ecs.Entity;
import org.example.ecs.GameSystem;
import org.example.ecs.World;
import org.example.render.Mesh;
import org.example.world.BlockType;
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

    // Neighbor offsets (dx,dy,dz) per face — same order as FACE_OFFSETS
    private static final int[][] FACE_NEIGHBOR = {
        { 0,  0,  1}, // Front  (z+)
        { 0,  0, -1}, // Back   (z-)
        { 0,  1,  0}, // Top    (y+)
        { 0, -1,  0}, // Bottom (y-)
        { 1,  0,  0}, // Right  (x+)
        {-1,  0,  0}, // Left   (x-)
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
        int SX = WorldConstants.CHUNK_SIZE_XZ;
        int H  = WorldConstants.WORLD_HEIGHT;
        MeshBuilder builder = new MeshBuilder();
        for (int y = 0; y < H; y++)
            for (int z = 0; z < SX; z++)
                for (int x = 0; x < SX; x++) {
                    byte block = data.get(x, y, z);
                    if (block != WorldConstants.BLOCK_AIR) appendVisibleFaces(builder, data, x, y, z, block);
                }
        return builder.toGeometry();
    }

    private static void appendVisibleFaces(MeshBuilder builder, VoxelChunkData data, int x, int y, int z, byte block) {
        for (int face = 0; face < 6; face++) {
            int nx = x + FACE_NEIGHBOR[face][0];
            int ny = y + FACE_NEIGHBOR[face][1];
            int nz = z + FACE_NEIGHBOR[face][2];
            if (isAirOrOutOfBounds(data, nx, ny, nz)) {
                builder.addFace(x, y, z, face, blockFaceColor(block, face));
            }
        }
    }

    private static boolean isAirOrOutOfBounds(VoxelChunkData data, int x, int y, int z) {
        int SX = WorldConstants.CHUNK_SIZE_XZ;
        if (x < 0 || x >= SX || y < 0 || y >= WorldConstants.WORLD_HEIGHT || z < 0 || z >= SX) return true;
        return data.get(x, y, z) == WorldConstants.BLOCK_AIR;
    }

    private static float[] blockFaceColor(byte block, int face) {
        BlockType type = BlockType.byId(block);
        return (face == FACE_TOP) ? type.colorTop() : type.colorSide();
    }

    @Override
    public void close() {
        generatedMeshes.forEach(Mesh::close);
        generatedMeshes.clear();
    }

    record Geometry(float[] vertices, int[] indices) {}

    private static final class MeshBuilder {
        // A full chunk floor exposes at least 32×32 top faces; start there and grow by
        // doubling, so transient buffers track the real mesh size instead of the
        // (vastly larger) theoretical worst case of every block showing all six faces.
        private static final int INITIAL_FACES   = WorldConstants.CHUNK_SIZE_XZ * WorldConstants.CHUNK_SIZE_XZ;
        private static final int FLOATS_PER_FACE = 4 * FLOATS_PER_VERTEX;
        private static final int INTS_PER_FACE   = 6;

        private float[] vertices = new float[INITIAL_FACES * FLOATS_PER_FACE];
        private int[]   indices  = new int[INITIAL_FACES * INTS_PER_FACE];
        private int vOffset = 0;
        private int iOffset = 0;

        void addFace(int bx, int by, int bz, int face, float[] color) {
            ensureCapacity();
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

        private void ensureCapacity() {
            if (vOffset + FLOATS_PER_FACE > vertices.length) {
                vertices = Arrays.copyOf(vertices, vertices.length * 2);
            }
            if (iOffset + INTS_PER_FACE > indices.length) {
                indices = Arrays.copyOf(indices, indices.length * 2);
            }
        }

        Geometry toGeometry() {
            return new Geometry(
                Arrays.copyOf(vertices, vOffset),
                Arrays.copyOf(indices,  iOffset)
            );
        }
    }
}
