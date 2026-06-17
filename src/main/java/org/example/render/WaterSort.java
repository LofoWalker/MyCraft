package org.example.render;

/**
 * Pure ordering helper for the translucent water pass: sorts chunk indices back-to-front
 * (farthest from the camera first) so alpha blending composites correctly. No OpenGL, no
 * allocation per call — the caller owns the {@code order} and {@code distances} scratch arrays
 * and reuses them every frame.
 */
public final class WaterSort {

    private WaterSort() {}

    /**
     * Fills {@code order[0..count)} with {@code 0..count-1} reordered so that the chunk centres at
     * {@code (centerX[i], centerZ[i])} are visited farthest-first relative to {@code (camX, camZ)}.
     * {@code distances} (length ≥ count) is used as scratch for the squared distances. Insertion
     * sort keeps it allocation-free and is cheap for the handful of water chunks in view.
     */
    public static void backToFront(int count, float camX, float camZ,
                                   float[] centerX, float[] centerZ,
                                   int[] order, float[] distances) {
        for (int i = 0; i < count; i++) {
            order[i] = i;
            float dx = centerX[i] - camX;
            float dz = centerZ[i] - camZ;
            distances[i] = dx * dx + dz * dz;
        }
        for (int i = 1; i < count; i++) {
            int idx = order[i];
            float dist = distances[idx];
            int j = i - 1;
            while (j >= 0 && distances[order[j]] < dist) {
                order[j + 1] = order[j];
                j--;
            }
            order[j + 1] = idx;
        }
    }
}
