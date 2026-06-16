package org.example.worldgen;

// Surface height provider for decoration stages (e.g. tree planting): maps a world column to its
// surface Y. Implemented by the real height field (TerrainShape) and by flat-world placeholders.
@FunctionalInterface
public interface SurfaceHeights {
    int surfaceY(int worldX, int worldZ);
}
