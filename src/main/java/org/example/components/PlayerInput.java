package org.example.components;

public record PlayerInput(
        boolean forward,
        boolean backward,
        boolean strafeLeft,
        boolean strafeRight,
        boolean jump,
        float mouseDeltaX,
        float mouseDeltaY
) {}
