package org.example.components;

// Singleton-world component holding the day/night clock. dayFraction is in [0, 1):
// 0.0 = dawn (sun rising on the eastern horizon), 0.25 = noon (sun highest),
// 0.5 = dusk, 0.75 = midnight (sun lowest). One full lap is a whole day.
public record TimeOfDay(float dayFraction) {}
