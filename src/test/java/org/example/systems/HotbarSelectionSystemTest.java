package org.example.systems;

import org.example.components.Hotbar;
import org.example.components.PlayerInput;
import org.example.ecs.Entity;
import org.example.ecs.World;
import org.example.world.WorldConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HotbarSelectionSystemTest {

    private World                 world;
    private Entity                player;
    private HotbarSelectionSystem system;

    @BeforeEach
    void setUp() {
        world  = new World();
        player = world.create();
        world.add(player, new Hotbar(0));
        system = new HotbarSelectionSystem();
    }

    private void inputScroll(int scroll) {
        world.add(player, input(scroll, WorldConstants.NO_HOTBAR_SELECT));
    }

    private void inputKey(int slot) {
        world.add(player, input(0, slot));
    }

    private static PlayerInput input(int scroll, int hotbarSelect) {
        return new PlayerInput(false, false, false, false, false, false, 0f, 0f, false, false,
                false, scroll, hotbarSelect, false);
    }

    private int selectedSlot() {
        return world.get(player, Hotbar.class).orElseThrow().selectedSlot();
    }

    @Test
    void scrollUpAdvancesSelection() {
        inputScroll(1);
        system.update(world, 0f);
        assertEquals(1, selectedSlot());
    }

    @Test
    void scrollDownStepsBackWithWrap() {
        inputScroll(-1);
        system.update(world, 0f);
        assertEquals(WorldConstants.HOTBAR_SLOTS - 1, selectedSlot());
    }

    @Test
    void scrollWrapsPastLastSlot() {
        world.add(player, new Hotbar(WorldConstants.HOTBAR_SLOTS - 1));
        inputScroll(1);
        system.update(world, 0f);
        assertEquals(0, selectedSlot());
    }

    @Test
    void multiTickScrollAccumulatesAcrossWrap() {
        for (int i = 0; i < WorldConstants.HOTBAR_SLOTS + 2; i++) {
            inputScroll(1);
            system.update(world, 0f);
        }
        assertEquals(2, selectedSlot());
    }

    @Test
    void numberKeyJumpsToSlot() {
        inputKey(5);
        system.update(world, 0f);
        assertEquals(5, selectedSlot());
    }

    @Test
    void numberKeyTakesPrecedenceOverScroll() {
        world.add(player, input(3, 7));
        system.update(world, 0f);
        assertEquals(7, selectedSlot());
    }

    @Test
    void noInputLeavesSelectionUnchanged() {
        world.add(player, new Hotbar(4));
        inputScroll(0);
        system.update(world, 0f);
        assertEquals(4, selectedSlot());
    }
}
