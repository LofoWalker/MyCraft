package org.example;

/**
 * Mutable holder for the current {@link AppState}. Passed to systems and the game loop so
 * they can inspect and request state transitions from a single authoritative source.
 * Transitions are applied at the start of the next game-loop iteration, so a system that
 * requests IN_GAME during a MAIN_MENU tick will see the new state from the following frame.
 */
public final class AppStateHolder {

    private AppState current;
    private AppState pending;

    public AppStateHolder(AppState initial) {
        this.current = initial;
        this.pending = initial;
    }

    /** The state currently active in this frame. */
    public AppState current() {
        return current;
    }

    /** Request a transition to {@code next}; takes effect at the next frame boundary. */
    public void request(AppState next) {
        this.pending = next;
    }

    /** Called by the game loop at the top of each frame to apply pending transitions. */
    public void advance() {
        current = pending;
    }
}
