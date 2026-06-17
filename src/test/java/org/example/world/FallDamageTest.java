package org.example.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FallDamageTest {

    @Test
    void noDamageBelowSafeSpeed() {
        assertEquals(0, FallDamage.fromImpactSpeed(0f));
        assertEquals(0, FallDamage.fromImpactSpeed(WorldConstants.SAFE_FALL_SPEED - 1f));
    }

    @Test
    void noDamageExactlyAtThreshold() {
        assertEquals(0, FallDamage.fromImpactSpeed(WorldConstants.SAFE_FALL_SPEED));
    }

    @Test
    void damageAboveThreshold() {
        assertTrue(FallDamage.fromImpactSpeed(WorldConstants.SAFE_FALL_SPEED + 4f) > 0);
    }

    @Test
    void damageGrowsWithImpactSpeed() {
        int small = FallDamage.fromImpactSpeed(WorldConstants.SAFE_FALL_SPEED + 6f);
        int large = FallDamage.fromImpactSpeed(WorldConstants.SAFE_FALL_SPEED + 20f);
        assertTrue(large > small);
    }

    @Test
    void damageNeverNegative() {
        assertTrue(FallDamage.fromImpactSpeed(-50f) >= 0);
    }

    @Test
    void damageMatchesLinearFormula() {
        float excessSpeed = 10f;
        int expected = Math.round(excessSpeed * WorldConstants.FALL_DAMAGE_PER_SPEED);
        assertEquals(expected, FallDamage.fromImpactSpeed(WorldConstants.SAFE_FALL_SPEED + excessSpeed));
    }
}
