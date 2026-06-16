package org.example.systems;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BlockBreakOverlaySystemTest {

    @Test
    void breakFractionIsZeroBeforeAnyDamage() {
        assertEquals(0f, BlockBreakOverlaySystem.breakFraction(0, 5), 1e-6f);
    }

    @Test
    void breakFractionScalesWithDamage() {
        assertEquals(0.6f, BlockBreakOverlaySystem.breakFraction(3, 5), 1e-6f);
    }

    @Test
    void breakFractionClampsAtOne() {
        assertEquals(1f, BlockBreakOverlaySystem.breakFraction(7, 5), 1e-6f);
    }

    @Test
    void breakFractionTreatsUnbreakableAsFull() {
        assertEquals(1f, BlockBreakOverlaySystem.breakFraction(1, 0), 1e-6f);
    }

    @Test
    void overlayAlphaGrowsWithProgressAndStaysVisible() {
        float none = BlockBreakOverlaySystem.overlayAlpha(0f);
        float half = BlockBreakOverlaySystem.overlayAlpha(0.5f);
        float full = BlockBreakOverlaySystem.overlayAlpha(1f);

        assertTrue(none > 0f, "first hit must already show some overlay");
        assertTrue(none < half && half < full, "alpha must grow with break progress");
        assertTrue(full <= 1f, "alpha must stay within the valid range");
    }
}
