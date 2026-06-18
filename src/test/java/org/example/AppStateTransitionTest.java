package org.example;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the pure AppState transition logic that determines which schedulers run.
 * No GLFW context is required; all logic is exercised via AppStateHolder and the static
 * GameLoop helpers.
 */
class AppStateTransitionTest {

    // -------------------------------------------------------------------------
    // AppStateHolder basic transitions
    // -------------------------------------------------------------------------

    @Test
    void initialStateIsReturnedBeforeAnyAdvance() {
        AppStateHolder holder = new AppStateHolder(AppState.MAIN_MENU);
        assertEquals(AppState.MAIN_MENU, holder.current());
    }

    @Test
    void requestedStateDoesNotTakeEffectUntilAdvance() {
        AppStateHolder holder = new AppStateHolder(AppState.MAIN_MENU);
        holder.request(AppState.IN_GAME);
        // Still the old state until advance() is called
        assertEquals(AppState.MAIN_MENU, holder.current(),
                "Transition must not apply before advance()");
    }

    @Test
    void advanceAppliesPendingTransition() {
        AppStateHolder holder = new AppStateHolder(AppState.MAIN_MENU);
        holder.request(AppState.IN_GAME);
        holder.advance();
        assertEquals(AppState.IN_GAME, holder.current(),
                "Transition must be visible after advance()");
    }

    @Test
    void multipleRequestsBeforeAdvanceAppliesLast() {
        AppStateHolder holder = new AppStateHolder(AppState.MAIN_MENU);
        holder.request(AppState.IN_GAME);
        holder.request(AppState.PAUSED);
        holder.advance();
        assertEquals(AppState.PAUSED, holder.current(),
                "The last request before advance() wins");
    }

    @Test
    void advanceWithoutRequestIsIdempotent() {
        AppStateHolder holder = new AppStateHolder(AppState.IN_GAME);
        holder.advance();
        assertEquals(AppState.IN_GAME, holder.current(),
                "advance() without a request must leave state unchanged");
    }

    // -------------------------------------------------------------------------
    // GameLoop scheduler gate helpers (pure logic, no GLFW)
    // -------------------------------------------------------------------------

    @Test
    void simRunsOnlyInGame() {
        assertTrue(GameLoop.simShouldRun(AppState.IN_GAME));
        assertFalse(GameLoop.simShouldRun(AppState.MAIN_MENU));
        assertFalse(GameLoop.simShouldRun(AppState.PAUSED));
    }

    @Test
    void menuRunsOnlyOnMainMenu() {
        assertTrue(GameLoop.menuShouldRun(AppState.MAIN_MENU));
        assertFalse(GameLoop.menuShouldRun(AppState.IN_GAME));
        assertFalse(GameLoop.menuShouldRun(AppState.PAUSED));
    }

    @Test
    void pauseSchedulerRunsOnlyWhenPaused() {
        assertTrue(GameLoop.pauseShouldRun(AppState.PAUSED));
        assertFalse(GameLoop.pauseShouldRun(AppState.IN_GAME));
        assertFalse(GameLoop.pauseShouldRun(AppState.MAIN_MENU));
    }

    // -------------------------------------------------------------------------
    // Pause freezes simulation, resume re-enables it
    // -------------------------------------------------------------------------

    @Test
    void pauseTransitionFreezesSimAndResumesIt() {
        AppStateHolder holder = new AppStateHolder(AppState.IN_GAME);

        // Simulate pressing Escape
        holder.request(AppState.PAUSED);
        holder.advance();
        assertFalse(GameLoop.simShouldRun(holder.current()),
                "Simulation must be frozen while paused");

        // Simulate pressing Resume
        holder.request(AppState.IN_GAME);
        holder.advance();
        assertTrue(GameLoop.simShouldRun(holder.current()),
                "Simulation must resume after returning to IN_GAME");
    }

    @Test
    void menuToGameTransition() {
        AppStateHolder holder = new AppStateHolder(AppState.MAIN_MENU);
        assertFalse(GameLoop.simShouldRun(holder.current()));

        holder.request(AppState.IN_GAME);
        holder.advance();
        assertTrue(GameLoop.simShouldRun(holder.current()));
    }
}
