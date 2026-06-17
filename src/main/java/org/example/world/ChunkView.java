package org.example.world;

// Read-only world voxel lookup, decoupled from the ECS chunk storage so the raycast logic can be
// driven by an in-memory test view as well as the live world.
@FunctionalInterface
public interface ChunkView {
    boolean isSolid(int wx, int wy, int wz);
}
