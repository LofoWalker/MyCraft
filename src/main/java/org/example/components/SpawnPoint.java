package org.example.components;

// Where the entity respawns on death. Stored per-entity so respawn logic stays pure (no reach back
// into Main's spawn constants from a System).
public record SpawnPoint(float x, float y, float z) {}
