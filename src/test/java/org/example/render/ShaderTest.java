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

    @Test
    void vertexShaderDeclaresModelViewProjectionUniforms() {
        String src = Shader.readResource("/shaders/basic.vert");
        assertTrue(src.contains("uModel"),      "Missing uModel uniform");
        assertTrue(src.contains("uView"),       "Missing uView uniform");
        assertTrue(src.contains("uProjection"), "Missing uProjection uniform");
    }

    @Test
    void vertexShaderDeclaresMeshAttributes() {
        String src = Shader.readResource("/shaders/basic.vert");
        assertTrue(src.contains("aPosition"), "Missing aPosition attribute");
        assertTrue(src.contains("aColor"),    "Missing aColor attribute");
    }

    @Test
    void vertexShaderPassesColorVaryingToFragment() {
        String src = Shader.readResource("/shaders/basic.vert");
        assertTrue(src.contains("vColor"), "Missing vColor varying output");
    }

    @Test
    void fragmentShaderWritesToFragColorOutput() {
        String src = Shader.readResource("/shaders/basic.frag");
        assertTrue(src.contains("fragColor"), "Missing fragColor output");
    }
}
