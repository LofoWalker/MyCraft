package org.example.render;

import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL30.*;

public final class Mesh implements AutoCloseable {

    // pos(3) + uv(2) + tint rgb(3) + light(1). The tint multiplies the sampled atlas texel in the
    // shader; light is a normalized 0..1 brightness (STEP-21) the fragment shader maps to a dim..full
    // factor. Position-only consumers (highlight/item/water shaders) ignore the extra attributes.
    static final int FLOATS_PER_VERTEX = 9;
    private static final int STRIDE = FLOATS_PER_VERTEX * Float.BYTES;

    private static final int POSITION_OFFSET = 0;
    private static final int UV_OFFSET       = 3;
    private static final int TINT_OFFSET     = 5;
    private static final int LIGHT_OFFSET    = 8;

    private final int vao;
    private final int vbo;
    private final int ebo;
    private final int indexCount;

    private Mesh(float[] vertices, int[] indices) {
        indexCount = indices.length;

        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        ebo = glGenBuffers();

        glBindVertexArray(vao);

        FloatBuffer vBuf = MemoryUtil.memAllocFloat(vertices.length);
        try {
            vBuf.put(vertices).flip();
            glBindBuffer(GL_ARRAY_BUFFER, vbo);
            glBufferData(GL_ARRAY_BUFFER, vBuf, GL_STATIC_DRAW);
        } finally {
            MemoryUtil.memFree(vBuf);
        }

        IntBuffer iBuf = MemoryUtil.memAllocInt(indices.length);
        try {
            iBuf.put(indices).flip();
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, iBuf, GL_STATIC_DRAW);
        } finally {
            MemoryUtil.memFree(iBuf);
        }

        glVertexAttribPointer(0, 3, GL_FLOAT, false, STRIDE, POSITION_OFFSET * (long) Float.BYTES);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, STRIDE, UV_OFFSET * (long) Float.BYTES);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(2, 3, GL_FLOAT, false, STRIDE, TINT_OFFSET * (long) Float.BYTES);
        glEnableVertexAttribArray(2);
        glVertexAttribPointer(3, 1, GL_FLOAT, false, STRIDE, LIGHT_OFFSET * (long) Float.BYTES);
        glEnableVertexAttribArray(3);

        glBindVertexArray(0);
    }

    public static Mesh createTestCube() {
        return new Mesh(buildCubeVertices(), buildCubeIndices());
    }

    public static Mesh create(float[] vertices, int[] indices) {
        return new Mesh(vertices, indices);
    }

    // Package-private: pure geometry data, no OpenGL — testable without a GL context.
    // Layout per vertex: pos(3), uv(2), tint rgb(3), light(1). The cube is drawn by the
    // highlight/item shaders, which read only position; the UVs span a full tile and the tint keeps
    // the old per-face debug colours so anything that does sample stays sensible. Light is pinned to
    // FULL_LIGHT so the stride matches the chunk format while these shaders simply ignore it.
    private static final float FULL_LIGHT = 1.0f;

    static float[] buildCubeVertices() {
        return new float[] {
            // Front (z+) — red
            -0.5f, -0.5f,  0.5f,  0.0f, 0.0f,  1.0f, 0.2f, 0.2f,  FULL_LIGHT,
             0.5f, -0.5f,  0.5f,  1.0f, 0.0f,  1.0f, 0.2f, 0.2f,  FULL_LIGHT,
             0.5f,  0.5f,  0.5f,  1.0f, 1.0f,  1.0f, 0.2f, 0.2f,  FULL_LIGHT,
            -0.5f,  0.5f,  0.5f,  0.0f, 1.0f,  1.0f, 0.2f, 0.2f,  FULL_LIGHT,
            // Back (z-) — cyan
             0.5f, -0.5f, -0.5f,  0.0f, 0.0f,  0.2f, 1.0f, 1.0f,  FULL_LIGHT,
            -0.5f, -0.5f, -0.5f,  1.0f, 0.0f,  0.2f, 1.0f, 1.0f,  FULL_LIGHT,
            -0.5f,  0.5f, -0.5f,  1.0f, 1.0f,  0.2f, 1.0f, 1.0f,  FULL_LIGHT,
             0.5f,  0.5f, -0.5f,  0.0f, 1.0f,  0.2f, 1.0f, 1.0f,  FULL_LIGHT,
            // Top (y+) — green
            -0.5f,  0.5f,  0.5f,  0.0f, 0.0f,  0.2f, 1.0f, 0.2f,  FULL_LIGHT,
             0.5f,  0.5f,  0.5f,  1.0f, 0.0f,  0.2f, 1.0f, 0.2f,  FULL_LIGHT,
             0.5f,  0.5f, -0.5f,  1.0f, 1.0f,  0.2f, 1.0f, 0.2f,  FULL_LIGHT,
            -0.5f,  0.5f, -0.5f,  0.0f, 1.0f,  0.2f, 1.0f, 0.2f,  FULL_LIGHT,
            // Bottom (y-) — magenta
            -0.5f, -0.5f, -0.5f,  0.0f, 0.0f,  1.0f, 0.2f, 1.0f,  FULL_LIGHT,
             0.5f, -0.5f, -0.5f,  1.0f, 0.0f,  1.0f, 0.2f, 1.0f,  FULL_LIGHT,
             0.5f, -0.5f,  0.5f,  1.0f, 1.0f,  1.0f, 0.2f, 1.0f,  FULL_LIGHT,
            -0.5f, -0.5f,  0.5f,  0.0f, 1.0f,  1.0f, 0.2f, 1.0f,  FULL_LIGHT,
            // Right (x+) — blue
             0.5f, -0.5f,  0.5f,  0.0f, 0.0f,  0.2f, 0.2f, 1.0f,  FULL_LIGHT,
             0.5f, -0.5f, -0.5f,  1.0f, 0.0f,  0.2f, 0.2f, 1.0f,  FULL_LIGHT,
             0.5f,  0.5f, -0.5f,  1.0f, 1.0f,  0.2f, 0.2f, 1.0f,  FULL_LIGHT,
             0.5f,  0.5f,  0.5f,  0.0f, 1.0f,  0.2f, 0.2f, 1.0f,  FULL_LIGHT,
            // Left (x-) — yellow
            -0.5f, -0.5f, -0.5f,  0.0f, 0.0f,  1.0f, 1.0f, 0.2f,  FULL_LIGHT,
            -0.5f, -0.5f,  0.5f,  1.0f, 0.0f,  1.0f, 1.0f, 0.2f,  FULL_LIGHT,
            -0.5f,  0.5f,  0.5f,  1.0f, 1.0f,  1.0f, 1.0f, 0.2f,  FULL_LIGHT,
            -0.5f,  0.5f, -0.5f,  0.0f, 1.0f,  1.0f, 1.0f, 0.2f,  FULL_LIGHT,
        };
    }

    static int[] buildCubeIndices() {
        int[] indices = new int[36];
        for (int f = 0; f < 6; f++) {
            int b = f * 4;
            int i = f * 6;
            indices[i]     = b;
            indices[i + 1] = b + 1;
            indices[i + 2] = b + 2;
            indices[i + 3] = b + 2;
            indices[i + 4] = b + 3;
            indices[i + 5] = b;
        }
        return indices;
    }

    public void draw() {
        glBindVertexArray(vao);
        glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT, 0L);
        glBindVertexArray(0);
    }

    @Override
    public void close() {
        glDeleteVertexArrays(vao);
        glDeleteBuffers(vbo);
        glDeleteBuffers(ebo);
    }
}
