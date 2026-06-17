package org.example.systems;

import org.example.components.TimeOfDay;
import org.example.ecs.Entity;
import org.example.ecs.World;
import org.example.world.WorldConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TimeSystemTest {

    private World      world;
    private Entity     clock;
    private TimeSystem system;

    @BeforeEach
    void setUp() {
        world  = new World();
        clock  = world.create();
        world.add(clock, new TimeOfDay(0f));
        system = new TimeSystem();
    }

    private float currentFraction() {
        return world.get(clock, TimeOfDay.class).orElseThrow().dayFraction();
    }

    @Test
    void dayFractionAdvancesProportionalToDt() {
        float dt = WorldConstants.DAY_LENGTH_SECONDS / 4f; // a quarter of a day
        system.update(world, dt);
        assertEquals(0.25f, currentFraction(), 1e-5f);
    }

    @Test
    void advanceIsLinearInDt() {
        system.update(world, 1f);
        float afterOne = currentFraction();
        assertEquals(1f / WorldConstants.DAY_LENGTH_SECONDS, afterOne, 1e-6f);
        system.update(world, 1f);
        assertEquals(2f / WorldConstants.DAY_LENGTH_SECONDS, currentFraction(), 1e-6f);
    }

    @Test
    void dayFractionWrapsFromOneToZero() {
        world.add(clock, new TimeOfDay(0.99f));
        float dt = WorldConstants.DAY_LENGTH_SECONDS * 0.02f; // pushes past 1.0
        system.update(world, dt);
        float result = currentFraction();
        assertTrue(result >= 0f && result < 1f, "fraction must stay in [0,1): " + result);
        assertEquals(0.01f, result, 1e-5f);
    }

    @Test
    void pureAdvanceWraps() {
        float result = TimeSystem.advance(1f, 0f);
        assertEquals(0f, result, 1e-6f);
    }

    @Test
    void noClockEntityIsNoOp() {
        World empty = new World();
        assertDoesNotThrow(() -> system.update(empty, 1f));
    }

    @Test
    void clockEntityIsNotConsumedAcrossUpdates() {
        system.update(world, 1f);
        system.update(world, 1f);
        assertTrue(world.get(clock, TimeOfDay.class).isPresent());
    }
}
