package org.example.world;

public final class PerlinNoise {

    private final int[] perm = new int[512];

    public PerlinNoise(long seed) {
        int[] p = new int[256];
        for (int i = 0; i < 256; i++) p[i] = i;
        long rng = seed ^ 0x9e3779b97f4a7c15L;
        for (int i = 255; i > 0; i--) {
            rng = rng * 6364136223846793005L + 1442695040888963407L;
            int j = (int) ((rng >>> 33) % (i + 1));
            int t = p[i]; p[i] = p[j]; p[j] = t;
        }
        for (int i = 0; i < 512; i++) perm[i] = p[i & 255];
    }

    public double noise(double x, double z) {
        int xi = (int) Math.floor(x) & 255;
        int zi = (int) Math.floor(z) & 255;
        double xf = x - Math.floor(x);
        double zf = z - Math.floor(z);

        int aa = perm[perm[xi    ] + zi    ];
        int ba = perm[perm[xi + 1] + zi    ];
        int ab = perm[perm[xi    ] + zi + 1];
        int bb = perm[perm[xi + 1] + zi + 1];

        double u = fade(xf);
        double v = fade(zf);
        return lerp(v,
            lerp(u, grad(aa, xf,     zf    ), grad(ba, xf - 1, zf    )),
            lerp(u, grad(ab, xf,     zf - 1), grad(bb, xf - 1, zf - 1)));
    }

    public double fractal(double x, double z, int octaves, double persistence, double lacunarity) {
        double value = 0;
        double amplitude = 1;
        double frequency = 1;
        double maxValue  = 0;
        for (int i = 0; i < octaves; i++) {
            value    += noise(x * frequency, z * frequency) * amplitude;
            maxValue += amplitude;
            amplitude *= persistence;
            frequency *= lacunarity;
        }
        return value / maxValue;
    }

    private static double fade(double t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    private static double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }

    private static double grad(int hash, double x, double z) {
        return switch (hash & 3) {
            case 0 ->  x + z;
            case 1 -> -x + z;
            case 2 ->  x - z;
            default -> -x - z;
        };
    }
}
