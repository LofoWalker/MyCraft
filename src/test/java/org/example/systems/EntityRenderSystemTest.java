package org.example.systems;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the pure-math parts of EntityRenderSystem without any OpenGL context.
 * Only EntityRenderSystem.buildModel (the static overload) is exercised here.
 */
class EntityRenderSystemTest {

    private static final float EPSILON = 1e-4f;

    @Test
    void buildModelTranslatesOrigin() {
        Matrix4f m = new Matrix4f();
        EntityRenderSystem.buildModel(m, 3f, 7f, -2f, 0f, 1f, 1f, 1f);

        // Applying the model matrix to (0,0,0,1) should yield the translation.
        Vector3f result = m.transformPosition(new Vector3f(0f, 0f, 0f));
        assertEquals(3f,  result.x(), EPSILON);
        assertEquals(7f,  result.y(), EPSILON);
        assertEquals(-2f, result.z(), EPSILON);
    }

    @Test
    void buildModelScalesUnit() {
        Matrix4f m = new Matrix4f();
        EntityRenderSystem.buildModel(m, 0f, 0f, 0f, 0f, 2f, 3f, 4f);

        // A point at (0.5, 0.5, 0.5) should map to (1, 1.5, 2) under pure scale.
        Vector3f result = m.transformPosition(new Vector3f(0.5f, 0.5f, 0.5f));
        assertEquals(1.0f, result.x(), EPSILON);
        assertEquals(1.5f, result.y(), EPSILON);
        assertEquals(2.0f, result.z(), EPSILON);
    }

    @Test
    void buildModelRotatesYawNinety() {
        Matrix4f m = new Matrix4f();
        // 90° yaw with scale 1: +X maps to -Z.
        EntityRenderSystem.buildModel(m, 0f, 0f, 0f, 90f, 1f, 1f, 1f);

        Vector3f result = m.transformPosition(new Vector3f(1f, 0f, 0f));
        assertEquals(0f,  result.x(), EPSILON);
        assertEquals(0f,  result.y(), EPSILON);
        assertEquals(-1f, result.z(), EPSILON);
    }

    @Test
    void buildModelZeroYawIsIdentityRotation() {
        Matrix4f m = new Matrix4f();
        EntityRenderSystem.buildModel(m, 0f, 0f, 0f, 0f, 1f, 1f, 1f);

        // No rotation: +Z stays at +Z.
        Vector3f result = m.transformPosition(new Vector3f(0f, 0f, 1f));
        assertEquals(0f, result.x(), EPSILON);
        assertEquals(0f, result.y(), EPSILON);
        assertEquals(1f, result.z(), EPSILON);
    }

    @Test
    void buildModelCombinesTranslationRotationScale() {
        Matrix4f m = new Matrix4f();
        // Translate to (10, 0, 0), yaw 0, scale 2 on all axes.
        // Point (1, 0, 0) → scale → (2, 0, 0) → rotate(0) → (2, 0, 0) → translate → (12, 0, 0).
        EntityRenderSystem.buildModel(m, 10f, 0f, 0f, 0f, 2f, 2f, 2f);

        Vector3f result = m.transformPosition(new Vector3f(1f, 0f, 0f));
        assertEquals(12f, result.x(), EPSILON);
        assertEquals(0f,  result.y(), EPSILON);
        assertEquals(0f,  result.z(), EPSILON);
    }
}
