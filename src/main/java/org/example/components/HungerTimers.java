package org.example.components;

// Per-entity accumulators driven by HungerSystem. {@code exhaustion} builds from activity and is spent
// in EXHAUSTION_THRESHOLD chunks to drain saturation/food; {@code regenAccum} paces hunger-funded heal
// ticks; {@code starveAccum} paces starvation-damage ticks at food == 0. Data-only — all advancement
// lives in HungerMath (pure) and HungerSystem (World access).
public record HungerTimers(float exhaustion, float regenAccum, float starveAccum) {}
