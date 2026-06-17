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
        assertTrue(src.contains("aUV"),       "Missing aUV attribute");
        assertTrue(src.contains("aTint"),     "Missing aTint attribute");
    }

    @Test
    void vertexShaderPassesUvAndTintVaryingsToFragment() {
        String src = Shader.readResource("/shaders/basic.vert");
        assertTrue(src.contains("vUV"),   "Missing vUV varying output");
        assertTrue(src.contains("vTint"), "Missing vTint varying output");
    }

    @Test
    void fragmentShaderSamplesAtlasUniform() {
        String src = Shader.readResource("/shaders/basic.frag");
        assertTrue(src.contains("sampler2D"), "Missing sampler2D atlas uniform");
        assertTrue(src.contains("uAtlas"),    "Missing uAtlas uniform");
        assertTrue(src.contains("texture("),  "Fragment shader must sample the atlas texture");
    }

    @Test
    void fragmentShaderWritesToFragColorOutput() {
        String src = Shader.readResource("/shaders/basic.frag");
        assertTrue(src.contains("fragColor"), "Missing fragColor output");
    }
}
