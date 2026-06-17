package org.example.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TextureAtlasTest {

    private static final float TILE_SIZE = 1.0f / TextureAtlas.TILES_PER_ROW;
    private static final float INSET     = 0.5f / TextureAtlas.ATLAS_PIXELS;
    private static final float EPS       = 1e-6f;

    @Test
    void firstTileStartsAtTopLeftCornerInset() {
        float[] uv = TextureAtlas.uvForTile(0, 0);
        assertEquals(INSET,             uv[0], EPS, "u0");
        assertEquals(INSET,             uv[1], EPS, "v0");
        assertEquals(TILE_SIZE - INSET, uv[2], EPS, "u1");
        assertEquals(TILE_SIZE - INSET, uv[3], EPS, "v1");
    }

    @Test
    void tileRectIsInsetInwardOnAllSides() {
        float[] uv = TextureAtlas.uvForTile(3, 0);
        float rawU0 = 3 * TILE_SIZE;
        float rawU1 = 4 * TILE_SIZE;
        assertTrue(uv[0] > rawU0, "u0 inset inward");
        assertTrue(uv[2] < rawU1, "u1 inset inward");
        assertTrue(uv[0] < uv[2], "u0 < u1");
        assertTrue(uv[1] < uv[3], "v0 < v1");
    }

    @Test
    void lastTileStaysWithinUnitRange() {
        int last = TextureAtlas.TILES_PER_ROW - 1;
        float[] uv = TextureAtlas.uvForTile(last, last);
        for (float c : uv) {
            assertTrue(c >= 0f && c <= 1f, "UV component out of [0,1]: " + c);
        }
        assertTrue(uv[2] < 1.0f, "rightmost u1 stays below 1 by the inset");
        assertTrue(uv[3] < 1.0f, "bottommost v1 stays below 1 by the inset");
    }

    @Test
    void linearIndexMatchesGridCoordinates() {
        int tileX = 5, tileY = 2;
        int index = tileY * TextureAtlas.TILES_PER_ROW + tileX;
        assertArrayEquals(TextureAtlas.uvForTile(tileX, tileY), TextureAtlas.uvForTile(index), EPS);
    }

    @Test
    void adjacentTilesDoNotOverlapButShareNoUv() {
        float[] left  = TextureAtlas.uvForTile(0, 0);
        float[] right = TextureAtlas.uvForTile(1, 0);
        assertTrue(left[2] < right[0], "inset gap prevents bleeding between neighbouring tiles");
    }
}
