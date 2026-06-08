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
    void twoSolidBlocksProduceTwelveFaces() {
        VoxelChunkData data = VoxelChunkData.empty();
        data.set(0, 0, 0, WorldConstants.BLOCK_STONE);
        data.set(1, 0, 0, WorldConstants.BLOCK_DIRT);

        ChunkMeshingSystem.Geometry geo = ChunkMeshingSystem.buildGeometry(data);

        int expectedFaces = 2 * FACES_PER_BLOCK;
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
