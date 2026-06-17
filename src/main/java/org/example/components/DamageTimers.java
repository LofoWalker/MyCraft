package org.example.components;

// Per-entity timers driven by HealthSystem. {@code sinceDamage} counts up since the last hit (gates
// passive regen); {@code drownAccum} accumulates submerged time past the breath limit to pace drown
// ticks; {@code regenAccum} paces passive heal ticks. Data-only — all advancement lives in the system.
public record DamageTimers(float sinceDamage, float drownAccum, float regenAccum) {}
