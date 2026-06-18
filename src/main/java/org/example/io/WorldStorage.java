package org.example.io;

import org.example.components.GameMode;
import org.example.components.Health;
import org.example.components.Hotbar;
import org.example.components.Hunger;
import org.example.components.Inventory;
import org.example.components.ItemStack;
import org.example.components.Position;
import org.example.components.Rotation;
import org.example.components.TimeOfDay;
import org.example.components.VoxelChunkData;
import org.example.world.WorldConstants;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Manages on-disk persistence for a single named world save.
 *
 * <p>Layout: {@code saves/<name>/}
 * <ul>
 *   <li>{@code level.dat} – GZIP-compressed binary: seed, player state, world clock.</li>
 *   <li>{@code region/r.<rx>.<rz>.dat} – GZIP-compressed region file (32×32 chunks).
 *       Each region stores a fixed index table of offsets and a payload of modified chunks.
 *       Unmodified chunks are absent from the file and must be regenerated from the seed.</li>
 * </ul>
 *
 * <p>All disk I/O is expected to run on virtual threads. The {@link #writeLock(int, int)} method
 * provides per-region mutual exclusion so no two workers can corrupt the same region file.
 */
public final class WorldStorage {

    // Format version — bump when the binary layout changes so old saves fail fast.
    // v1: original layout. v2: added gameMode field (STEP-37).
    public static final int REGION_FORMAT_VERSION = 1;
    public static final int LEVEL_FORMAT_VERSION  = 2;

    // Each region covers REGION_CHUNKS × REGION_CHUNKS chunk columns.
    public static final int REGION_CHUNKS = 32;

    private static final int CHUNKS_PER_REGION = REGION_CHUNKS * REGION_CHUNKS;

    // One int per slot: byte offset of the chunk payload within the data section, or 0 = absent.
    private static final int INDEX_BYTES = CHUNKS_PER_REGION * Integer.BYTES;

    private final Path saveRoot;

    // One lock per (regionX, regionZ) pair so two virtual-thread workers never corrupt one file.
    private final ConcurrentHashMap<Long, ReentrantLock> regionLocks = new ConcurrentHashMap<>();

    public WorldStorage(Path saveRoot) {
        this.saveRoot = saveRoot;
    }

    /** Factory for a named world under the global {@code saves/} directory. */
    public static WorldStorage forWorld(String worldName) {
        return new WorldStorage(Path.of("saves", worldName));
    }

    /**
     * Returns the names of all existing world saves found in the {@code saves/} directory.
     * Each name corresponds to a sub-directory that contains a {@code level.dat} file.
     */
    public static java.util.List<String> listWorlds() {
        return listWorldsIn(Path.of("saves"));
    }

    /**
     * Returns the names of all existing world saves found under {@code savesDir}.
     * Used in tests to supply an isolated directory instead of the real {@code saves/} path.
     */
    public static java.util.List<String> listWorldsIn(Path savesDir) {
        if (!Files.exists(savesDir)) return java.util.List.of();
        try (java.util.stream.Stream<Path> entries = Files.list(savesDir)) {
            return entries
                    .filter(Files::isDirectory)
                    .filter(p -> Files.exists(p.resolve("level.dat")))
                    .map(p -> p.getFileName().toString())
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to list worlds in " + savesDir, e);
        }
    }

    // -------------------------------------------------------------------------
    // Chunk I/O
    // -------------------------------------------------------------------------

    /**
     * Reads the chunk at (chunkX, chunkZ) from its region file.
     *
     * @return the stored voxel data, or {@link Optional#empty()} if the chunk was never written.
     */
    public Optional<VoxelChunkData> readChunk(int chunkX, int chunkZ) {
        int rx = regionCoord(chunkX);
        int rz = regionCoord(chunkZ);
        Path regionFile = regionPath(rx, rz);
        if (!Files.exists(regionFile)) return Optional.empty();

        ReentrantLock lock = acquireLock(rx, rz);
        lock.lock();
        try {
            return readChunkFromRegion(regionFile, chunkX, chunkZ);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read chunk (%d,%d)".formatted(chunkX, chunkZ), e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Writes the given chunk data to its region file. Creates the file if it does not exist.
     * This method is thread-safe per region (two chunks in different regions may be written in
     * parallel; two chunks in the same region are serialised by a per-region lock).
     */
    public void writeChunk(int chunkX, int chunkZ, VoxelChunkData data) {
        int rx = regionCoord(chunkX);
        int rz = regionCoord(chunkZ);
        Path regionFile = regionPath(rx, rz);

        ReentrantLock lock = acquireLock(rx, rz);
        lock.lock();
        try {
            upsertChunkInRegion(regionFile, chunkX, chunkZ, data);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write chunk (%d,%d)".formatted(chunkX, chunkZ), e);
        } finally {
            lock.unlock();
        }
    }

    // -------------------------------------------------------------------------
    // Level.dat I/O
    // -------------------------------------------------------------------------

    /** Returns true if a level.dat already exists for this world. */
    public boolean levelExists() {
        return Files.exists(levelPath());
    }

    /**
     * Reads the level.dat and returns the stored player/world state.
     *
     * @throws UncheckedIOException if the file cannot be read or is corrupt.
     */
    public LevelData readLevel() {
        try (InputStream raw = Files.newInputStream(levelPath());
             GZIPInputStream gz = new GZIPInputStream(raw);
             DataInputStream in = new DataInputStream(gz)) {

            int version = in.readInt();
            if (version != LEVEL_FORMAT_VERSION) {
                throw new IOException("Unknown level.dat version: " + version);
            }
            long seed         = in.readLong();
            float posX        = in.readFloat();
            float posY        = in.readFloat();
            float posZ        = in.readFloat();
            float yaw         = in.readFloat();
            float pitch       = in.readFloat();
            int   health      = in.readInt();
            int   maxHealth   = in.readInt();
            int   food        = in.readInt();
            float saturation  = in.readFloat();
            float dayFraction = in.readFloat();
            int   hotbarSlot  = in.readInt();
            ItemStack[] slots = readInventory(in);
            int   gameModeOrd = in.readInt();
            GameMode.Mode modeEnum = gameModeOrd == 1
                    ? GameMode.Mode.CREATIVE
                    : GameMode.Mode.SURVIVAL;

            return new LevelData(
                    seed,
                    new Position(posX, posY, posZ),
                    new Rotation(yaw, pitch),
                    new Health(health, maxHealth),
                    new Hunger(food, saturation),
                    new TimeOfDay(dayFraction),
                    new Hotbar(hotbarSlot),
                    new Inventory(slots),
                    new GameMode(modeEnum));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read level.dat", e);
        }
    }

    /** Writes all player/world state to level.dat (creates or overwrites). */
    public void writeLevel(LevelData level) {
        ensureDirectoryExists(saveRoot);
        try (OutputStream raw = Files.newOutputStream(levelPath());
             GZIPOutputStream gz = new GZIPOutputStream(raw);
             DataOutputStream out = new DataOutputStream(gz)) {

            out.writeInt(LEVEL_FORMAT_VERSION);
            out.writeLong(level.seed());
            Position pos = level.position();
            out.writeFloat(pos.x());
            out.writeFloat(pos.y());
            out.writeFloat(pos.z());
            Rotation rot = level.rotation();
            out.writeFloat(rot.yaw());
            out.writeFloat(rot.pitch());
            Health h = level.health();
            out.writeInt(h.current());
            out.writeInt(h.max());
            Hunger hunger = level.hunger();
            out.writeInt(hunger.food());
            out.writeFloat(hunger.saturation());
            out.writeFloat(level.timeOfDay().dayFraction());
            out.writeInt(level.hotbar().selectedSlot());
            writeInventory(out, level.inventory().slots());
            out.writeInt(level.gameMode().mode() == GameMode.Mode.CREATIVE ? 1 : 0);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write level.dat", e);
        }
    }

    // -------------------------------------------------------------------------
    // Region file implementation
    // -------------------------------------------------------------------------

    /**
     * Region file binary layout (all big-endian, wrapped in GZIP):
     * <pre>
     *   [int]  version (REGION_FORMAT_VERSION)
     *   [int]  regionX
     *   [int]  regionZ
     *   [int × CHUNKS_PER_REGION]  index: byte offset from start of payload section, or 0 = absent
     *   --- payload section ---
     *   For each present chunk (in order of appearance):
     *     [int]  localX (0..REGION_CHUNKS-1)
     *     [int]  localZ (0..REGION_CHUNKS-1)
     *     [int]  byteLength of blocks array
     *     [byte × byteLength]  raw block ids
     * </pre>
     * The index value is a 1-based offset within the payload (so 0 means absent).
     * A chunk is always written atomically by reading, modifying, and rewriting the whole file.
     */
    private Optional<VoxelChunkData> readChunkFromRegion(Path regionFile, int chunkX, int chunkZ)
            throws IOException {
        int localX = Math.floorMod(chunkX, REGION_CHUNKS);
        int localZ = Math.floorMod(chunkZ, REGION_CHUNKS);
        int slotIndex = localZ * REGION_CHUNKS + localX;

        try (InputStream raw = Files.newInputStream(regionFile);
             GZIPInputStream gz = new GZIPInputStream(raw);
             DataInputStream in = new DataInputStream(gz)) {

            int version = in.readInt();
            if (version != REGION_FORMAT_VERSION) {
                throw new IOException("Unknown region format version: " + version);
            }
            in.readInt(); // regionX – not needed
            in.readInt(); // regionZ – not needed

            // Read the whole index to find this slot
            int[] index = new int[CHUNKS_PER_REGION];
            for (int i = 0; i < CHUNKS_PER_REGION; i++) {
                index[i] = in.readInt();
            }
            if (index[slotIndex] == 0) return Optional.empty();

            // Skip payload chunks before the one we want (1-based offset in order of chunks)
            int targetOrder = index[slotIndex]; // order in payload (1-based)
            for (int order = 1; order < targetOrder; order++) {
                in.readInt(); // localX
                in.readInt(); // localZ
                int len = in.readInt();
                in.skipNBytes(len);
            }
            in.readInt(); // localX
            in.readInt(); // localZ
            int len = in.readInt();
            byte[] blocks = in.readNBytes(len);
            return Optional.of(new VoxelChunkData(blocks));
        }
    }

    /**
     * Reads the existing region file (if any), adds/replaces the given chunk, and rewrites the file.
     * The whole file is small enough (32×32 chunks × 32×32×256 bytes each, worst case ~8 MB) that
     * full-rewrite atomicity is acceptable for a game save.
     */
    private void upsertChunkInRegion(Path regionFile, int chunkX, int chunkZ, VoxelChunkData data)
            throws IOException {
        ensureDirectoryExists(regionPath(regionCoord(chunkX), regionCoord(chunkZ)).getParent());

        // Collect existing chunks from the region (excluding the one we're updating).
        java.util.List<StoredChunk> chunks = new java.util.ArrayList<>();
        if (Files.exists(regionFile)) {
            chunks = readAllChunksFromRegion(regionFile);
        }

        int localX = Math.floorMod(chunkX, REGION_CHUNKS);
        int localZ = Math.floorMod(chunkZ, REGION_CHUNKS);
        chunks.removeIf(c -> c.localX() == localX && c.localZ() == localZ);
        chunks.add(new StoredChunk(localX, localZ, data.blocks()));

        writeRegionFile(regionFile, regionCoord(chunkX), regionCoord(chunkZ), chunks);
    }

    private java.util.List<StoredChunk> readAllChunksFromRegion(Path regionFile) throws IOException {
        java.util.List<StoredChunk> result = new java.util.ArrayList<>();
        try (InputStream raw = Files.newInputStream(regionFile);
             GZIPInputStream gz = new GZIPInputStream(raw);
             DataInputStream in = new DataInputStream(gz)) {

            in.readInt(); // version
            in.readInt(); // regionX
            in.readInt(); // regionZ
            int[] index = new int[CHUNKS_PER_REGION];
            for (int i = 0; i < CHUNKS_PER_REGION; i++) index[i] = in.readInt();

            int totalPresent = 0;
            for (int idx : index) if (idx > 0) totalPresent++;

            for (int i = 0; i < totalPresent; i++) {
                int lx  = in.readInt();
                int lz  = in.readInt();
                int len = in.readInt();
                byte[] blocks = in.readNBytes(len);
                result.add(new StoredChunk(lx, lz, blocks));
            }
        }
        return result;
    }

    private void writeRegionFile(Path regionFile, int rx, int rz,
                                 java.util.List<StoredChunk> chunks) throws IOException {
        try (OutputStream raw = Files.newOutputStream(regionFile);
             GZIPOutputStream gz = new GZIPOutputStream(raw);
             DataOutputStream out = new DataOutputStream(gz)) {

            out.writeInt(REGION_FORMAT_VERSION);
            out.writeInt(rx);
            out.writeInt(rz);

            // Build index: for each slot 0..CHUNKS_PER_REGION-1, the order (1-based) in payload.
            int[] index = new int[CHUNKS_PER_REGION];
            for (int i = 0; i < chunks.size(); i++) {
                StoredChunk c = chunks.get(i);
                int slot = c.localZ() * REGION_CHUNKS + c.localX();
                index[slot] = i + 1; // 1-based order
            }
            for (int idx : index) out.writeInt(idx);

            for (StoredChunk c : chunks) {
                out.writeInt(c.localX());
                out.writeInt(c.localZ());
                out.writeInt(c.blocks().length);
                out.write(c.blocks());
            }
        }
    }

    // -------------------------------------------------------------------------
    // Inventory serialisation
    // -------------------------------------------------------------------------

    private static void writeInventory(DataOutputStream out, ItemStack[] slots) throws IOException {
        out.writeInt(slots.length);
        for (ItemStack s : slots) {
            out.writeInt(s.itemId());
            out.writeInt(s.count());
        }
    }

    private static ItemStack[] readInventory(DataInputStream in) throws IOException {
        int size = in.readInt();
        ItemStack[] slots = new ItemStack[size];
        for (int i = 0; i < size; i++) {
            int itemId = in.readInt();
            int count  = in.readInt();
            slots[i] = count <= 0 ? ItemStack.EMPTY : new ItemStack(itemId, count);
        }
        return slots;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private ReentrantLock acquireLock(int rx, int rz) {
        long key = ((long) rx << 32) | (rz & 0xFFFFFFFFL);
        return regionLocks.computeIfAbsent(key, k -> new ReentrantLock());
    }

    static int regionCoord(int chunkCoord) {
        return Math.floorDiv(chunkCoord, REGION_CHUNKS);
    }

    private Path regionPath(int rx, int rz) {
        return saveRoot.resolve("region").resolve("r.%d.%d.dat".formatted(rx, rz));
    }

    private Path levelPath() {
        return saveRoot.resolve("level.dat");
    }

    private static void ensureDirectoryExists(Path dir) {
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create save directory: " + dir, e);
        }
    }

    // -------------------------------------------------------------------------
    // Internal data holders
    // -------------------------------------------------------------------------

    private record StoredChunk(int localX, int localZ, byte[] blocks) {}

    /** Immutable snapshot of all world and player state written to level.dat. */
    public record LevelData(
            long seed,
            Position position,
            Rotation rotation,
            Health health,
            Hunger hunger,
            TimeOfDay timeOfDay,
            Hotbar hotbar,
            Inventory inventory,
            GameMode gameMode) {

        public static LevelData of(long seed, Position position, Rotation rotation,
                                   Health health, Hunger hunger, TimeOfDay timeOfDay,
                                   Hotbar hotbar, Inventory inventory, GameMode gameMode) {
            return new LevelData(seed, position, rotation, health, hunger, timeOfDay,
                    hotbar, inventory, gameMode);
        }
    }
}
