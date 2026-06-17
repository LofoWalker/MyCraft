package org.example.world;

import java.util.Optional;

// Shared voxel raycast (Amanatides & Woo grid traversal). Walks integer cells along the ray and
// returns the first solid voxel within reach, together with the integer normal of the face the ray
// entered through (handy for placing a block against that face). Pure logic, no OpenGL or ECS — it
// reads the world only through a ChunkView, which keeps it unit-testable in memory.
public final class VoxelRaycast {

    private VoxelRaycast() {}

    // faceX/Y/Z is the integer normal of the entered face: e.g. hitting a block from above yields
    // (0, +1, 0). The origin cell has no entry face, so it reports a zero normal.
    public record RaycastHit(int x, int y, int z, int faceX, int faceY, int faceZ) {}

    public static Optional<RaycastHit> cast(float ox, float oy, float oz,
                                            float dx, float dy, float dz,
                                            float reach, ChunkView view) {
        int ix = (int) Math.floor(ox);
        int iy = (int) Math.floor(oy);
        int iz = (int) Math.floor(oz);
        int stepX = signum(dx), stepY = signum(dy), stepZ = signum(dz);
        float tDeltaX = stepDelta(dx);
        float tDeltaY = stepDelta(dy);
        float tDeltaZ = stepDelta(dz);
        float tMaxX = boundaryDistance(ox, dx, stepX);
        float tMaxY = boundaryDistance(oy, dy, stepY);
        float tMaxZ = boundaryDistance(oz, dz, stepZ);

        int faceX = 0, faceY = 0, faceZ = 0;
        float traveled = 0f;
        while (traveled <= reach) {
            if (view.isSolid(ix, iy, iz)) {
                return Optional.of(new RaycastHit(ix, iy, iz, faceX, faceY, faceZ));
            }
            if (tMaxX < tMaxY && tMaxX < tMaxZ) {
                ix += stepX; traveled = tMaxX; tMaxX += tDeltaX;
                faceX = -stepX; faceY = 0; faceZ = 0;
            } else if (tMaxY < tMaxZ) {
                iy += stepY; traveled = tMaxY; tMaxY += tDeltaY;
                faceX = 0; faceY = -stepY; faceZ = 0;
            } else {
                iz += stepZ; traveled = tMaxZ; tMaxZ += tDeltaZ;
                faceX = 0; faceY = 0; faceZ = -stepZ;
            }
        }
        return Optional.empty();
    }

    private static int signum(float v) {
        return v > 0 ? 1 : (v < 0 ? -1 : 0);
    }

    private static float stepDelta(float dir) {
        return dir != 0f ? Math.abs(1f / dir) : Float.POSITIVE_INFINITY;
    }

    // Distance along the ray from the origin to the first voxel boundary it crosses on this axis.
    private static float boundaryDistance(float origin, float dir, int step) {
        if (step == 0) return Float.POSITIVE_INFINITY;
        float cell = (float) Math.floor(origin);
        float nextBoundary = step > 0 ? cell + 1f : cell;
        return (nextBoundary - origin) / dir;
    }
}
