package org.example.worldgen;

// Deterministic spatial hash (SplitMix-style finalizer): the same seed and coordinates always
// produce the same non-negative value, so generation stays reproducible across runs and threads.
public final class SpatialHash {

    private SpatialHash() {}

    public static long hash(int x, int z, long seed) {
        long h = seed ^ (x * 0x9E3779B97F4A7C15L) ^ (z * 0xC2B2AE3D27D4EB4FL);
        h ^= (h >>> 30);
        h *= 0xBF58476D1CE4E5B9L;
        h ^= (h >>> 27);
        h *= 0x94D049BB133111EBL;
        h ^= (h >>> 31);
        return h & 0x7FFFFFFFFFFFFFFFL;
    }
}
