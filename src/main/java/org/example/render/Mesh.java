package org.example.render;

import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL30.*;

public final class Mesh implements AutoCloseable {

    private static final int STRIDE = 6 * Float.BYTES;

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

        glVertexAttribPointer(0, 3, GL_FLOAT, false, STRIDE, 0L);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, STRIDE, 3L * Float.BYTES);
        glEnableVertexAttribArray(1);

        glBindVertexArray(0);
    }

    public static Mesh createTestCube() {
        return new Mesh(buildCubeVertices(), buildCubeIndices());
    }

    // Package-private: pure geometry data, no OpenGL — testable without a GL context
    static float[] buildCubeVertices() {
        return new float[] {
            // Front (z+) — red
            -0.5f, -0.5f,  0.5f,  1.0f, 0.2f, 0.2f,
             0.5f, -0.5f,  0.5f,  1.0f, 0.2f, 0.2f,
             0.5f,  0.5f,  0.5f,  1.0f, 0.2f, 0.2f,
            -0.5f,  0.5f,  0.5f,  1.0f, 0.2f, 0.2f,
            // Back (z-) — cyan
             0.5f, -0.5f, -0.5f,  0.2f, 1.0f, 1.0f,
            -0.5f, -0.5f, -0.5f,  0.2f, 1.0f, 1.0f,
            -0.5f,  0.5f, -0.5f,  0.2f, 1.0f, 1.0f,
             0.5f,  0.5f, -0.5f,  0.2f, 1.0f, 1.0f,
            // Top (y+) — green
            -0.5f,  0.5f,  0.5f,  0.2f, 1.0f, 0.2f,
             0.5f,  0.5f,  0.5f,  0.2f, 1.0f, 0.2f,
             0.5f,  0.5f, -0.5f,  0.2f, 1.0f, 0.2f,
            -0.5f,  0.5f, -0.5f,  0.2f, 1.0f, 0.2f,
            // Bottom (y-) — magenta
            -0.5f, -0.5f, -0.5f,  1.0f, 0.2f, 1.0f,
             0.5f, -0.5f, -0.5f,  1.0f, 0.2f, 1.0f,
             0.5f, -0.5f,  0.5f,  1.0f, 0.2f, 1.0f,
            -0.5f, -0.5f,  0.5f,  1.0f, 0.2f, 1.0f,
            // Right (x+) — blue
             0.5f, -0.5f,  0.5f,  0.2f, 0.2f, 1.0f,
             0.5f, -0.5f, -0.5f,  0.2f, 0.2f, 1.0f,
             0.5f,  0.5f, -0.5f,  0.2f, 0.2f, 1.0f,
             0.5f,  0.5f,  0.5f,  0.2f, 0.2f, 1.0f,
            // Left (x-) — yellow
            -0.5f, -0.5f, -0.5f,  1.0f, 1.0f, 0.2f,
            -0.5f, -0.5f,  0.5f,  1.0f, 1.0f, 0.2f,
            -0.5f,  0.5f,  0.5f,  1.0f, 1.0f, 0.2f,
            -0.5f,  0.5f, -0.5f,  1.0f, 1.0f, 0.2f,
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
