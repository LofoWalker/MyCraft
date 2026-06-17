package org.example.components;

// Invincibility window (in seconds) after taking a hit, so a single damage event cannot drain
// health on consecutive ticks. Counted down by HealthSystem; the entity is vulnerable at <= 0.
public record DamageImmunity(float seconds) {}
