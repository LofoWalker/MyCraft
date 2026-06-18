package org.example.components;

// Marker: the chunk's voxel data was edited at least once since it was loaded (a block was broken
// or placed). Only modified chunks are written to region files on unload; untouched chunks are
// regenerated deterministically from the world seed. Distinct from ChunkDirty, which only signals
// that the mesh must be rebuilt and is cleared after the rebuild.
public record ChunkModified() {}
