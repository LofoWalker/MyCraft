package org.example.systems;

import org.example.components.VoxelChunkData;
import org.example.render.TextureAtlas;
import org.example.world.BlockType;
import org.example.world.LightEngine;
import org.example.world.WorldConstants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChunkMeshingSystemTest {

    private static final int FLOATS_PER_VERTEX  = 9;   // pos(3) + uv(2) + tint(3) + light(1)
    private static final int UV_OFFSET          = 3;
    private static final int TINT_OFFSET        = 5;
    private static final int LIGHT_OFFSET       = 8;
    private static final int VERTICES_PER_FACE  = 4;
    private static final int INDICES_PER_FACE   = 6;
    private static final int FACES_PER_BLOCK    = 6;

    // Face order follows FACE_OFFSETS: 0=Front, 1=Back, 2=Top, 3=Bottom, 4=Right, 5=Left.
    private static final int FACE_FRONT = 0;
    private static final int FACE_TOP   = 2;

    @Test
    void emptyChunkProducesNoGeometry() {
        ChunkMeshingSystem.Geometry geo = ChunkMeshingSystem.buildGeometry(VoxelChunkData.empty()).opaque();
        assertEquals(0, geo.vertices().length);
        assertEquals(0, geo.indices().length);
    }

    @Test
    void singleSolidBlockProducesSixFaces() {
        VoxelChunkData data = VoxelChunkData.empty();
        data.set(0, 0, 0, WorldConstants.BLOCK_STONE);

        ChunkMeshingSystem.Geometry geo = ChunkMeshingSystem.buildGeometry(data).opaque();

        assertEquals(FACES_PER_BLOCK * VERTICES_PER_FACE * FLOATS_PER_VERTEX, geo.vertices().length);
        assertEquals(FACES_PER_BLOCK * INDICES_PER_FACE, geo.indices().length);
    }

    @Test
    void twoAdjacentBlocksSuppressSharedFaces() {
        VoxelChunkData data = VoxelChunkData.empty();
        data.set(0, 0, 0, WorldConstants.BLOCK_STONE);
        data.set(1, 0, 0, WorldConstants.BLOCK_DIRT);

        ChunkMeshingSystem.Geometry geo = ChunkMeshingSystem.buildGeometry(data).opaque();

        // 2 blocks × 6 faces − 2 shared internal faces = 10 visible faces
        int expectedFaces = 2 * FACES_PER_BLOCK - 2;
        assertEquals(expectedFaces * VERTICES_PER_FACE * FLOATS_PER_VERTEX, geo.vertices().length);
        assertEquals(expectedFaces * INDICES_PER_FACE, geo.indices().length);
    }

    @Test
    void fullySurroundedBlockProducesNoFaces() {
        VoxelChunkData data = VoxelChunkData.empty();
        // Centre block at (1,1,1) surrounded on all 6 sides
        data.set(1, 1, 1, WorldConstants.BLOCK_STONE);
        data.set(2, 1, 1, WorldConstants.BLOCK_STONE); // x+
        data.set(0, 1, 1, WorldConstants.BLOCK_STONE); // x-
        data.set(1, 2, 1, WorldConstants.BLOCK_STONE); // y+
        data.set(1, 0, 1, WorldConstants.BLOCK_STONE); // y-
        data.set(1, 1, 2, WorldConstants.BLOCK_STONE); // z+
        data.set(1, 1, 0, WorldConstants.BLOCK_STONE); // z-

        ChunkMeshingSystem.Geometry geo = ChunkMeshingSystem.buildGeometry(data).opaque();

        // The centre block contributes 0 faces; each neighbour exposes 5 outer faces
        int centreFaceCount = 0;
        int neighbourFaces = 6 * 5; // 6 neighbours × 5 exposed faces each
        int expectedFaces = centreFaceCount + neighbourFaces;
        assertEquals(expectedFaces * VERTICES_PER_FACE * FLOATS_PER_VERTEX, geo.vertices().length);
        assertEquals(expectedFaces * INDICES_PER_FACE, geo.indices().length);
    }

    @Test
    void vertexTintComponentsAreInUnitRange() {
        VoxelChunkData data = VoxelChunkData.empty();
        data.set(0, 0, 0, WorldConstants.BLOCK_GRASS);
        data.set(1, 0, 0, WorldConstants.BLOCK_DIRT);
        data.set(2, 0, 0, WorldConstants.BLOCK_STONE);

        float[] vertices = ChunkMeshingSystem.buildGeometry(data).opaque().vertices();
        for (int i = TINT_OFFSET; i < vertices.length; i += FLOATS_PER_VERTEX) {
            float r = vertices[i], g = vertices[i + 1], b = vertices[i + 2];
            assertTrue(r >= 0f && r <= 1f, "Red out of [0,1]: " + r);
            assertTrue(g >= 0f && g <= 1f, "Green out of [0,1]: " + g);
            assertTrue(b >= 0f && b <= 1f, "Blue out of [0,1]: " + b);
        }
    }

    @Test
    void vertexUvComponentsAreInUnitRange() {
        VoxelChunkData data = VoxelChunkData.empty();
        data.set(0, 0, 0, WorldConstants.BLOCK_GRASS);

        float[] v = ChunkMeshingSystem.buildGeometry(data).opaque().vertices();
        for (int i = UV_OFFSET; i < v.length; i += FLOATS_PER_VERTEX) {
            float u = v[i], uv = v[i + 1];
            assertTrue(u >= 0f && u <= 1f, "U out of [0,1]: " + u);
            assertTrue(uv >= 0f && uv <= 1f, "V out of [0,1]: " + uv);
        }
    }

    @Test
    void blockAtChunkBorderExposesAllSixFaces() {
        VoxelChunkData data = VoxelChunkData.empty();
        int maxIdx = WorldConstants.CHUNK_SIZE_XZ - 1;
        data.set(maxIdx, 0, 0, WorldConstants.BLOCK_STONE);

        ChunkMeshingSystem.Geometry geo = ChunkMeshingSystem.buildGeometry(data).opaque();

        // Out-of-bounds neighbours count as air → all 6 faces visible
        assertEquals(FACES_PER_BLOCK * VERTICES_PER_FACE * FLOATS_PER_VERTEX, geo.vertices().length);
        assertEquals(FACES_PER_BLOCK * INDICES_PER_FACE, geo.indices().length);
    }

    @Test
    void grassTopFaceCarriesTopTileAndSideFaceCarriesSideTile() {
        VoxelChunkData data = VoxelChunkData.empty();
        data.set(0, 0, 0, WorldConstants.BLOCK_GRASS);

        float[] v = ChunkMeshingSystem.buildGeometry(data).opaque().vertices();

        assertArrayEquals(TextureAtlas.uvForTile(BlockType.GRASS.tileTop()),
                faceUvRect(v, FACE_TOP), 1e-6f, "Top face must carry grass top tile UVs");
        assertArrayEquals(TextureAtlas.uvForTile(BlockType.GRASS.tileSide()),
                faceUvRect(v, FACE_FRONT), 1e-6f, "Side (front) face must carry grass side tile UVs");
    }

    @Test
    void unknownBlockTypeTintsMagenta() {
        VoxelChunkData data = VoxelChunkData.empty();
        data.set(0, 0, 0, (byte) 99);

        float[] v = ChunkMeshingSystem.buildGeometry(data).opaque().vertices();

        assertEquals(1.0f, v[TINT_OFFSET],     1e-6f, "Expected tint R=1.0 for unknown block");
        assertEquals(0.0f, v[TINT_OFFSET + 1], 1e-6f, "Expected tint G=0.0 for unknown block");
        assertEquals(1.0f, v[TINT_OFFSET + 2], 1e-6f, "Expected tint B=1.0 for unknown block");
    }

    // {minU, minV, maxU, maxV} of the four vertices of the given face — comparable to a tile's
    // {u0,v0,u1,v1} rect regardless of which vertex got which corner.
    private static float[] faceUvRect(float[] vertices, int face) {
        int start = face * VERTICES_PER_FACE * FLOATS_PER_VERTEX;
        float minU = Float.MAX_VALUE, minV = Float.MAX_VALUE;
        float maxU = -Float.MAX_VALUE, maxV = -Float.MAX_VALUE;
        for (int vtx = 0; vtx < VERTICES_PER_FACE; vtx++) {
            float u = vertices[start + vtx * FLOATS_PER_VERTEX + UV_OFFSET];
            float w = vertices[start + vtx * FLOATS_PER_VERTEX + UV_OFFSET + 1];
            minU = Math.min(minU, u); maxU = Math.max(maxU, u);
            minV = Math.min(minV, w); maxV = Math.max(maxV, w);
        }
        return new float[]{ minU, minV, maxU, maxV };
    }

    @Test
    void vertexPositionsMatchBlockCoordinates() {
        VoxelChunkData data = VoxelChunkData.empty();
        int bx = 5, by = 3, bz = 7;
        data.set(bx, by, bz, WorldConstants.BLOCK_STONE);

        float[] v = ChunkMeshingSystem.buildGeometry(data).opaque().vertices();

        float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
        float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        float minZ = Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
        for (int i = 0; i < v.length; i += FLOATS_PER_VERTEX) {
            minX = Math.min(minX, v[i]);     maxX = Math.max(maxX, v[i]);
            minY = Math.min(minY, v[i + 1]); maxY = Math.max(maxY, v[i + 1]);
            minZ = Math.min(minZ, v[i + 2]); maxZ = Math.max(maxZ, v[i + 2]);
        }
        assertEquals(bx,     minX, 1e-6f, "Block min x");
        assertEquals(bx + 1, maxX, 1e-6f, "Block max x");
        assertEquals(by,     minY, 1e-6f, "Block min y");
        assertEquals(by + 1, maxY, 1e-6f, "Block max y");
        assertEquals(bz,     minZ, 1e-6f, "Block min z");
        assertEquals(bz + 1, maxZ, 1e-6f, "Block max z");
    }

    @Test
    void indexesReferenceValidVertices() {
        VoxelChunkData data = VoxelChunkData.empty();
        data.set(3, 2, 1, WorldConstants.BLOCK_STONE);

        ChunkMeshingSystem.Geometry geo = ChunkMeshingSystem.buildGeometry(data).opaque();
        int vertexCount = geo.vertices().length / FLOATS_PER_VERTEX;
        for (int idx : geo.indices()) {
            assertTrue(idx >= 0 && idx < vertexCount,
                    "Index " + idx + " out of vertex range [0, " + vertexCount + ")");
        }
    }

    // --- STEP-20: water geometry --------------------------------------------------------------

    @Test
    void waterCellGoesToWaterGeometryNotOpaque() {
        VoxelChunkData data = VoxelChunkData.empty();
        data.set(0, 0, 0, WorldConstants.BLOCK_WATER);

        ChunkMeshingSystem.ChunkGeometry geo = ChunkMeshingSystem.buildGeometry(data);

        assertEquals(0, geo.opaque().indices().length, "Water must not appear in opaque geometry");
        // Isolated water cell: 6 exposed faces (all neighbours are air/out-of-bounds).
        assertEquals(FACES_PER_BLOCK * INDICES_PER_FACE, geo.water().indices().length);
    }

    @Test
    void adjacentWaterCellsEmitNoFaceBetweenThem() {
        VoxelChunkData data = VoxelChunkData.empty();
        data.set(0, 0, 0, WorldConstants.BLOCK_WATER);
        data.set(1, 0, 0, WorldConstants.BLOCK_WATER);

        ChunkMeshingSystem.Geometry water = ChunkMeshingSystem.buildGeometry(data).water();

        // 2 water cells × 6 faces − 2 shared faces between them = 10 visible water faces.
        int expectedFaces = 2 * FACES_PER_BLOCK - 2;
        assertEquals(expectedFaces * INDICES_PER_FACE, water.indices().length);
    }

    @Test
    void waterTopFaceIsGeneratedUnderAir() {
        VoxelChunkData data = VoxelChunkData.empty();
        // Column of two water cells: only the lower cell's top is shared with water (culled),
        // the upper cell's top sits under air, so exactly one top face is exposed at its surface.
        data.set(0, 0, 0, WorldConstants.BLOCK_WATER);
        data.set(0, 1, 0, WorldConstants.BLOCK_WATER);

        ChunkMeshingSystem.Geometry water = ChunkMeshingSystem.buildGeometry(data).water();
        float maxY = -Float.MAX_VALUE;
        for (int i = 1; i < water.vertices().length; i += FLOATS_PER_VERTEX) {
            maxY = Math.max(maxY, water.vertices()[i]);
        }

        // The surface is lowered by WATER_SURFACE_DROP below the upper cell's ceiling (y=2).
        float expectedSurfaceY = 2f - WorldConstants.WATER_SURFACE_DROP;
        assertEquals(expectedSurfaceY, maxY, 1e-6f, "Water surface must be lowered under air");
    }

    // --- STEP-21: per-face vertex lighting ----------------------------------------------------

    @Test
    void exposedFaceCarriesNeighbourCellLight() {
        VoxelChunkData data = VoxelChunkData.empty();
        int bx = 4, by = 4, bz = 4;
        data.set(bx, by, bz, WorldConstants.BLOCK_STONE);

        // Hand-built light field: the air cell directly above the block carries skylight 9.
        byte[] light = new byte[WorldConstants.CHUNK_SIZE_XZ * WorldConstants.CHUNK_SIZE_XZ
                * WorldConstants.WORLD_HEIGHT];
        int expectedLevel = 9;
        light[cellIndex(bx, by + 1, bz)] = packSkylight(expectedLevel);

        float[] v = ChunkMeshingSystem.buildGeometry(data, light).opaque().vertices();

        float topFaceLight = v[FACE_TOP * VERTICES_PER_FACE * FLOATS_PER_VERTEX + LIGHT_OFFSET];
        assertEquals(expectedLevel / (float) WorldConstants.MAX_LIGHT_LEVEL, topFaceLight, 1e-6f,
                "Top face must carry the light level of the air cell above it");
    }

    @Test
    void exposedSkyFaceIsFullyLitViaLightEngine() {
        VoxelChunkData data = VoxelChunkData.empty();
        data.set(0, 0, 0, WorldConstants.BLOCK_STONE); // open-sky column → top neighbour skylight 15

        float[] v = ChunkMeshingSystem.buildGeometry(data,
                LightEngine.computeLight(data)).opaque().vertices();

        float topFaceLight = v[FACE_TOP * VERTICES_PER_FACE * FLOATS_PER_VERTEX + LIGHT_OFFSET];
        assertEquals(1.0f, topFaceLight, 1e-6f, "An open-sky top face must be fully lit");
    }

    private static int cellIndex(int x, int y, int z) {
        int sx = WorldConstants.CHUNK_SIZE_XZ;
        return x + z * sx + y * sx * sx;
    }

    private static byte packSkylight(int level) {
        return (byte) (level << 4);
    }

    @Test
    void waterSubmergedUnderAnotherWaterCellHasNoTopFace() {
        VoxelChunkData data = VoxelChunkData.empty();
        data.set(0, 0, 0, WorldConstants.BLOCK_WATER); // bottom
        data.set(0, 1, 0, WorldConstants.BLOCK_WATER); // covers bottom's top face

        ChunkMeshingSystem.Geometry water = ChunkMeshingSystem.buildGeometry(data).water();

        // 2 cells × 6 − 2 shared (between the stacked cells) = 10 faces.
        int expectedFaces = 2 * FACES_PER_BLOCK - 2;
        assertEquals(expectedFaces * INDICES_PER_FACE, water.indices().length);
    }
}
