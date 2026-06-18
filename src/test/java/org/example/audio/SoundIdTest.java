package org.example.audio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies SoundId enum contracts — no audio device required.
 */
class SoundIdTest {

    @Test
    void allSoundIdsHaveResourcePath() {
        for (SoundId id : SoundId.values()) {
            assertNotNull(id.resourcePath, "resourcePath must not be null for " + id);
            assertFalse(id.resourcePath.isBlank(), "resourcePath must not be blank for " + id);
        }
    }

    @Test
    void resourcePathStartsWithLeadingSlash() {
        for (SoundId id : SoundId.values()) {
            assertTrue(id.resourcePath.startsWith("/"),
                    "resourcePath should start with '/' so Class.getResourceAsStream works: " + id);
        }
    }

    @Test
    void blockBreakAndPlaceAreDefined() {
        // These are the two sounds the BlockInteractionSystem depends on.
        assertDoesNotThrow(() -> SoundId.valueOf("BLOCK_BREAK"));
        assertDoesNotThrow(() -> SoundId.valueOf("BLOCK_PLACE"));
    }

    @Test
    void stepSoundIsDefined() {
        assertDoesNotThrow(() -> SoundId.valueOf("STEP"));
    }

    @Test
    void musicSoundIsDefined() {
        assertDoesNotThrow(() -> SoundId.valueOf("MUSIC"));
    }
}
