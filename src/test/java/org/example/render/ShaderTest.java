package org.example.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ShaderTest {

    @Test
    void readsVertexShaderFromClasspath() {
        String src = Shader.readResource("/shaders/basic.vert");
        assertNotNull(src);
        assertFalse(src.isBlank());
    }

    @Test
    void readsFragmentShaderFromClasspath() {
        String src = Shader.readResource("/shaders/basic.frag");
        assertNotNull(src);
        assertFalse(src.isBlank());
    }

    @Test
    void vertexShaderContainsGlslVersion() {
        String src = Shader.readResource("/shaders/basic.vert");
        assertTrue(src.contains("#version"), "Missing #version directive");
    }

    @Test
    void fragmentShaderContainsGlslVersion() {
        String src = Shader.readResource("/shaders/basic.frag");
        assertTrue(src.contains("#version"), "Missing #version directive");
    }

    @Test
    void missingResourceThrowsRuntimeException() {
        assertThrows(RuntimeException.class,
                () -> Shader.readResource("/shaders/nonexistent.vert"));
    }
}
