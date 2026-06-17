package org.example.components;

// Largest downward speed (positive magnitude) observed during the current airborne stretch. Captured
// by HealthSystem one tick BEFORE CollisionSystem zeroes Velocity.y on landing, so the impact speed
// survives to be turned into fall damage. Reset to zero once consumed or while grounded.
public record FallSpeed(float speed) {}
