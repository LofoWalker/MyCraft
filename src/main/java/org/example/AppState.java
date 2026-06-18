package org.example;

/**
 * Application lifecycle state. Drives which schedulers run each frame.
 * MAIN_MENU: only the menu UI scheduler runs; no simulation.
 * IN_GAME: full simulation + render schedulers run.
 * PAUSED: simulation frozen, pause overlay rendered instead of simulating.
 */
public enum AppState {
    MAIN_MENU,
    IN_GAME,
    PAUSED
}
