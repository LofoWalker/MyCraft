package org.example.components;

import org.example.render.Mesh;

import java.util.Optional;

/**
 * Rendering meshes for one chunk: the opaque solid geometry plus an optional translucent water
 * mesh. Water is a separate mesh so it can be drawn in its own back-to-front, depth-write-off pass
 * (STEP-20). A chunk with no water cells carries an empty {@code water}.
 */
public record ChunkMeshComponent(Mesh opaque, Optional<Mesh> water) {

    public ChunkMeshComponent {
        if (opaque == null) throw new IllegalArgumentException("opaque mesh must not be null");
        if (water == null)  throw new IllegalArgumentException("water optional must not be null");
    }

    public static ChunkMeshComponent of(Mesh opaque, Mesh water) {
        return new ChunkMeshComponent(opaque, Optional.ofNullable(water));
    }
}
