package org.example.systems;

import org.example.components.Flying;
import org.example.components.PlayerInput;
import org.example.ecs.Entity;
import org.example.ecs.World;
import org.example.world.WorldConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FlightControlSystemTest {

    private World               world;
    private Entity              player;
    private FlightControlSystem system;

    @BeforeEach
    void setUp() {
        world  = new World();
        player = world.create();
        world.add(player, new PlayerInput(false, false, false, false, false, false, 0f, 0f, false, false,
                0, WorldConstants.NO_HOTBAR_SELECT));
        system = new FlightControlSystem();
    }

    private void step(boolean jump, float dt) {
        world.add(player, new PlayerInput(false, false, false, false, jump, false, 0f, 0f, false, false,
                0, WorldConstants.NO_HOTBAR_SELECT));
        system.update(world, dt);
    }

    private void doubleTap() {
        step(true, 0.05f);
        step(false, 0.05f);
        step(true, 0.05f);
        step(false, 0.05f);
    }

    private boolean flying() {
        return world.has(player, Flying.class);
    }

    @Test
    void doubleTapWithinWindowEnablesFlying() {
        doubleTap();
        assertTrue(flying());
    }

    @Test
    void secondDoubleTapDisablesFlying() {
        doubleTap();
        doubleTap();
        assertFalse(flying());
    }

    @Test
    void singleTapDoesNotEnableFlying() {
        step(true, 0.05f);
        step(false, 0.05f);
        assertFalse(flying());
    }

    @Test
    void tapsTooFarApartDoNotToggle() {
        step(true, 0.05f);
        step(false, 0.05f);
        step(true, 0.5f); // second press well beyond the double-tap window
        assertFalse(flying());
    }

    @Test
    void heldSpaceIsASingleTapNotMany() {
        step(true, 0.05f);
        step(true, 0.05f);
        step(true, 0.05f);
        assertFalse(flying());
    }
}
