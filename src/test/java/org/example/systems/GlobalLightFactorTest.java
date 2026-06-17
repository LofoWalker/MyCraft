package org.example.systems;

import org.example.world.WorldConstants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// Pure-function tests for the day/night global skylight factor.
// Convention: dayFraction 0=dawn, 0.25=noon, 0.5=dusk, 0.75=midnight.
class GlobalLightFactorTest {

    @Test
    void maximalAtNoon() {
        assertEquals(1.0f, TimeSystem.globalLightFactor(0.25f), 1e-5f);
    }

    @Test
    void minimalAtMidnight() {
        assertEquals(WorldConstants.NIGHT_LIGHT, TimeSystem.globalLightFactor(0.75f), 1e-5f);
    }

    @Test
    void staysWithinNightAndFullDay() {
        for (int i = 0; i <= 100; i++) {
            float f = i / 100f;
            float v = TimeSystem.globalLightFactor(f);
            assertTrue(v >= WorldConstants.NIGHT_LIGHT - 1e-5f, "below floor at " + f + ": " + v);
            assertTrue(v <= 1.0f + 1e-5f, "above ceiling at " + f + ": " + v);
        }
    }

    @Test
    void monotonicRiseFromMidnightToNoon() {
        // dawn (0) sits between midnight and noon along the rising arc; sample 0.75..1.0 then 0..0.25.
        float prev = TimeSystem.globalLightFactor(0.75f);
        for (int i = 76; i <= 100; i++) {
            float v = TimeSystem.globalLightFactor(i / 100f);
            assertTrue(v >= prev - 1e-5f, "not rising at " + (i / 100f));
            prev = v;
        }
        for (int i = 0; i <= 25; i++) {
            float v = TimeSystem.globalLightFactor(i / 100f);
            assertTrue(v >= prev - 1e-5f, "not rising at " + (i / 100f));
            prev = v;
        }
    }

    @Test
    void monotonicFallFromNoonToMidnight() {
        float prev = TimeSystem.globalLightFactor(0.25f);
        for (int i = 26; i <= 75; i++) {
            float v = TimeSystem.globalLightFactor(i / 100f);
            assertTrue(v <= prev + 1e-5f, "not falling at " + (i / 100f));
            prev = v;
        }
    }

    @Test
    void noonBrighterThanDawnAndDusk() {
        float noon = TimeSystem.globalLightFactor(0.25f);
        assertTrue(noon > TimeSystem.globalLightFactor(0.0f));
        assertTrue(noon > TimeSystem.globalLightFactor(0.5f));
    }

    @Test
    void sunHeightMatchesConvention() {
        assertEquals(0.0f,  TimeSystem.sunHeight(0.0f),  1e-5f);
        assertEquals(1.0f,  TimeSystem.sunHeight(0.25f), 1e-5f);
        assertEquals(0.0f,  TimeSystem.sunHeight(0.5f),  1e-5f);
        assertEquals(-1.0f, TimeSystem.sunHeight(0.75f), 1e-5f);
    }
}
