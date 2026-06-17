package org.example.systems;

import org.example.components.ChunkComponent;
import org.example.components.ChunkLight;
import org.example.components.ChunkMeshComponent;
import org.example.components.VoxelChunkData;
import org.example.ecs.Entity;
import org.example.ecs.GameSystem;
import org.example.ecs.World;
import org.example.render.Mesh;
import org.example.render.TextureAtlas;
import org.example.world.AmbientOcclusion;
import org.example.world.BlockType;
import org.example.world.LightEngine;
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

    // Face plane basis (normal, in-plane axes u and v), same face order as FACE_OFFSETS. Used to
    // derive the AO corner-neighbour offsets; a vertex at the high end of an axis (coord 1) reaches
    // toward +axis, at the low end (coord 0) toward -axis.
    private static final int[][] FACE_NORMAL = {
        { 0, 0, 1}, { 0, 0,-1}, { 0, 1, 0}, { 0,-1, 0}, { 1, 0, 0}, {-1, 0, 0},
    };
    private static final int[][] FACE_AXIS_U = {
        { 1, 0, 0}, { 1, 0, 0}, { 1, 0, 0}, { 1, 0, 0}, { 0, 0, 1}, { 0, 0, 1},
    };
    private static final int[][] FACE_AXIS_V = {
        { 0, 1, 0}, { 0, 1, 0}, { 0, 0, 1}, { 0, 0, 1}, { 0, 1, 0}, { 0, 1, 0},
    };

    private static final int VERTICES_PER_FACE = 4;
    private static final int AO_INTS_PER_VERTEX = 9;

    // AO corner-neighbour offsets relative to the block: [face][vertex] -> 9 ints
    // {side1(dx,dy,dz), side2(dx,dy,dz), corner(dx,dy,dz)}, all on the exposed (normal) side.
    private static final int[][][] FACE_VERTEX_AO_OFFSETS = buildAoOffsets();

    private static final int FACE_TOP          = 2;
    private static final int FACE_BOTTOM       = 3;
    private static final int FLOATS_PER_VERTEX = 9; // pos(3) + uv(2) + tint(3) + light(1)

    // An exposed face borders an out-of-bounds neighbour (chunk seam) where no light is computed; we
    // assume open sky there so seams stay lit rather than artificially dark (see LightEngine borders).
    private static final float EDGE_LIGHT = 1.0f;
    private static final float LIGHT_NORMALIZER = WorldConstants.MAX_LIGHT_LEVEL;

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
            byte[] light = world.get(entity, ChunkLight.class)
                    .map(ChunkLight::light)
                    .orElseGet(() -> LightEngine.computeLight(data));
            world.add(entity, buildMeshes(data, light));
        }
    }

    private ChunkMeshComponent buildMeshes(VoxelChunkData data, byte[] light) {
        ChunkGeometry geo = buildGeometry(data, light);
        Mesh opaque = Mesh.create(geo.opaque().vertices(), geo.opaque().indices());
        generatedMeshes.add(opaque);
        Mesh water = null;
        if (!geo.water().isEmpty()) {
            water = Mesh.create(geo.water().vertices(), geo.water().indices());
            generatedMeshes.add(water);
        }
        return ChunkMeshComponent.of(opaque, water);
    }

    // Test/fallback overload: bakes the chunk's light on the spot. The live streaming path passes a
    // precomputed light array (computed on the workers alongside generation/remesh).
    static ChunkGeometry buildGeometry(VoxelChunkData data) {
        return buildGeometry(data, LightEngine.computeLight(data));
    }

    // Package-private: pure data transform — testable without GL context. Produces opaque solid
    // geometry and translucent water geometry separately so water can be drawn in its own pass. Each
    // exposed face carries the light level of the air cell it faces (normalized to 0..1).
    static ChunkGeometry buildGeometry(VoxelChunkData data, byte[] light) {
        int SX = WorldConstants.CHUNK_SIZE_XZ;
        int H  = WorldConstants.WORLD_HEIGHT;
        MeshBuilder opaque = new MeshBuilder();
        MeshBuilder water  = new MeshBuilder();
        for (int y = 0; y < H; y++)
            for (int z = 0; z < SX; z++)
                for (int x = 0; x < SX; x++) {
                    byte block = data.get(x, y, z);
                    if (block == WorldConstants.BLOCK_WATER) appendWaterFaces(water, data, light, x, y, z);
                    else if (block != WorldConstants.BLOCK_AIR) appendVisibleFaces(opaque, data, light, x, y, z, block);
                }
        return new ChunkGeometry(opaque.toGeometry(), water.toGeometry());
    }

    private static void appendVisibleFaces(MeshBuilder builder, VoxelChunkData data, byte[] light,
                                           int x, int y, int z, byte block) {
        for (int face = 0; face < 6; face++) {
            int nx = x + FACE_NEIGHBOR[face][0];
            int ny = y + FACE_NEIGHBOR[face][1];
            int nz = z + FACE_NEIGHBOR[face][2];
            if (isAirOrOutOfBounds(data, nx, ny, nz)) {
                BlockType type = BlockType.byId(block);
                computeVertexLight(data, x, y, z, face, faceLight(light, nx, ny, nz), builder);
                builder.addFace(x, y, z, face, blockFaceUv(type, face), type.tint(), FULL_TOP);
            }
        }
    }

    // A water face is emitted only when its neighbour is NOT water (no faces between two water
    // cells); the water surface is lowered to WATER_TOP so it reads as a sunken sheet.
    private static void appendWaterFaces(MeshBuilder builder, VoxelChunkData data, byte[] light,
                                         int x, int y, int z) {
        float[] uv = TextureAtlas.uvForTile(BlockType.WATER.tileTop());
        float[] tint = BlockType.WATER.tint();
        for (int face = 0; face < 6; face++) {
            int nx = x + FACE_NEIGHBOR[face][0];
            int ny = y + FACE_NEIGHBOR[face][1];
            int nz = z + FACE_NEIGHBOR[face][2];
            if (!isWater(data, nx, ny, nz)) {
                computeVertexLight(data, x, y, z, face, faceLight(light, nx, ny, nz), builder);
                builder.addFace(x, y, z, face, uv, tint, WATER_TOP);
            }
        }
    }

    // Normalized brightness of the (air) cell a face is exposed to. Out-of-bounds neighbours sit on a
    // chunk seam with no baked light, so they read as open sky (EDGE_LIGHT) — see the LightEngine note.
    private static float faceLight(byte[] light, int nx, int ny, int nz) {
        int SX = WorldConstants.CHUNK_SIZE_XZ;
        if (nx < 0 || nx >= SX || ny < 0 || ny >= WorldConstants.WORLD_HEIGHT || nz < 0 || nz >= SX) {
            return EDGE_LIGHT;
        }
        int idx = nx + nz * SX + ny * SX * SX;
        return LightEngine.effectiveLevel(light[idx]) / LIGHT_NORMALIZER;
    }

    // Builds, once at class load, the AO corner-neighbour offsets for every face/vertex from the
    // face plane basis. Pure setup — keeps the meshing hot loop a plain table read with no math.
    private static int[][][] buildAoOffsets() {
        int[][][] table = new int[FACE_OFFSETS.length][VERTICES_PER_FACE][AO_INTS_PER_VERTEX];
        for (int face = 0; face < FACE_OFFSETS.length; face++)
            for (int v = 0; v < VERTICES_PER_FACE; v++)
                table[face][v] = cornerOffsets(face, v);
        return table;
    }

    private static int[] cornerOffsets(int face, int v) {
        int[] n = FACE_NORMAL[face], u = FACE_AXIS_U[face], w = FACE_AXIS_V[face];
        int su = axisSign(face, v, u);
        int sv = axisSign(face, v, w);
        int[] side1  = add(n, scale(u, su));
        int[] side2  = add(n, scale(w, sv));
        int[] corner = add(side1, scale(w, sv));
        return new int[]{ side1[0], side1[1], side1[2], side2[0], side2[1], side2[2],
                          corner[0], corner[1], corner[2] };
    }

    // +1 if the vertex sits at the high end of the axis (coord 1, reaches toward +axis), else -1.
    private static int axisSign(int face, int v, int[] axis) {
        float[] off = FACE_OFFSETS[face];
        int k = axis[0] != 0 ? 0 : (axis[1] != 0 ? 1 : 2);
        return off[v * 3 + k] == 1f ? 1 : -1;
    }

    private static int[] scale(int[] a, int s) { return new int[]{ a[0] * s, a[1] * s, a[2] * s }; }
    private static int[] add(int[] a, int[] b) { return new int[]{ a[0] + b[0], a[1] + b[1], a[2] + b[2] }; }

    // Combined per-vertex brightness = face light (STEP-21) * AO factor, written into the SAME light
    // attribute. Out-of-bounds neighbours are not solid (no occlusion), matching isAirOrOutOfBounds.
    private static void computeVertexLight(VoxelChunkData data, int bx, int by, int bz, int face,
                                           float faceLight, MeshBuilder builder) {
        int[] levels = builder.aoLevels;
        for (int v = 0; v < VERTICES_PER_FACE; v++) {
            int[] o = FACE_VERTEX_AO_OFFSETS[face][v];
            boolean side1  = isSolid(data, bx + o[0], by + o[1], bz + o[2]);
            boolean side2  = isSolid(data, bx + o[3], by + o[4], bz + o[5]);
            boolean corner = isSolid(data, bx + o[6], by + o[7], bz + o[8]);
            levels[v] = AmbientOcclusion.cornerLevel(side1, side2, corner);
            builder.vertexLight[v] = faceLight * AmbientOcclusion.factor(levels[v]);
        }
        builder.flipQuad = AmbientOcclusion.shouldFlip(levels[0], levels[1], levels[2], levels[3]);
    }

    private static boolean isSolid(VoxelChunkData data, int x, int y, int z) {
        return !isAirOrOutOfBounds(data, x, y, z) && BlockType.byId(data.get(x, y, z)).solid();
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

        // Reused per-face scratch (zero alloc in the loop): combined light*AO brightness per vertex,
        // raw AO levels (for the flip decision), and whether the diagonal must be rotated.
        final float[] vertexLight = new float[VERTICES_PER_FACE];
        final int[]   aoLevels    = new int[VERTICES_PER_FACE];
        boolean flipQuad = false;

        // topY replaces the y=1 vertices' height so water can sit below the cell ceiling; solid
        // blocks pass topY = 1.0 and are unaffected. Each vertex carries its own light*AO brightness
        // from the vertexLight scratch, giving smooth-shaded occluded corners.
        void addFace(int bx, int by, int bz, int face, float[] uv, float[] tint, float topY) {
            ensureCapacity();
            float[] off = FACE_OFFSETS[face];
            int baseVertex = vOffset / FLOATS_PER_VERTEX;
            for (int v = 0; v < VERTICES_PER_FACE; v++) {
                float yOff = off[v * 3 + 1];
                vertices[vOffset++] = bx + off[v * 3];
                vertices[vOffset++] = by + (yOff == TOP_Y_OFFSET ? topY : yOff);
                vertices[vOffset++] = bz + off[v * 3 + 2];
                vertices[vOffset++] = uv[FACE_UV_CORNER[v * 2]];
                vertices[vOffset++] = uv[FACE_UV_CORNER[v * 2 + 1]];
                vertices[vOffset++] = tint[0];
                vertices[vOffset++] = tint[1];
                vertices[vOffset++] = tint[2];
                vertices[vOffset++] = vertexLight[v];
            }
            emitIndices(baseVertex);
        }

        // Two triangles per quad. The default diagonal joins v0–v2; when AO is imbalanced across that
        // diagonal we rotate it to v1–v3 so the shade gradient stays symmetric (no anisotropy seam).
        // Both orderings keep CCW winding, so face culling is unaffected.
        private void emitIndices(int baseVertex) {
            if (flipQuad) {
                indices[iOffset++] = baseVertex + 1;
                indices[iOffset++] = baseVertex + 2;
                indices[iOffset++] = baseVertex + 3;
                indices[iOffset++] = baseVertex + 3;
                indices[iOffset++] = baseVertex;
                indices[iOffset++] = baseVertex + 1;
            } else {
                indices[iOffset++] = baseVertex;
                indices[iOffset++] = baseVertex + 1;
                indices[iOffset++] = baseVertex + 2;
                indices[iOffset++] = baseVertex + 2;
                indices[iOffset++] = baseVertex + 3;
                indices[iOffset++] = baseVertex;
            }
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
