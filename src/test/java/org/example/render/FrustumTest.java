package org.example.render;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FrustumTest {

    private Frustum frustum;

    @BeforeEach
    void setUp() {
        // Camera at the origin looking down -Z, 90° fov, near 0.1 / far 100.
        Matrix4f projection = new Matrix4f().perspective((float) Math.toRadians(90f), 1f, 0.1f, 100f);
        Matrix4f view = new Matrix4f().lookAt(
            new Vector3f(0f, 0f, 0f),
            new Vector3f(0f, 0f, -1f),
            new Vector3f(0f, 1f, 0f)
        );
        frustum = new Frustum();
        frustum.update(projection.mul(view));
    }

    @Test
    void boxInFrontIsVisible() {
        assertTrue(frustum.isBoxVisible(-1f, -1f, -11f, 1f, 1f, -9f));
    }

    @Test
    void boxBehindCameraIsCulled() {
        assertFalse(frustum.isBoxVisible(-1f, -1f, 9f, 1f, 1f, 11f));
    }

    @Test
    void boxBeyondFarPlaneIsCulled() {
        assertFalse(frustum.isBoxVisible(-1f, -1f, -201f, 1f, 1f, -199f));
    }

    @Test
    void boxFarToTheSideIsCulled() {
        assertFalse(frustum.isBoxVisible(199f, -1f, -11f, 201f, 1f, -9f));
    }

    @Test
    void boxStraddlingAnEdgeStaysVisible() {
        // Partially inside boxes must still be drawn, not culled.
        assertTrue(frustum.isBoxVisible(-1f, -1f, -11f, 1000f, 1f, -9f));
    }
}
