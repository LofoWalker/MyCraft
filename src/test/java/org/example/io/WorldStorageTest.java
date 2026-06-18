package org.example.io;

import org.example.components.Health;
import org.example.components.Hotbar;
import org.example.components.Hunger;
import org.example.components.Inventory;
import org.example.components.ItemStack;
import org.example.components.Position;
import org.example.components.Rotation;
import org.example.components.TimeOfDay;
import org.example.components.VoxelChunkData;
import org.example.io.WorldStorage.LevelData;
import org.example.world.Inventories;
import org.example.world.WorldConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class WorldStorageTest {

    @TempDir
    Path tempDir;

    // -------------------------------------------------------------------------
    // Chunk round-trip tests
    // -------------------------------------------------------------------------

    @Test
    void roundTripEmptyChunk() {
        WorldStorage storage = new WorldStorage(tempDir);
        VoxelChunkData original = VoxelChunkData.empty();

        storage.writeChunk(0, 0, original);
        Optional<VoxelChunkData> loaded = storage.readChunk(0, 0);

        assertTrue(loaded.isPresent());
        assertArrayEquals(original.blocks(), loaded.get().blocks());
    }

    @Test
    void roundTripFullChunk() {
        WorldStorage storage = new WorldStorage(tempDir);
        VoxelChunkData original = fullyFilledChunk((byte) WorldConstants.BLOCK_STONE);

        storage.writeChunk(5, -3, original);
        Optional<VoxelChunkData> loaded = storage.readChunk(5, -3);

        assertTrue(loaded.isPresent());
        assertArrayEquals(original.blocks(), loaded.get().blocks());
    }

    @Test
    void roundTripSparseChunk() {
        WorldStorage storage = new WorldStorage(tempDir);
        VoxelChunkData original = VoxelChunkData.empty();
        original.set(0, 64, 0, (byte) WorldConstants.BLOCK_DIRT);
        original.set(15, 128, 15, (byte) WorldConstants.BLOCK_STONE);
        original.set(31, 255, 31, (byte) WorldConstants.BLOCK_GRASS);

        storage.writeChunk(2, 7, original);
        Optional<VoxelChunkData> loaded = storage.readChunk(2, 7);

        assertTrue(loaded.isPresent());
        assertArrayEquals(original.blocks(), loaded.get().blocks());
    }

    @Test
    void absentChunkReturnsEmpty() {
        WorldStorage storage = new WorldStorage(tempDir);
        Optional<VoxelChunkData> loaded = storage.readChunk(99, 99);
        assertTrue(loaded.isEmpty());
    }

    @Test
    void unmodifiedChunkIsNotWritten_ModifiedChunkIsRestored() {
        WorldStorage storage = new WorldStorage(tempDir);

        // Write only the modified chunk; skip the unmodified one.
        VoxelChunkData modifiedData = VoxelChunkData.empty();
        modifiedData.set(5, 70, 5, (byte) WorldConstants.BLOCK_STONE);
        storage.writeChunk(0, 0, modifiedData);
        // Chunk (1, 0) is intentionally never written (unmodified → regenerated from seed).

        Optional<VoxelChunkData> loaded = storage.readChunk(0, 0);
        assertTrue(loaded.isPresent(), "Modified chunk must be present in region file");
        assertArrayEquals(modifiedData.blocks(), loaded.get().blocks(),
                "Modified chunk data must survive round-trip exactly");

        Optional<VoxelChunkData> absent = storage.readChunk(1, 0);
        assertTrue(absent.isEmpty(), "Unmodified chunk must not be stored");
    }

    @Test
    void multipleChunksInSameRegionPreserveAllData() {
        WorldStorage storage = new WorldStorage(tempDir);

        VoxelChunkData chunk00 = VoxelChunkData.empty();
        chunk00.set(0, 60, 0, (byte) WorldConstants.BLOCK_DIRT);

        VoxelChunkData chunk10 = VoxelChunkData.empty();
        chunk10.set(10, 80, 10, (byte) WorldConstants.BLOCK_STONE);

        VoxelChunkData chunk05 = VoxelChunkData.empty();
        chunk05.set(20, 100, 20, (byte) WorldConstants.BLOCK_WOOD);

        storage.writeChunk(0, 0, chunk00);
        storage.writeChunk(1, 0, chunk10);
        storage.writeChunk(0, 5, chunk05);

        assertArrayEquals(chunk00.blocks(), storage.readChunk(0, 0).orElseThrow().blocks());
        assertArrayEquals(chunk10.blocks(), storage.readChunk(1, 0).orElseThrow().blocks());
        assertArrayEquals(chunk05.blocks(), storage.readChunk(0, 5).orElseThrow().blocks());
    }

    @Test
    void overwritingChunkReplacesData() {
        WorldStorage storage = new WorldStorage(tempDir);
        VoxelChunkData first = VoxelChunkData.empty();
        first.set(0, 64, 0, (byte) WorldConstants.BLOCK_DIRT);
        storage.writeChunk(0, 0, first);

        VoxelChunkData second = VoxelChunkData.empty();
        second.set(0, 64, 0, (byte) WorldConstants.BLOCK_STONE);
        storage.writeChunk(0, 0, second);

        VoxelChunkData loaded = storage.readChunk(0, 0).orElseThrow();
        assertEquals(WorldConstants.BLOCK_STONE, loaded.get(0, 64, 0));
    }

    @Test
    void chunksInDifferentRegionsAreIndependent() {
        WorldStorage storage = new WorldStorage(tempDir);

        // Place chunks in different regions (region size = 32 chunks).
        VoxelChunkData chunkA = VoxelChunkData.empty();
        chunkA.set(1, 64, 1, (byte) WorldConstants.BLOCK_DIRT);
        storage.writeChunk(0, 0, chunkA);   // region (0,0)

        VoxelChunkData chunkB = VoxelChunkData.empty();
        chunkB.set(2, 64, 2, (byte) WorldConstants.BLOCK_STONE);
        storage.writeChunk(32, 0, chunkB);  // region (1,0)

        assertArrayEquals(chunkA.blocks(), storage.readChunk(0, 0).orElseThrow().blocks());
        assertArrayEquals(chunkB.blocks(), storage.readChunk(32, 0).orElseThrow().blocks());
        // The other region must not contain region (0,0)'s chunks.
        assertTrue(storage.readChunk(1, 0).isEmpty());
    }

    // -------------------------------------------------------------------------
    // Level.dat round-trip tests
    // -------------------------------------------------------------------------

    @Test
    void roundTripLevelDat() {
        WorldStorage storage = new WorldStorage(tempDir);
        Inventory inventory = sampleInventory();
        LevelData original = LevelData.of(
                12345678L,
                new Position(10.5f, 64.0f, -22.3f),
                new Rotation(45.0f, -30.0f),
                new Health(15, WorldConstants.MAX_HEALTH),
                new Hunger(18, 3.5f),
                new TimeOfDay(0.27f),
                new Hotbar(3),
                inventory);

        storage.writeLevel(original);
        LevelData loaded = storage.readLevel();

        assertEquals(original.seed(), loaded.seed());
        assertEquals(original.position(), loaded.position());
        assertEquals(original.rotation(), loaded.rotation());
        assertEquals(original.health(), loaded.health());
        assertEquals(original.hunger(), loaded.hunger());
        assertEquals(original.hotbar(), loaded.hotbar());
        // TimeOfDay comparison with tolerance for float serialisation.
        assertEquals(original.timeOfDay().dayFraction(), loaded.timeOfDay().dayFraction(), 1e-5f);
        assertInventoryEquals(original.inventory(), loaded.inventory());
    }

    @Test
    void levelExistsFalseBeforeWrite() {
        WorldStorage storage = new WorldStorage(tempDir);
        assertFalse(storage.levelExists());
    }

    @Test
    void levelExistsTrueAfterWrite() {
        WorldStorage storage = new WorldStorage(tempDir);
        LevelData level = LevelData.of(
                42L,
                new Position(0, 64, 0),
                new Rotation(0, 0),
                new Health(WorldConstants.MAX_HEALTH, WorldConstants.MAX_HEALTH),
                new Hunger(WorldConstants.MAX_FOOD, WorldConstants.MAX_FOOD),
                new TimeOfDay(0f),
                new Hotbar(0),
                Inventories.empty());
        storage.writeLevel(level);
        assertTrue(storage.levelExists());
    }

    @Test
    void roundTripLevelDatWithFullInventory() {
        WorldStorage storage = new WorldStorage(tempDir);
        Inventory full = Inventories.empty();
        full = Inventories.add(full, new ItemStack(WorldConstants.BLOCK_STONE,
                WorldConstants.INVENTORY_SLOTS * WorldConstants.MAX_STACK)).inventory();

        LevelData original = LevelData.of(
                999L,
                new Position(0, 0, 0),
                new Rotation(0, 0),
                new Health(WorldConstants.MAX_HEALTH, WorldConstants.MAX_HEALTH),
                new Hunger(WorldConstants.MAX_FOOD, WorldConstants.MAX_FOOD),
                new TimeOfDay(0f),
                new Hotbar(0),
                full);

        storage.writeLevel(original);
        LevelData loaded = storage.readLevel();
        assertInventoryEquals(original.inventory(), loaded.inventory());
    }

    @Test
    void roundTripLevelDatSeed() {
        WorldStorage storage = new WorldStorage(tempDir);
        long expectedSeed = Long.MIN_VALUE;
        LevelData level = LevelData.of(
                expectedSeed,
                new Position(0, 64, 0),
                new Rotation(0, 0),
                new Health(WorldConstants.MAX_HEALTH, WorldConstants.MAX_HEALTH),
                new Hunger(WorldConstants.MAX_FOOD, WorldConstants.MAX_FOOD),
                new TimeOfDay(0f),
                new Hotbar(0),
                Inventories.empty());
        storage.writeLevel(level);
        assertEquals(expectedSeed, storage.readLevel().seed());
    }

    // -------------------------------------------------------------------------
    // Region coord helper test
    // -------------------------------------------------------------------------

    @Test
    void regionCoordGroupsChunksCorrectly() {
        assertEquals(0, WorldStorage.regionCoord(0));
        assertEquals(0, WorldStorage.regionCoord(31));
        assertEquals(1, WorldStorage.regionCoord(32));
        assertEquals(-1, WorldStorage.regionCoord(-1));
        assertEquals(-1, WorldStorage.regionCoord(-32));
        assertEquals(-2, WorldStorage.regionCoord(-33));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static VoxelChunkData fullyFilledChunk(byte blockId) {
        int size = WorldConstants.CHUNK_SIZE_XZ * WorldConstants.CHUNK_SIZE_XZ * WorldConstants.WORLD_HEIGHT;
        byte[] blocks = new byte[size];
        Arrays.fill(blocks, blockId);
        return new VoxelChunkData(blocks);
    }

    private static Inventory sampleInventory() {
        Inventory inv = Inventories.empty();
        inv = Inventories.add(inv, new ItemStack(WorldConstants.BLOCK_STONE, 32)).inventory();
        inv = Inventories.add(inv, new ItemStack(WorldConstants.BLOCK_DIRT, 10)).inventory();
        inv = Inventories.add(inv, new ItemStack(WorldConstants.ITEM_BREAD, 5)).inventory();
        return inv;
    }

    private static void assertInventoryEquals(Inventory expected, Inventory actual) {
        assertEquals(expected.size(), actual.size(), "Inventory size mismatch");
        for (int i = 0; i < expected.size(); i++) {
            assertEquals(Inventories.get(expected, i), Inventories.get(actual, i),
                    "Slot " + i + " mismatch");
        }
    }
}
