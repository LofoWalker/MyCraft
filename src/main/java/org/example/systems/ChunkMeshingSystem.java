package org.example.systems;

import org.example.components.ChunkComponent;
import org.example.components.ChunkMeshComponent;
import org.example.components.VoxelChunkData;
import org.example.ecs.Entity;
import org.example.ecs.GameSystem;
import org.example.ecs.World;
import org.example.render.Mesh;
import org.example.render.TextureAtlas;
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
    private static final int FACE_BOTTOM       = 3;
    private static final int FLOATS_PER_VERTEX = 8; // pos(3) + uv(2) + tint(3)

    // Solid blocks fill their cell to the ceiling; water's top edge is lowered (beta surface).
    private static final float FULL_TOP  = 1.0f;
    private static final float WATER_TOP = 1.0f - WorldConstants.WATER_SURFACE_DROP;

    // Per-vertex UV corner picks, matching the CCW vertex order in FACE_OFFSETS
    // (v0 bottom-left, v1 bottom-right, v2 top-right, v3 top-left). With a non-flipped,
    // top-left-origin atlas, the geometric bottom maps to the larger v (v1 of the tile rect).
    private static final int UV_U0 = 0, UV_V0 = 1, UV_U1 = 2, UV_V1 = 3;
    private static final int[] FACE_UV_CORNER = {
        UV_U0, UV_V1,   // v0 bottom-left
        UV_U1, UV_V1,   // v1 bottom-right
        UV_U1, UV_V0,   // v2 top-right
        UV_U0, UV_V0,   // v3 top-left
    };

    private final List<Mesh> generatedMeshes = new ArrayList<>();

    @Override
    public void update(World world, float dt) {
        for (int eid : world.query(VoxelChunkData.class, ChunkComponent.class)) {
            Entity entity = new Entity(eid);
            if (world.has(entity, ChunkMeshComponent.class)) continue;
            VoxelChunkData data = world.get(entity, VoxelChunkData.class).orElseThrow();
            world.add(entity, buildMeshes(data));
        }
    }

    private ChunkMeshComponent buildMeshes(VoxelChunkData data) {
        ChunkGeometry geo = buildGeometry(data);
        Mesh opaque = Mesh.create(geo.opaque().vertices(), geo.opaque().indices());
        generatedMeshes.add(opaque);
        Mesh water = null;
        if (!geo.water().isEmpty()) {
            water = Mesh.create(geo.water().vertices(), geo.water().indices());
            generatedMeshes.add(water);
        }
        return ChunkMeshComponent.of(opaque, water);
    }

    // Package-private: pure data transform — testable without GL context. Produces opaque solid
    // geometry and translucent water geometry separately so water can be drawn in its own pass.
    static ChunkGeometry buildGeometry(VoxelChunkData data) {
        int SX = WorldConstants.CHUNK_SIZE_XZ;
        int H  = WorldConstants.WORLD_HEIGHT;
        MeshBuilder opaque = new MeshBuilder();
        MeshBuilder water  = new MeshBuilder();
        for (int y = 0; y < H; y++)
            for (int z = 0; z < SX; z++)
                for (int x = 0; x < SX; x++) {
                    byte block = data.get(x, y, z);
                    if (block == WorldConstants.BLOCK_WATER) appendWaterFaces(water, data, x, y, z);
                    else if (block != WorldConstants.BLOCK_AIR) appendVisibleFaces(opaque, data, x, y, z, block);
                }
        return new ChunkGeometry(opaque.toGeometry(), water.toGeometry());
    }

    private static void appendVisibleFaces(MeshBuilder builder, VoxelChunkData data, int x, int y, int z, byte block) {
        for (int face = 0; face < 6; face++) {
            int nx = x + FACE_NEIGHBOR[face][0];
            int ny = y + FACE_NEIGHBOR[face][1];
            int nz = z + FACE_NEIGHBOR[face][2];
            if (isAirOrOutOfBounds(data, nx, ny, nz)) {
                BlockType type = BlockType.byId(block);
                builder.addFace(x, y, z, face, blockFaceUv(type, face), type.tint(), FULL_TOP);
            }
        }
    }

    // A water face is emitted only when its neighbour is NOT water (no faces between two water
    // cells); the water surface is lowered to WATER_TOP so it reads as a sunken sheet.
    private static void appendWaterFaces(MeshBuilder builder, VoxelChunkData data, int x, int y, int z) {
        float[] uv = TextureAtlas.uvForTile(BlockType.WATER.tileTop());
        float[] tint = BlockType.WATER.tint();
        for (int face = 0; face < 6; face++) {
            int nx = x + FACE_NEIGHBOR[face][0];
            int ny = y + FACE_NEIGHBOR[face][1];
            int nz = z + FACE_NEIGHBOR[face][2];
            if (!isWater(data, nx, ny, nz)) {
                builder.addFace(x, y, z, face, uv, tint, WATER_TOP);
            }
        }
    }

    private static boolean isAirOrOutOfBounds(VoxelChunkData data, int x, int y, int z) {
        int SX = WorldConstants.CHUNK_SIZE_XZ;
        if (x < 0 || x >= SX || y < 0 || y >= WorldConstants.WORLD_HEIGHT || z < 0 || z >= SX) return true;
        return data.get(x, y, z) == WorldConstants.BLOCK_AIR;
    }

    private static boolean isWater(VoxelChunkData data, int x, int y, int z) {
        int SX = WorldConstants.CHUNK_SIZE_XZ;
        if (x < 0 || x >= SX || y < 0 || y >= WorldConstants.WORLD_HEIGHT || z < 0 || z >= SX) return false;
        return data.get(x, y, z) == WorldConstants.BLOCK_WATER;
    }

    private static float[] blockFaceUv(BlockType type, int face) {
        int tile = switch (face) {
            case FACE_TOP    -> type.tileTop();
            case FACE_BOTTOM -> type.tileBottom();
            default          -> type.tileSide();
        };
        return TextureAtlas.uvForTile(tile);
    }

    @Override
    public void close() {
        generatedMeshes.forEach(Mesh::close);
        generatedMeshes.clear();
    }

    record Geometry(float[] vertices, int[] indices) {
        boolean isEmpty() { return indices.length == 0; }
    }

    record ChunkGeometry(Geometry opaque, Geometry water) {}

    private static final class MeshBuilder {
        // A full chunk floor exposes at least 32×32 top faces; start there and grow by
        // doubling, so transient buffers track the real mesh size instead of the
        // (vastly larger) theoretical worst case of every block showing all six faces.
        private static final int INITIAL_FACES   = WorldConstants.CHUNK_SIZE_XZ * WorldConstants.CHUNK_SIZE_XZ;
        private static final int FLOATS_PER_FACE = 4 * FLOATS_PER_VERTEX;
        private static final int INTS_PER_FACE   = 6;
        private static final float TOP_Y_OFFSET  = 1.0f;

        private float[] vertices = new float[INITIAL_FACES * FLOATS_PER_FACE];
        private int[]   indices  = new int[INITIAL_FACES * INTS_PER_FACE];
        private int vOffset = 0;
        private int iOffset = 0;

        // topY replaces the y=1 vertices' height so water can sit below the cell ceiling; solid
        // blocks pass topY = 1.0 and are unaffected.
        void addFace(int bx, int by, int bz, int face, float[] uv, float[] tint, float topY) {
            ensureCapacity();
            float[] off = FACE_OFFSETS[face];
            int baseVertex = vOffset / FLOATS_PER_VERTEX;
            for (int v = 0; v < 4; v++) {
                float yOff = off[v * 3 + 1];
                vertices[vOffset++] = bx + off[v * 3];
                vertices[vOffset++] = by + (yOff == TOP_Y_OFFSET ? topY : yOff);
                vertices[vOffset++] = bz + off[v * 3 + 2];
                vertices[vOffset++] = uv[FACE_UV_CORNER[v * 2]];
                vertices[vOffset++] = uv[FACE_UV_CORNER[v * 2 + 1]];
                vertices[vOffset++] = tint[0];
                vertices[vOffset++] = tint[1];
                vertices[vOffset++] = tint[2];
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
