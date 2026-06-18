package org.example.audio;

/**
 * Canonical identifiers for every sound the game can play.
 * Each entry maps to a classpath resource under {@code /sounds/}.
 */
public enum SoundId {

    BLOCK_BREAK ("sounds/block_break.wav"),
    BLOCK_PLACE ("sounds/block_place.wav"),
    STEP        ("sounds/step.wav"),
    HURT        ("sounds/hurt.wav"),
    MOB_AMBIENT ("sounds/mob_ambient.wav"),
    MUSIC       ("sounds/music.wav");

    /** Classpath resource path (leading slash included). */
    public final String resourcePath;

    SoundId(String resourcePath) {
        this.resourcePath = "/" + resourcePath;
    }
}
