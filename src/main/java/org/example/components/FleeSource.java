package org.example.components;

/**
 * Records the world-space XZ position of whatever hurt this mob so that the AI
 * system can steer directly away from it during the FLEE behaviour.
 * Written by MobAiSystem when a hit is detected; absent when the mob is calm.
 */
public record FleeSource(float x, float z) {}
