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
import org.example.ecs.World;
import org.example.world.WorldConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests AudioSystem pure logic without a real OpenAL device.
 * All audio calls go through a recording AudioPlayer stub.
 */
class AudioSystemTest {

    // -------------------------------------------------------------------------
    // Recording stub — captures calls for assertion
    // -------------------------------------------------------------------------

    private static final class RecordingPlayer implements AudioPlayer {

        record PlayCall(SoundId id, float x, float y, float z, float volume, float pitch) {}
        record GlobalCall(SoundId id, float volume, float pitch) {}

        final List<PlayCall>   playCalls   = new ArrayList<>();
        final List<GlobalCall> globalCalls = new ArrayList<>();
        boolean listenerUpdated = false;

        @Override
        public void play(SoundId id, float x, float y, float z, float volume, float pitch) {
            playCalls.add(new PlayCall(id, x, y, z, volume, pitch));
        }

        @Override
        public void playGlobal(SoundId id, float volume, float pitch) {
            globalCalls.add(new GlobalCall(id, volume, pitch));
        }

        @Override
        public void setListener(float x, float y, float z,
                                float fwX, float fwY, float fwZ,
                                float upX, float upY, float upZ,
                                float velX, float velY, float velZ) {
            listenerUpdated = true;
        }
    }

    // -------------------------------------------------------------------------
    // Fixtures
    // -------------------------------------------------------------------------

    private static final float DT = 1f / 60f;

    private World           world;
    private Entity          player;
    private RecordingPlayer recorder;
    private AudioSystem     system;

    @BeforeEach
    void setUp() {
        world    = new World();
        recorder = new RecordingPlayer();
        system   = new AudioSystem(recorder);

        player = world.create();
        world.add(player, new Position(10f, 64f, 10f));
        world.add(player, new Rotation(0f, 0f));
        world.add(player, new Velocity(0f, 0f, 0f));
        world.add(player, new PlayerInput(false, false, false, false, false, false, 0f, 0f, false, false,
                false, 0, WorldConstants.NO_HOTBAR_SELECT, false));
    }

    // -------------------------------------------------------------------------
    // SoundEvent consumption
    // -------------------------------------------------------------------------

    @Test
    void soundEventIsConsumedAndPlayed() {
        world.add(player, new SoundEvent(SoundId.BLOCK_BREAK, 5f, 64f, 5f));

        system.update(world, DT);

        assertEquals(1, recorder.playCalls.size(), "Exactly one play call expected");
        RecordingPlayer.PlayCall call = recorder.playCalls.get(0);
        assertEquals(SoundId.BLOCK_BREAK, call.id());
        assertEquals(5f, call.x(), 1e-5f);
        assertEquals(64f, call.y(), 1e-5f);
        assertEquals(5f, call.z(), 1e-5f);
    }

    @Test
    void soundEventIsRemovedAfterConsumption() {
        world.add(player, new SoundEvent(SoundId.BLOCK_PLACE, 0f, 0f, 0f));
        system.update(world, DT);

        // Second tick must not replay it
        recorder.playCalls.clear();
        system.update(world, DT);

        assertTrue(recorder.playCalls.isEmpty(), "SoundEvent should be removed after first consumption");
    }

    @Test
    void multipleSoundEventsInOneTick() {
        // Two entities each have a SoundEvent
        Entity other = world.create();
        world.add(other, new SoundEvent(SoundId.BLOCK_BREAK, 1f, 64f, 1f));
        world.add(player, new SoundEvent(SoundId.BLOCK_PLACE, 2f, 64f, 2f));

        system.update(world, DT);

        assertEquals(2, recorder.playCalls.size(), "Both SoundEvents should be played");
    }

    // -------------------------------------------------------------------------
    // Listener update
    // -------------------------------------------------------------------------

    @Test
    void listenerIsUpdatedEachTick() {
        system.update(world, DT);
        assertTrue(recorder.listenerUpdated, "setListener must be called every tick");
    }

    // -------------------------------------------------------------------------
    // Footstep cadence
    // -------------------------------------------------------------------------

    @Test
    void noFootstepWhenStandingStill() {
        world.add(player, new Grounded());
        world.add(player, new Velocity(0f, 0f, 0f));

        system.update(world, DT);

        long steps = recorder.playCalls.stream()
                .filter(c -> c.id() == SoundId.STEP).count();
        assertEquals(0, steps, "No step when velocity is zero");
    }

    @Test
    void footstepFiresAfterSufficientDistance() {
        world.add(player, new Grounded());
        // Walk at step-distance speed; two seconds covers ~2 step distances, so at least one step
        // must fire (avoids a knife-edge exactly-at-threshold float comparison after 1 second).
        float speed = FootstepCadence.STEP_DISTANCE_METRES;
        world.add(player, new Velocity(speed, 0f, 0f));

        // Run two seconds of simulation (120 ticks at 1/60 each)
        for (int i = 0; i < 120; i++) {
            system.update(world, DT);
        }

        long steps = recorder.playCalls.stream()
                .filter(c -> c.id() == SoundId.STEP).count();
        assertTrue(steps >= 1, "At least one step sound should fire in one second of walking");
    }

    @Test
    void noFootstepWhenAirborne() {
        // Deliberately do NOT add Grounded
        world.add(player, new Velocity(10f, -5f, 0f));

        for (int i = 0; i < 60; i++) {
            system.update(world, DT);
        }

        long steps = recorder.playCalls.stream()
                .filter(c -> c.id() == SoundId.STEP).count();
        assertEquals(0, steps, "No footstep sounds while airborne");
    }

    // -------------------------------------------------------------------------
    // SoundId → source mapping
    // -------------------------------------------------------------------------

    @Test
    void blockBreakSoundIdIsUsed() {
        world.add(player, new SoundEvent(SoundId.BLOCK_BREAK, 1f, 2f, 3f));
        system.update(world, DT);
        assertTrue(recorder.playCalls.stream().anyMatch(c -> c.id() == SoundId.BLOCK_BREAK));
    }

    @Test
    void blockPlaceSoundIdIsUsed() {
        world.add(player, new SoundEvent(SoundId.BLOCK_PLACE, 1f, 2f, 3f));
        system.update(world, DT);
        assertTrue(recorder.playCalls.stream().anyMatch(c -> c.id() == SoundId.BLOCK_PLACE));
    }
}
