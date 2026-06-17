package org.example.components;

// Cooldown (in seconds) before a freshly dropped item may be picked up, so a block broken at the
// player's feet is not absorbed on the very same tick it spawns. Counts down in ItemPickupSystem and
// is removed once it reaches zero.
public record PickupDelay(float seconds) {}
