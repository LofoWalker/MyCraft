package org.example.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AmbientOcclusionTest {

    @Test
    void bothSidesSolidGivesDarkestLevelRegardlessOfCorner() {
        assertEquals(0, AmbientOcclusion.cornerLevel(true, true, false));
        assertEquals(0, AmbientOcclusion.cornerLevel(true, true, true));
    }

    @Test
    void fullyOpenCornerGivesMaxLevel() {
        assertEquals(AmbientOcclusion.MAX_LEVEL,
                AmbientOcclusion.cornerLevel(false, false, false));
    }

    @Test
    void oneSideSolidGivesLevelTwo() {
        assertEquals(2, AmbientOcclusion.cornerLevel(true, false, false));
        assertEquals(2, AmbientOcclusion.cornerLevel(false, true, false));
    }

    @Test
    void cornerOnlySolidGivesLevelTwo() {
        assertEquals(2, AmbientOcclusion.cornerLevel(false, false, true));
    }

    @Test
    void oneSideAndCornerSolidGivesLevelOne() {
        assertEquals(1, AmbientOcclusion.cornerLevel(true, false, true));
    }

    @Test
    void factorTableMapsLevelsMonotonically() {
        assertEquals(0.5f,  AmbientOcclusion.factor(0), 1e-6f);
        assertEquals(0.7f,  AmbientOcclusion.factor(1), 1e-6f);
        assertEquals(0.85f, AmbientOcclusion.factor(2), 1e-6f);
        assertEquals(1.0f,  AmbientOcclusion.factor(3), 1e-6f);
    }

    @Test
    void openCornerFactorIsOnePointZero() {
        assertEquals(1.0f, AmbientOcclusion.factor(false, false, false), 1e-6f);
    }

    @Test
    void boxedCornerFactorIsDarkest() {
        assertEquals(0.5f, AmbientOcclusion.factor(true, true, false), 1e-6f);
    }

    @Test
    void flipWhenDiagonalV0V2IsMoreOccluded() {
        // v0,v2 darker (lower level) than v1,v3 → v0+v2 > v1+v3 is false; flip is the inverse.
        // shouldFlip uses ao0+ao2 > ao1+ao3, so balance flips only on the v1/v3-dark case.
        assertTrue(AmbientOcclusion.shouldFlip(3, 0, 3, 0));   // 6 > 0
        assertFalse(AmbientOcclusion.shouldFlip(0, 3, 0, 3));  // 0 > 6 false
    }

    @Test
    void noFlipWhenBalanced() {
        assertFalse(AmbientOcclusion.shouldFlip(3, 3, 3, 3));
        assertFalse(AmbientOcclusion.shouldFlip(2, 2, 2, 2));
    }
}
