package org.example.components;

// Remaining lungful of air (in seconds) while the head is underwater. Drains submerged, refills in
// air. Drowning damage starts once it hits zero.
public record Breath(float air) {}
