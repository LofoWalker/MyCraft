package org.example.components;

/**
 * Marker component that identifies a mob as hostile. Hostile mobs enter the CHASE
 * behaviour when the player is within detection range, attack on contact, and are
 * spawned by HostileSpawnSystem in low-light conditions.
 */
public record HostileTag() {}
