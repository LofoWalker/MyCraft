package org.example.systems;

import org.example.audio.AudioPlayer;
import org.example.audio.FootstepCadence;
import org.example.audio.SoundId;
import org.example.components.Grounded;
import org.example.components.PlayerInput;
import org.example.components.Position;
import org.example.components.Rotation;
import org.example.components.SoundEvent;
import org.example.components.Velocity;
import org.example.ecs.Entity;
import org.example.ecs.GameSystem;
import org.example.ecs.World;
import org.example.world.WorldConstants;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Consumes transient {@link SoundEvent} components each tick (emitted by
 * gameplay systems such as {@link BlockInteractionSystem}), updates the OpenAL
 * listener to follow the player camera, ticks footstep cadence, and schedules
 * ambient music at random intervals.
 *
 * <p>All actual AL calls are delegated to {@link AudioPlayer} so the system
 * can be unit-tested without a real audio device.
 */
public final class AudioSystem implements GameSystem {

    // -------------------------------------------------------------------------
    // Tunable constants
    // -------------------------------------------------------------------------

    /** Minimum seconds between two ambient music tracks. */
    public static final float MUSIC_MIN_INTERVAL_SECONDS = 60f;
    /** Maximum seconds between two ambient music tracks. */
    public static final float MUSIC_MAX_INTERVAL_SECONDS = 180f;

    private static final float VOLUME_EFFECTS = 0.8f;
    private static final float VOLUME_MUSIC   = 0.35f;
    private static final float PITCH_NORMAL   = 1.0f;

    // Slight random pitch shift on block sounds for variety.
    private static final float PITCH_VARIANCE  = 0.10f;

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private final AudioPlayer     player;
    private final FootstepCadence footstepCadence = new FootstepCadence();
    private float musicCountdown;

    public AudioSystem(AudioPlayer player) {
        this.player       = player;
        this.musicCountdown = initialMusicDelay();
    }

    // -------------------------------------------------------------------------
    // GameSystem
    // -------------------------------------------------------------------------

    @Override
    public void update(World world, float dt) {
        consumeSoundEvents(world);
        updateListener(world);
        tickFootsteps(world, dt);
        tickMusic(dt);
    }

    // -------------------------------------------------------------------------
    // SoundEvent consumption
    // -------------------------------------------------------------------------

    private void consumeSoundEvents(World world) {
        for (int eid : world.query(SoundEvent.class)) {
            Entity    entity = new Entity(eid);
            SoundEvent event = world.get(entity, SoundEvent.class).orElseThrow();
            world.remove(entity, SoundEvent.class);
            float pitch = PITCH_NORMAL + randomPitchVariance();
            player.play(event.id(), event.x(), event.y(), event.z(), VOLUME_EFFECTS, pitch);
        }
    }

    // -------------------------------------------------------------------------
    // Listener update
    // -------------------------------------------------------------------------

    private void updateListener(World world) {
        int[] players = world.query(Position.class, Rotation.class, Velocity.class, PlayerInput.class);
        if (players.length == 0) return;

        Entity   entity = new Entity(players[0]);
        Position pos    = world.get(entity, Position.class).orElseThrow();
        Rotation rot    = world.get(entity, Rotation.class).orElseThrow();
        Velocity vel    = world.get(entity, Velocity.class).orElseThrow();

        float eyeX = pos.x();
        float eyeY = pos.y() + WorldConstants.PLAYER_EYE_HEIGHT;
        float eyeZ = pos.z();

        float[] forward = forwardVector(rot);
        player.setListener(
                eyeX, eyeY, eyeZ,
                forward[0], forward[1], forward[2],
                0f, 1f, 0f,
                vel.x(), vel.y(), vel.z());
    }

    private static float[] forwardVector(Rotation rot) {
        float yawRad   = (float) Math.toRadians(rot.yaw());
        float pitchRad = (float) Math.toRadians(rot.pitch());
        float cosPitch = (float) Math.cos(pitchRad);
        return new float[]{
            (float)  Math.sin(yawRad) * cosPitch,
            (float)  Math.sin(pitchRad),
            (float) -Math.cos(yawRad) * cosPitch
        };
    }

    // -------------------------------------------------------------------------
    // Footsteps
    // -------------------------------------------------------------------------

    private void tickFootsteps(World world, float dt) {
        int[] players = world.query(Position.class, Velocity.class, PlayerInput.class);
        if (players.length == 0) return;

        Entity   entity   = new Entity(players[0]);
        Velocity vel      = world.get(entity, Velocity.class).orElseThrow();
        Position pos      = world.get(entity, Position.class).orElseThrow();
        boolean  grounded = world.has(entity, Grounded.class);

        float horizontalSpeed = (float) Math.sqrt(vel.x() * vel.x() + vel.z() * vel.z());

        if (!grounded) {
            footstepCadence.reset();
            return;
        }

        if (footstepCadence.tick(horizontalSpeed, dt, true)) {
            player.play(SoundId.STEP, pos.x(), pos.y(), pos.z(), VOLUME_EFFECTS, PITCH_NORMAL);
        }
    }

    // -------------------------------------------------------------------------
    // Ambient music
    // -------------------------------------------------------------------------

    private void tickMusic(float dt) {
        musicCountdown -= dt;
        if (musicCountdown <= 0f) {
            player.playGlobal(SoundId.MUSIC, VOLUME_MUSIC, PITCH_NORMAL);
            musicCountdown = nextMusicInterval();
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static float randomPitchVariance() {
        return (ThreadLocalRandom.current().nextFloat() * 2f - 1f) * PITCH_VARIANCE;
    }

    private static float initialMusicDelay() {
        return MUSIC_MIN_INTERVAL_SECONDS
             + ThreadLocalRandom.current().nextFloat()
               * (MUSIC_MAX_INTERVAL_SECONDS - MUSIC_MIN_INTERVAL_SECONDS);
    }

    private static float nextMusicInterval() {
        return MUSIC_MIN_INTERVAL_SECONDS
             + ThreadLocalRandom.current().nextFloat()
               * (MUSIC_MAX_INTERVAL_SECONDS - MUSIC_MIN_INTERVAL_SECONDS);
    }
}
