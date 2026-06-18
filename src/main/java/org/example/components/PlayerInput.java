package org.example.components;

public record PlayerInput(
        boolean forward,
        boolean backward,
        boolean strafeLeft,
        boolean strafeRight,
        boolean jump,
        boolean descend,
        float mouseDeltaX,
        float mouseDeltaY,
        boolean breakBlock,
        boolean placeBlock,
        boolean eat,
        int scrollDelta,
        int hotbarSelect,
        // Edge-triggered: true on the tick the player pressed E (toggle inventory screen).
        boolean toggleInventory
) {}
