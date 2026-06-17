package org.example.systems;

import org.example.components.Hotbar;
import org.example.components.PlayerInput;
import org.example.ecs.Entity;
import org.example.ecs.GameSystem;
import org.example.ecs.World;
import org.example.world.WorldConstants;

// Turns hotbar input into the selected slot: a number-key press (1..9) jumps straight to that slot,
// and the scroll wheel steps the selection forward/back, wrapping around 0..HOTBAR_SLOTS-1. Stateless;
// reads PlayerInput and rewrites Hotbar.
public final class HotbarSelectionSystem implements GameSystem {

    @Override
    public void update(World world, float dt) {
        for (int eid : world.query(PlayerInput.class, Hotbar.class)) {
            Entity      entity = new Entity(eid);
            PlayerInput input  = world.get(entity, PlayerInput.class).orElseThrow();
            Hotbar      hotbar = world.get(entity, Hotbar.class).orElseThrow();
            int next = nextSlot(hotbar.selectedSlot(), input);
            if (next != hotbar.selectedSlot()) world.add(entity, new Hotbar(next));
        }
    }

    private static int nextSlot(int current, PlayerInput input) {
        if (input.hotbarSelect() != WorldConstants.NO_HOTBAR_SELECT) return input.hotbarSelect();
        return wrap(current + input.scrollDelta());
    }

    private static int wrap(int slot) {
        int slots = WorldConstants.HOTBAR_SLOTS;
        return ((slot % slots) + slots) % slots;
    }
}
