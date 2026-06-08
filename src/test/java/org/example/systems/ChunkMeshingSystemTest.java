package org.example.systems;

import org.example.components.VoxelChunkData;
import org.example.world.WorldConstants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChunkMeshingSystemTest {

    private static final int FLOATS_PER_VERTEX  = 6;
    private static final int VERTICES_PER_FACE  = 4;
    private static final int INDICES_PER_FACE   = 6;
    private static final int FACES_PER_BLOCK    = 6;

    @Test
    void emptyChunkProducesNoGeometry() {
        ChunkMeshingSystem.Geometry geo = ChunkMeshingSystem.buildGeometry(VoxelChunkData.empty());
        assertEquals(0, geo.vertices().length);
        assertEquals(0, geo.indices().length);
    }

    @Test
    void singleSolidBlockProducesSixFaces() {
        VoxelChunkData data = VoxelChunkData.empty();
        data.set(0, 0, 0, WorldConstants.BLOCK_STONE);

        ChunkMeshingSystem.Geometry geo = ChunkMeshingSystem.buildGeometry(data);

        assertEquals(FACES_PER_BLOCK * VERTICES_PER_FACE * FLOATS_PER_VERTEX, geo.vertices().length);
        assertEquals(FACES_PER_BLOCK * INDICES_PER_FACE, geo.indices().length);
    }

    @Test
    void twoAdjacentBlocksSuppressSharedFaces() {
        VoxelChunkData data = VoxelChunkData.empty();
        data.set(0, 0, 0, WorldConstants.BLOCK_STONE);
        data.set(1, 0, 0, WorldConstants.BLOCK_DIRT);

        ChunkMeshingSystem.Geometry geo = ChunkMeshingSystem.buildGeometry(data);

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

        ChunkMeshingSystem.Geometry geo = ChunkMeshingSystem.buildGeometry(data);

        // The centre block contributes 0 faces; each neighbour exposes 5 outer faces
        int centreFaceCount = 0;
        int neighbourFaces = 6 * 5; // 6 neighbours × 5 exposed faces each
        int expectedFaces = centreFaceCount + neighbourFaces;
        assertEquals(expectedFaces * VERTICES_PER_FACE * FLOATS_PER_VERTEX, geo.vertices().length);
        assertEquals(expectedFaces * INDICES_PER_FACE, geo.indices().length);
    }

    @Test
    void vertexColorsAreInUnitRange() {
        VoxelChunkData data = VoxelChunkData.empty();
        data.set(0, 0, 0, WorldConstants.BLOCK_GRASS);
        data.set(1, 0, 0, WorldConstants.BLOCK_DIRT);
        data.set(2, 0, 0, WorldConstants.BLOCK_STONE);

        float[] vertices = ChunkMeshingSystem.buildGeometry(data).vertices();
        for (int i = 3; i < vertices.length; i += FLOATS_PER_VERTEX) {
            float r = vertices[i], g = vertices[i + 1], b = vertices[i + 2];
            assertTrue(r >= 0f && r <= 1f, "Red out of [0,1]: " + r);
            assertTrue(g >= 0f && g <= 1f, "Green out of [0,1]: " + g);
            assertTrue(b >= 0f && b <= 1f, "Blue out of [0,1]: " + b);
        }
    }

    @Test
    void blockAtChunkBorderExposesAllSixFaces() {
        VoxelChunkData data = VoxelChunkData.empty();
        int maxIdx = WorldConstants.CHUNK_SIZE - 1;
        data.set(maxIdx, 0, 0, WorldConstants.BLOCK_STONE);

        ChunkMeshingSystem.Geometry geo = ChunkMeshingSystem.buildGeometry(data);

        // Out-of-bounds neighbours count as air → all 6 faces visible
        assertEquals(FACES_PER_BLOCK * VERTICES_PER_FACE * FLOATS_PER_VERTEX, geo.vertices().length);
        assertEquals(FACES_PER_BLOCK * INDICES_PER_FACE, geo.indices().length);
    }

    @Test
    void grassTopFaceIsGreenAndSideFaceIsBrown() {
        VoxelChunkData data = VoxelChunkData.empty();
        data.set(0, 0, 0, WorldConstants.BLOCK_GRASS);

        float[] v = ChunkMeshingSystem.buildGeometry(data).vertices();

        // Face order follows FACE_OFFSETS: 0=Front, 1=Back, 2=Top, 3=Bottom, 4=Right, 5=Left
        int topFaceStart  = 2 * VERTICES_PER_FACE * FLOATS_PER_VERTEX; // face 2
        int sideFaceStart = 0; // face 0 (Front)

        float topG  = v[topFaceStart  + 4];
        float sideG = v[sideFaceStart + 4];

        assertTrue(topG > sideG, "Grass top face should be greener than its side faces");
    }

    @Test
    void unknownBlockTypeProducesMagentaColor() {
        VoxelChunkData data = VoxelChunkData.empty();
        data.set(0, 0, 0, (byte) 99);

        float[] v = ChunkMeshingSystem.buildGeometry(data).vertices();

        assertEquals(1.0f, v[3], 1e-6f, "Expected R=1.0 for unknown block");
        assertEquals(0.0f, v[4], 1e-6f, "Expected G=0.0 for unknown block");
        assertEquals(1.0f, v[5], 1e-6f, "Expected B=1.0 for unknown block");
    }

    @Test
    void vertexPositionsMatchBlockCoordinates() {
        VoxelChunkData data = VoxelChunkData.empty();
        int bx = 5, by = 3, bz = 7;
        data.set(bx, by, bz, WorldConstants.BLOCK_STONE);

        float[] v = ChunkMeshingSystem.buildGeometry(data).vertices();

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

        ChunkMeshingSystem.Geometry geo = ChunkMeshingSystem.buildGeometry(data);
        int vertexCount = geo.vertices().length / FLOATS_PER_VERTEX;
        for (int idx : geo.indices()) {
            assertTrue(idx >= 0 && idx < vertexCount,
                    "Index " + idx + " out of vertex range [0, " + vertexCount + ")");
        }
    }
}
