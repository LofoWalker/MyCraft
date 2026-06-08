package org.example.render;

import org.joml.FrustumIntersection;
import org.joml.Matrix4f;

public final class Frustum {

    // Refreshed in place every frame: the render hot loop stays allocation-free.
    private final FrustumIntersection planes = new FrustumIntersection();

    public void update(Matrix4f viewProjection) {
        planes.set(viewProjection);
    }

    public boolean isBoxVisible(float minX, float minY, float minZ,
                                float maxX, float maxY, float maxZ) {
        return planes.testAab(minX, minY, minZ, maxX, maxY, maxZ);
    }
}
