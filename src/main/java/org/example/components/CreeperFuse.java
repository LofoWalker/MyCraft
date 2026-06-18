package org.example.components;

/**
 * Tracks how long a Creeper has been within explosion range of the player.
 * When the fuse reaches zero the creeper detonates (area damage; block destruction is a TODO).
 * Absent when the creeper is not in range.
 */
public record CreeperFuse(float secondsRemaining) {}
