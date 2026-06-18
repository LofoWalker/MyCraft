package org.example.components;

/**
 * Current AI behaviour and how long the mob has been in it. The timer lets each
 * behaviour run for a randomised duration before the AI picks a new one.
 */
public record AiState(Behaviour behaviour, float timer) {

    public enum Behaviour {
        IDLE,
        WANDER,
        FLEE,
        CHASE
    }
}
