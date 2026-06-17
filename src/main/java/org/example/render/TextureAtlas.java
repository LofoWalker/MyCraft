package org.example.render;

import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.stb.STBImage.stbi_image_free;
import static org.lwjgl.stb.STBImage.stbi_load_from_memory;

/**
 * The single block texture atlas: a 16×16 grid of 16px tiles painted with top-left origin
 * (tile {@code index = tileY * TILES_PER_ROW + tileX} maps to pixel {@code (tileX*16, tileY*16)}
 * from the top of the PNG). Loaded once via stb_image with NO vertical flip (so the in-memory
 * row order matches the painted layout) and uploaded to a GL_NEAREST / GL_CLAMP_TO_EDGE texture
 * for crisp, bleed-free voxel faces.
 *
 * <p>UV computation ({@link #uvForTile}) is a pure function, so it is unit-tested without a GL
 * context. Because the image is not flipped, UVs use a top-left origin: v grows downward.
 */
public final class TextureAtlas implements AutoCloseable {

    public static final int TILE_PIXELS    = 16;
    public static final int TILES_PER_ROW  = 16;
    public static final int ATLAS_PIXELS   = TILE_PIXELS * TILES_PER_ROW;

    // Half-texel inset shrinks each tile's UV rect inward so bilinear/edge sampling never reaches
    // into a neighbouring tile. Tuned in texels and converted to normalized UV space.
    private static final float UV_INSET = 0.5f / ATLAS_PIXELS;

    private static final int RGBA_CHANNELS = 4;

    private final int textureId;

    private TextureAtlas(int textureId) {
        this.textureId = textureId;
    }

    public static TextureAtlas loadFromClasspath(String resourcePath) {
        ByteBuffer fileBytes = readResource(resourcePath);
        try {
            return new TextureAtlas(uploadTexture(fileBytes));
        } finally {
            org.lwjgl.system.MemoryUtil.memFree(fileBytes);
        }
    }

    private static int uploadTexture(ByteBuffer fileBytes) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer width    = stack.mallocInt(1);
            IntBuffer height   = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            ByteBuffer pixels = stbi_load_from_memory(fileBytes, width, height, channels, RGBA_CHANNELS);
            if (pixels == null) {
                throw new RuntimeException("Failed to decode texture atlas: " + org.lwjgl.stb.STBImage.stbi_failure_reason());
            }
            try {
                return createGlTexture(pixels, width.get(0), height.get(0));
            } finally {
                stbi_image_free(pixels);
            }
        }
    }

    private static int createGlTexture(ByteBuffer pixels, int width, int height) {
        int id = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, id);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
        glBindTexture(GL_TEXTURE_2D, 0);
        return id;
    }

    /**
     * UV rect for the tile at grid position {@code (tileX, tileY)}, top-left origin.
     * Returns {@code {u0, v0, u1, v1}} where {@code (u0,v0)} is the top-left corner and
     * {@code (u1,v1)} the bottom-right, both inset by half a texel and clamped to [0,1].
     */
    public static float[] uvForTile(int tileX, int tileY) {
        float tileSize = 1.0f / TILES_PER_ROW;
        float u0 = tileX * tileSize + UV_INSET;
        float v0 = tileY * tileSize + UV_INSET;
        float u1 = (tileX + 1) * tileSize - UV_INSET;
        float v1 = (tileY + 1) * tileSize - UV_INSET;
        return new float[]{ u0, v0, u1, v1 };
    }

    /** UV rect for a linear tile index ({@code index = tileY * TILES_PER_ROW + tileX}). */
    public static float[] uvForTile(int tileIndex) {
        return uvForTile(tileIndex % TILES_PER_ROW, tileIndex / TILES_PER_ROW);
    }

    public void bind(int unit) {
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(GL_TEXTURE_2D, textureId);
    }

    @Override
    public void close() {
        glDeleteTextures(textureId);
    }

    private static ByteBuffer readResource(String path) {
        try (InputStream is = TextureAtlas.class.getResourceAsStream(path)) {
            if (is == null) throw new RuntimeException("Texture resource not found: " + path);
            byte[] bytes = is.readAllBytes();
            ByteBuffer buffer = org.lwjgl.system.MemoryUtil.memAlloc(bytes.length);
            buffer.put(bytes).flip();
            return buffer;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read texture: " + path, e);
        }
    }
}
