package org.example.world;

/**
 * Pure per-vertex ambient-occlusion math for the chunk mesher (no OpenGL, unit-testable).
 *
 * <p>Follows the 0fps "Ambient Occlusion for Minecraft-like worlds" model: each vertex of a face
 * looks at the three voxel cells touching its corner on the exposed side of the face — the two
 * edge-adjacent cells ({@code side1}, {@code side2}) and the diagonal {@code corner} cell. The
 * occlusion level is 0..3 (0 = darkest, fully boxed-in corner; 3 = fully open), mapped to a
 * brightness factor through {@link #AO_FACTORS}. This factor is multiplied onto the per-vertex
 * light written by the mesher (STEP-21) — it does NOT add a vertex attribute.
 *
 * <p>The quad-flip test ({@link #shouldFlip(int, int, int, int)}) detects the diagonal-gradient
 * artifact: when opposite corners have imbalanced AO, the shared triangulation diagonal must be
 * rotated so the interpolated shade is symmetric.
 */
public final class AmbientOcclusion {

    private AmbientOcclusion() {}

    /** Fully open corner: no occluding neighbour, brightness left untouched. */
    public static final int MAX_LEVEL = 3;

    // Brightness factor per AO level (index = level 0..3). Level 0 (boxed-in corner) is darkest;
    // level 3 (open) leaves brightness unchanged. Values follow the 0fps reference curve.
    public static final float[] AO_FACTORS = { 0.5f, 0.7f, 0.85f, 1.0f };

    /**
     * Occlusion level 0..3 for a vertex from its three corner neighbours' solidity.
     * Two solid sides fully box the corner (level 0) regardless of the diagonal; otherwise the
     * level drops by one for each solid neighbour.
     */
    public static int cornerLevel(boolean side1, boolean side2, boolean corner) {
        if (side1 && side2) return 0;
        int occluders = (side1 ? 1 : 0) + (side2 ? 1 : 0) + (corner ? 1 : 0);
        return MAX_LEVEL - occluders;
    }

    /** Brightness multiplier for an AO level (0..3). */
    public static float factor(int level) {
        return AO_FACTORS[level];
    }

    /** Brightness multiplier straight from the three corner neighbours. */
    public static float factor(boolean side1, boolean side2, boolean corner) {
        return AO_FACTORS[cornerLevel(side1, side2, corner)];
    }

    /**
     * Whether the quad's triangulation diagonal must be flipped to avoid the anisotropy artifact.
     * The default diagonal joins v0–v2; when that pair is more occluded than v1–v3 the gradient
     * bends asymmetrically, so the diagonal is rotated to join v1–v3 instead.
     * AO levels are passed in the face's 4-vertex order (v0, v1, v2, v3).
     */
    public static boolean shouldFlip(int ao0, int ao1, int ao2, int ao3) {
        return ao0 + ao2 > ao1 + ao3;
    }
}
