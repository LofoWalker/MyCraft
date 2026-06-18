package org.example.components;

/**
 * Tracks the current play mode for an entity (typically the player or the world).
 * SURVIVAL: standard gameplay with gravity, damage, and limited resources.
 * CREATIVE: flying enabled by default, no damage taken, instant block break, infinite resources.
 */
public record GameMode(Mode mode) {

    public enum Mode {
        SURVIVAL,
        CREATIVE
    }

    public boolean isCreative() {
        return mode == Mode.CREATIVE;
    }

    public boolean isSurvival() {
        return mode == Mode.SURVIVAL;
    }
}
