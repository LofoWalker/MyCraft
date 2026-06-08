package org.example.render;

import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import static org.lwjgl.opengl.GL20.*;

public final class Shader implements AutoCloseable {

    private final int programId;

    public Shader(String vertexSrc, String fragmentSrc) {
        int vert = compileShader(GL_VERTEX_SHADER, vertexSrc);
        int frag = compileShader(GL_FRAGMENT_SHADER, fragmentSrc);
        programId = linkProgram(vert, frag);
        glDeleteShader(vert);
        glDeleteShader(frag);
    }

    public static Shader fromResources(String vertexPath, String fragmentPath) {
        return new Shader(readResource(vertexPath), readResource(fragmentPath));
    }

    // Package-private: pure I/O, no OpenGL — testable without a GL context
    static String readResource(String path) {
        try (InputStream is = Shader.class.getResourceAsStream(path)) {
            if (is == null) throw new RuntimeException("Shader resource not found: " + path);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read shader: " + path, e);
        }
    }

    private static int compileShader(int type, String source) {
        int id = glCreateShader(type);
        glShaderSource(id, source);
        glCompileShader(id);
        if (glGetShaderi(id, GL_COMPILE_STATUS) == GL_FALSE) {
            String log = glGetShaderInfoLog(id);
            glDeleteShader(id);
            throw new RuntimeException("Shader compilation failed:\n" + log);
        }
        return id;
    }

    private static int linkProgram(int vert, int frag) {
        int id = glCreateProgram();
        glAttachShader(id, vert);
        glAttachShader(id, frag);
        glLinkProgram(id);
        if (glGetProgrami(id, GL_LINK_STATUS) == GL_FALSE) {
            String log = glGetProgramInfoLog(id);
            glDeleteProgram(id);
            throw new RuntimeException("Shader program linking failed:\n" + log);
        }
        return id;
    }

    public void bind() {
        glUseProgram(programId);
    }

    public void unbind() {
        glUseProgram(0);
    }

    public void setUniformMatrix4f(String name, Matrix4f matrix) {
        int loc = glGetUniformLocation(programId, name);
        if (loc == -1) return;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            glUniformMatrix4fv(loc, false, matrix.get(stack.mallocFloat(16)));
        }
    }

    @Override
    public void close() {
        glDeleteProgram(programId);
    }
}
