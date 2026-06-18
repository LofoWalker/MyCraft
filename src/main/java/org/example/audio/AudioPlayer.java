package org.example.audio;

/**
 * Mockable seam between AudioSystem (pure logic) and the OpenAL engine (hardware).
 * Tests inject a no-op or recording implementation; the live game uses SoundEngine.
 */
public interface AudioPlayer {

    /**
     * Plays a positional (attenuated) sound at world coordinates.
     *
     * @param id     sound to play
     * @param x      world X of the source
     * @param y      world Y of the source
     * @param z      world Z of the source
     * @param volume gain in [0, 1]
     * @param pitch  pitch multiplier (1.0 = normal)
     */
    void play(SoundId id, float x, float y, float z, float volume, float pitch);

    /**
     * Plays a global (non-positional) sound — used for ambient music and UI feedback.
     *
     * @param id     sound to play
     * @param volume gain in [0, 1]
     * @param pitch  pitch multiplier (1.0 = normal)
     */
    void playGlobal(SoundId id, float volume, float pitch);

    /**
     * Updates the OpenAL listener transform so positional sounds spatialise correctly.
     *
     * @param x        listener X
     * @param y        listener Y
     * @param z        listener Z
     * @param forwardX forward-direction X component
     * @param forwardY forward-direction Y component
     * @param forwardZ forward-direction Z component
     * @param upX      up-direction X component
     * @param upY      up-direction Y component
     * @param upZ      up-direction Z component
     * @param velX     listener velocity X (for Doppler)
     * @param velY     listener velocity Y
     * @param velZ     listener velocity Z
     */
    void setListener(float x, float y, float z,
                     float forwardX, float forwardY, float forwardZ,
                     float upX,     float upY,     float upZ,
                     float velX,    float velY,    float velZ);
}
