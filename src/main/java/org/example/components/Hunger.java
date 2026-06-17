package org.example.components;

// Player hunger. {@code food} is the visible bar in 0..WorldConstants.MAX_FOOD; {@code saturation} is
// a hidden reservoir in 0..food that activity drains before it eats into food. Data-only: all draining,
// eating and regen-coupling math lives in world.HungerMath / systems.HungerSystem.
public record Hunger(int food, float saturation) {}
