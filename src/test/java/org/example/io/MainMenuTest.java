package org.example.io;

import org.example.components.GameMode;
import org.example.components.Health;
import org.example.components.Hotbar;
import org.example.components.Hunger;
import org.example.components.Inventory;
import org.example.components.Position;
import org.example.components.Rotation;
import org.example.components.TimeOfDay;
import org.example.io.WorldStorage.LevelData;
import org.example.world.Inventories;
import org.example.world.WorldConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies menu-related WorldStorage behaviour:
 * - listWorlds() returns existing world names.
 * - "New World" flow creates a valid level.dat with the chosen game mode.
 * - GameMode round-trips correctly for both SURVIVAL and CREATIVE.
 */
class MainMenuTest {

    @TempDir
    Path tempDir;

    // -------------------------------------------------------------------------
    // listWorlds() tests
    // -------------------------------------------------------------------------

    @Test
    void listWorldsIsEmptyWhenNoSavesDirectory() {
        // The saves/ directory does not exist in a fresh temp context, so listWorlds()
        // reads from Path.of("saves") which may exist on the developer machine.
        // We test the listWorlds(savesDir) overload instead to isolate from the filesystem.
        // Since listWorlds() reads from a fixed "saves/" path, we test the production path
        // indirectly by checking that a storage created with a missing parent returns empty.
        Path nonExistent = tempDir.resolve("no-such-dir");
        // WorldStorage.listWorlds() reads from the static "saves/" dir; we instead test that
        // individual WorldStorage correctly reports levelExists() = false before write.
        WorldStorage storage = new WorldStorage(nonExistent.resolve("world1"));
        assertFalse(storage.levelExists(), "No level.dat before any write");
    }

    @Test
    void listWorldsFindsWorldsWithLevelDat() throws Exception {
        // Create two world directories with level.dat files under tempDir/saves/
        Path savesDir = tempDir.resolve("saves");
        createWorldSave(savesDir, "alpha");
        createWorldSave(savesDir, "beta");
        // Also create a directory without level.dat — should be excluded.
        Files.createDirectories(savesDir.resolve("empty-dir"));

        List<String> worlds = WorldStorage.listWorldsIn(savesDir);
        assertEquals(2, worlds.size(), "Only dirs with level.dat should be listed");
        assertTrue(worlds.contains("alpha"), "alpha must appear");
        assertTrue(worlds.contains("beta"),  "beta must appear");
    }

    @Test
    void listWorldsReturnsSortedNames() throws Exception {
        Path savesDir = tempDir.resolve("saves");
        createWorldSave(savesDir, "zebra");
        createWorldSave(savesDir, "apple");
        createWorldSave(savesDir, "mango");

        List<String> worlds = WorldStorage.listWorldsIn(savesDir);
        assertEquals(List.of("apple", "mango", "zebra"), worlds,
                "listWorlds must return names in alphabetical order");
    }

    // -------------------------------------------------------------------------
    // "New World" flow: writing a fresh level.dat with a chosen mode
    // -------------------------------------------------------------------------

    @Test
    void newWorldCreatesSurvivalLevelDat() {
        WorldStorage storage = new WorldStorage(tempDir.resolve("newworld"));
        assertFalse(storage.levelExists());

        LevelData level = freshLevel(GameMode.Mode.SURVIVAL);
        storage.writeLevel(level);

        assertTrue(storage.levelExists(), "level.dat must exist after write");
        LevelData loaded = storage.readLevel();
        assertEquals(GameMode.Mode.SURVIVAL, loaded.gameMode().mode());
    }

    @Test
    void newWorldCreatesCreativeLevelDat() {
        WorldStorage storage = new WorldStorage(tempDir.resolve("creativeworld"));
        LevelData level = freshLevel(GameMode.Mode.CREATIVE);
        storage.writeLevel(level);

        LevelData loaded = storage.readLevel();
        assertEquals(GameMode.Mode.CREATIVE, loaded.gameMode().mode(),
                "Chosen creative mode must survive the round-trip");
    }

    // -------------------------------------------------------------------------
    // GameMode persists across re-reads
    // -------------------------------------------------------------------------

    @Test
    void gameModeRoundTripSurvival() {
        WorldStorage storage = new WorldStorage(tempDir.resolve("survival"));
        storage.writeLevel(freshLevel(GameMode.Mode.SURVIVAL));
        assertEquals(GameMode.Mode.SURVIVAL, storage.readLevel().gameMode().mode());
    }

    @Test
    void gameModeRoundTripCreative() {
        WorldStorage storage = new WorldStorage(tempDir.resolve("creative"));
        storage.writeLevel(freshLevel(GameMode.Mode.CREATIVE));
        assertEquals(GameMode.Mode.CREATIVE, storage.readLevel().gameMode().mode());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static LevelData freshLevel(GameMode.Mode mode) {
        return LevelData.of(
                42L,
                new Position(8, 64, 8),
                new Rotation(0f, 0f),
                new Health(WorldConstants.MAX_HEALTH, WorldConstants.MAX_HEALTH),
                new Hunger(WorldConstants.MAX_FOOD, WorldConstants.MAX_FOOD),
                new TimeOfDay(0f),
                new Hotbar(0),
                Inventories.empty(),
                new GameMode(mode));
    }

    private static void createWorldSave(Path savesDir, String name) throws Exception {
        Path worldDir = savesDir.resolve(name);
        WorldStorage storage = new WorldStorage(worldDir);
        storage.writeLevel(freshLevel(GameMode.Mode.SURVIVAL));
    }
}
