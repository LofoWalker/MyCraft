package org.example.audio;

import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.EnumMap;
import java.util.Map;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.AL11.*;
import static org.lwjgl.openal.ALC10.*;
import static org.lwjgl.stb.STBVorbis.stb_vorbis_decode_memory;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.*;

/**
 * OpenAL-backed audio engine. Opens a device and context on construction,
 * loads all known sound buffers at startup, then provides a reusable source
 * pool so individual play requests never allocate new AL sources at runtime.
 *
 * <p>Must be created on the main thread (AL context is current here).
 * Close via try-with-resources to release the device/context cleanly.
 */
public final class SoundEngine implements AutoCloseable, AudioPlayer {

    // -------------------------------------------------------------------------
    // Audio constants — no magic numbers in callers
    // -------------------------------------------------------------------------

    /** Default master volume for positional block/step sounds. */
    public static final float VOLUME_EFFECTS   = 0.8f;
    /** Volume for ambient music (low, so it stays in the background). */
    public static final float VOLUME_MUSIC     = 0.35f;
    /** Default pitch for all sounds (1.0 = unmodified). */
    public static final float PITCH_NORMAL     = 1.0f;
    /** Slight pitch variance range applied to block sounds for variety. */
    public static final float PITCH_VARIANCE   = 0.1f;
    /** OpenAL reference distance: full volume within this many world units. */
    public static final float ATTENUATION_REFERENCE_DISTANCE = 4.0f;
    /** OpenAL max distance: no further attenuation beyond this. */
    public static final float ATTENUATION_MAX_DISTANCE       = 64.0f;
    /** Number of reusable AL source handles in the pool. */
    private static final int SOURCE_POOL_SIZE = 16;
    /** Source index reserved for long-running music playback. */
    private static final int MUSIC_SOURCE_INDEX = SOURCE_POOL_SIZE - 1;

    // -------------------------------------------------------------------------
    // OpenAL handles
    // -------------------------------------------------------------------------

    private final long device;
    private final long context;
    private final int[] sources   = new int[SOURCE_POOL_SIZE];
    private final Map<SoundId, Integer> buffers = new EnumMap<>(SoundId.class);
    private int nextSource = 0; // round-robin index for positional sources

    // -------------------------------------------------------------------------
    // Construction / teardown
    // -------------------------------------------------------------------------

    public SoundEngine() {
        device  = openDevice();
        context = createContext(device);
        alcMakeContextCurrent(context);
        ALCCapabilities alcCaps = ALC.createCapabilities(device);
        AL.createCapabilities(alcCaps);
        configureDistanceModel();
        generateSources();
        loadAllBuffers();
    }

    private static long openDevice() {
        long dev = alcOpenDevice((ByteBuffer) null);
        if (dev == NULL) {
            throw new RuntimeException("No OpenAL device available");
        }
        return dev;
    }

    private static long createContext(long dev) {
        long ctx = alcCreateContext(dev, (IntBuffer) null);
        if (ctx == NULL) {
            alcCloseDevice(dev);
            throw new RuntimeException("Failed to create OpenAL context");
        }
        return ctx;
    }

    private void configureDistanceModel() {
        alDistanceModel(AL_LINEAR_DISTANCE_CLAMPED);
    }

    private void generateSources() {
        alGenSources(sources);
        for (int src : sources) {
            alSourcef(src, AL_REFERENCE_DISTANCE, ATTENUATION_REFERENCE_DISTANCE);
            alSourcef(src, AL_MAX_DISTANCE,       ATTENUATION_MAX_DISTANCE);
        }
    }

    @Override
    public void close() {
        alDeleteSources(sources);
        buffers.values().forEach(buf -> alDeleteBuffers(buf));
        buffers.clear();
        alcMakeContextCurrent(NULL);
        alcDestroyContext(context);
        alcCloseDevice(device);
    }

    // -------------------------------------------------------------------------
    // Buffer loading
    // -------------------------------------------------------------------------

    private void loadAllBuffers() {
        for (SoundId id : SoundId.values()) {
            loadBuffer(id);
        }
    }

    private void loadBuffer(SoundId id) {
        try {
            int buf = tryLoadWav(id);
            if (buf != AL_NONE) {
                buffers.put(id, buf);
            }
        } catch (Exception e) {
            System.err.println("[Audio] Skipping missing sound " + id + ": " + e.getMessage());
        }
    }

    private int tryLoadWav(SoundId id) throws IOException {
        byte[] raw = readResource(id.resourcePath);
        if (raw == null) return AL_NONE;

        ByteBuffer rawBuf = memAlloc(raw.length).put(raw).flip();
        try (MemoryStack stack = stackPush()) {
            IntBuffer channels   = stack.mallocInt(1);
            IntBuffer sampleRate = stack.mallocInt(1);
            ShortBuffer decoded  = stb_vorbis_decode_memory(rawBuf, channels, sampleRate);
            if (decoded != null) {
                return uploadPcm(decoded, channels.get(0), sampleRate.get(0));
            }
        } finally {
            memFree(rawBuf);
        }
        // Fallback: try loading as raw PCM16 WAV (minimal header parsing)
        return loadRawWav(id.resourcePath);
    }

    private static byte[] readResource(String path) {
        try (InputStream in = SoundEngine.class.getResourceAsStream(path)) {
            if (in == null) return null;
            return in.readAllBytes();
        } catch (IOException e) {
            return null;
        }
    }

    private static int uploadPcm(ShortBuffer pcm, int channels, int sampleRate) {
        int format = channels == 1 ? AL_FORMAT_MONO16 : AL_FORMAT_STEREO16;
        int buf = alGenBuffers();
        alBufferData(buf, format, pcm, sampleRate);
        memFree(pcm);
        return buf;
    }

    /**
     * Minimal WAV loader: reads 44-byte PCM header and uploads the audio data.
     * Returns {@code AL_NONE} when the file is absent or the format is unsupported.
     */
    private static int loadRawWav(String path) {
        byte[] raw = readResource(path);
        if (raw == null || raw.length < 44) return AL_NONE;
        // Byte offsets per the standard PCM WAV spec:
        // 22=channels, 24=sampleRate(LE int), 34=bitsPerSample
        int channels   = (raw[22] & 0xFF) | ((raw[23] & 0xFF) << 8);
        int sampleRate = (raw[24] & 0xFF) | ((raw[25] & 0xFF) << 8)
                       | ((raw[26] & 0xFF) << 16) | ((raw[27] & 0xFF) << 24);
        int bitsPerSample = (raw[34] & 0xFF) | ((raw[35] & 0xFF) << 8);
        if (bitsPerSample != 16) return AL_NONE;
        int format = channels == 1 ? AL_FORMAT_MONO16 : AL_FORMAT_STEREO16;
        int dataLen = raw.length - 44;
        if (dataLen <= 0) return AL_NONE;
        ByteBuffer pcm = memAlloc(dataLen).put(raw, 44, dataLen).flip();
        try {
            int buf = alGenBuffers();
            alBufferData(buf, format, pcm.asShortBuffer(), sampleRate);
            return buf;
        } finally {
            memFree(pcm);
        }
    }

    // -------------------------------------------------------------------------
    // AudioPlayer implementation
    // -------------------------------------------------------------------------

    @Override
    public void play(SoundId id, float x, float y, float z, float volume, float pitch) {
        Integer buf = buffers.get(id);
        if (buf == null) return;
        // Use round-robin pool, never touch the reserved music source
        int sourceIndex = nextSource % (SOURCE_POOL_SIZE - 1);
        nextSource = (nextSource + 1) % (SOURCE_POOL_SIZE - 1);
        int src = sources[sourceIndex];
        alSourceStop(src);
        alSourcei(src, AL_BUFFER,     buf);
        alSourcef(src, AL_GAIN,       volume);
        alSourcef(src, AL_PITCH,      pitch);
        alSource3f(src, AL_POSITION,  x, y, z);
        alSourcei(src, AL_SOURCE_RELATIVE, AL_FALSE);
        alSourcePlay(src);
    }

    @Override
    public void playGlobal(SoundId id, float volume, float pitch) {
        Integer buf = buffers.get(id);
        if (buf == null) return;
        int src = sources[MUSIC_SOURCE_INDEX];
        alSourceStop(src);
        alSourcei(src, AL_BUFFER,    buf);
        alSourcef(src, AL_GAIN,      volume);
        alSourcef(src, AL_PITCH,     pitch);
        alSource3f(src, AL_POSITION, 0f, 0f, 0f);
        alSourcei(src, AL_SOURCE_RELATIVE, AL_TRUE); // non-positional
        alSourcePlay(src);
    }

    /** Returns true when the dedicated music source is currently playing. */
    public boolean isMusicPlaying() {
        return alGetSourcei(sources[MUSIC_SOURCE_INDEX], AL_SOURCE_STATE) == AL_PLAYING;
    }

    @Override
    public void setListener(float x,  float y,  float z,
                            float fwX, float fwY, float fwZ,
                            float upX, float upY, float upZ,
                            float velX, float velY, float velZ) {
        alListener3f(AL_POSITION, x, y, z);
        alListener3f(AL_VELOCITY, velX, velY, velZ);
        try (MemoryStack stack = stackPush()) {
            FloatBuffer orientation = stack.mallocFloat(6)
                    .put(fwX).put(fwY).put(fwZ)
                    .put(upX).put(upY).put(upZ)
                    .flip();
            alListenerfv(AL_ORIENTATION, orientation);
        }
    }
}
