package org.example.components;

import org.example.audio.SoundId;

/**
 * Transient component emitted by gameplay systems to request a positional sound.
 * AudioSystem consumes and removes these every tick; gameplay logic never calls
 * the audio engine directly.
 */
public record SoundEvent(SoundId id, float x, float y, float z) {}
