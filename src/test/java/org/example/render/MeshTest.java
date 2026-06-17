package org.example.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MeshTest {

    private static final int FLOATS_PER_VERTEX = 8;   // pos(3) + uv(2) + tint(3)
    private static final int TINT_OFFSET       = 5;   // tint rgb follows pos + uv
    private static final int UV_OFFSET         = 3;   // uv follows pos
    private static final int VERTICES_PER_FACE = 4;
    private static final int FACES             = 6;
    private static final int TRIANGLES_PER_FACE = 2;
    private static final int INDICES_PER_TRIANGLE = 3;

    @Test
    void vertexArrayHasCorrectSize() {
        float[] v = Mesh.buildCubeVertices();
        assertEquals(FACES * VERTICES_PER_FACE * FLOATS_PER_VERTEX, v.length);
    }

    @Test
    void indexArrayHasCorrectSize() {
        int[] idx = Mesh.buildCubeIndices();
        assertEquals(FACES * TRIANGLES_PER_FACE * INDICES_PER_TRIANGLE, idx.length);
    }

    @Test
    void eachFaceUsesItsOwnVertexRange() {
        int[] idx = Mesh.buildCubeIndices();
        for (int f = 0; f < FACES; f++) {
            int base  = f * VERTICES_PER_FACE;
            int start = f * 6;
            // Every index in this face must fall within [base, base+3]
            for (int j = start; j < start + 6; j++) {
                assertTrue(idx[j] >= base && idx[j] <= base + 3,
                        "Face " + f + ": index " + idx[j] + " out of range [" + base + ", " + (base + 3) + "]");
            }
        }
    }

    @Test
    void eachFaceFormsTwoTriangles() {
        int[] idx = Mesh.buildCubeIndices();
        for (int f = 0; f < FACES; f++) {
            int i = f * 6;
            // Pattern: (b, b+1, b+2), (b+2, b+3, b) — counter-clockwise quad split
            int b = f * VERTICES_PER_FACE;
            assertEquals(b,     idx[i]);
            assertEquals(b + 1, idx[i + 1]);
            assertEquals(b + 2, idx[i + 2]);
            assertEquals(b + 2, idx[i + 3]);
            assertEquals(b + 3, idx[i + 4]);
            assertEquals(b,     idx[i + 5]);
        }
    }

    @Test
    void vertexTintComponentsAreInUnitRange() {
        float[] v = Mesh.buildCubeVertices();
        for (int i = TINT_OFFSET; i < v.length; i += FLOATS_PER_VERTEX) {
            float r = v[i], g = v[i + 1], b = v[i + 2];
            assertTrue(r >= 0f && r <= 1f, "Red out of [0,1]: " + r);
            assertTrue(g >= 0f && g <= 1f, "Green out of [0,1]: " + g);
            assertTrue(b >= 0f && b <= 1f, "Blue out of [0,1]: " + b);
        }
    }

    @Test
    void vertexUvComponentsAreInUnitRange() {
        float[] v = Mesh.buildCubeVertices();
        for (int i = UV_OFFSET; i < v.length; i += FLOATS_PER_VERTEX) {
            float u = v[i], uv = v[i + 1];
            assertTrue(u >= 0f && u <= 1f, "U out of [0,1]: " + u);
            assertTrue(uv >= 0f && uv <= 1f, "V out of [0,1]: " + uv);
        }
    }

    @Test
    void vertexPositionComponentsAreHalfUnit() {
        float[] v = Mesh.buildCubeVertices();
        for (int i = 0; i < v.length; i += FLOATS_PER_VERTEX) {
            float x = v[i], y = v[i + 1], z = v[i + 2];
            assertEquals(0.5f, Math.abs(x), 1e-6f, "x must be ±0.5");
            assertEquals(0.5f, Math.abs(y), 1e-6f, "y must be ±0.5");
            assertEquals(0.5f, Math.abs(z), 1e-6f, "z must be ±0.5");
        }
    }

    @Test
    void buildCubeVerticesSpansFullUnitCube() {
        float[] v = Mesh.buildCubeVertices();
        float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
        float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        float minZ = Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
        for (int i = 0; i < v.length; i += FLOATS_PER_VERTEX) {
            minX = Math.min(minX, v[i]);     maxX = Math.max(maxX, v[i]);
            minY = Math.min(minY, v[i + 1]); maxY = Math.max(maxY, v[i + 1]);
            minZ = Math.min(minZ, v[i + 2]); maxZ = Math.max(maxZ, v[i + 2]);
        }
        assertEquals(-0.5f, minX, 1e-6f); assertEquals(0.5f, maxX, 1e-6f);
        assertEquals(-0.5f, minY, 1e-6f); assertEquals(0.5f, maxY, 1e-6f);
        assertEquals(-0.5f, minZ, 1e-6f); assertEquals(0.5f, maxZ, 1e-6f);
    }

    @Test
    void eachFaceHasFourDistinctVertexPositions() {
        float[] v = Mesh.buildCubeVertices();
        for (int f = 0; f < FACES; f++) {
            int base = f * VERTICES_PER_FACE * FLOATS_PER_VERTEX;
            float[][] pos = new float[4][3];
            for (int vi = 0; vi < 4; vi++) {
                pos[vi][0] = v[base + vi * FLOATS_PER_VERTEX];
                pos[vi][1] = v[base + vi * FLOATS_PER_VERTEX + 1];
                pos[vi][2] = v[base + vi * FLOATS_PER_VERTEX + 2];
            }
            for (int a = 0; a < 4; a++) {
                for (int b = a + 1; b < 4; b++) {
                    boolean same = pos[a][0] == pos[b][0] && pos[a][1] == pos[b][1] && pos[a][2] == pos[b][2];
                    assertFalse(same, "Face " + f + ": vertices " + a + " and " + b + " share same position");
                }
            }
        }
    }
}
