package org.example.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BitmapFontTest {

    @Test
    void everyDigitHasAtLeastOneLitCell() {
        for (int digit = 0; digit <= 9; digit++) {
            int lit = countLitCells(digit);
            assertTrue(lit > 0, "digit " + digit + " must have lit cells");
        }
    }

    @Test
    void glyphMaskRejectsNonDigits() {
        assertThrows(IllegalArgumentException.class, () -> BitmapFont.glyphMask(-1));
        assertThrows(IllegalArgumentException.class, () -> BitmapFont.glyphMask(10));
    }

    @Test
    void litCellsStayWithinTheGlyphGrid() {
        assertFalse(BitmapFont.isCellLit(8, -1, 0));
        assertFalse(BitmapFont.isCellLit(8, BitmapFont.GLYPH_COLS, 0));
        assertFalse(BitmapFont.isCellLit(8, 0, BitmapFont.GLYPH_ROWS));
    }

    @Test
    void digitOneTopLeftIsBlankAndCenterColumnIsLit() {
        // '1' has an empty top-left corner and a lit center stroke at the top row.
        assertFalse(BitmapFont.isCellLit(1, 0, 0));
        assertTrue(BitmapFont.isCellLit(1, 1, 0));
    }

    @Test
    void digitEightIsFullyFramed() {
        // '8' lights all four corners.
        assertTrue(BitmapFont.isCellLit(8, 0, 0));
        assertTrue(BitmapFont.isCellLit(8, 2, 0));
        assertTrue(BitmapFont.isCellLit(8, 0, 4));
        assertTrue(BitmapFont.isCellLit(8, 2, 4));
    }

    @Test
    void digitCountMatchesDecimalLength() {
        assertEquals(1, BitmapFont.digitCount(0));
        assertEquals(1, BitmapFont.digitCount(7));
        assertEquals(2, BitmapFont.digitCount(64));
        assertEquals(3, BitmapFont.digitCount(640));
    }

    @Test
    void measureWidthGrowsWithDigitCount() {
        float oneDigit = BitmapFont.measureWidth(7, 4f);
        float twoDigits = BitmapFont.measureWidth(64, 4f);
        assertTrue(twoDigits > oneDigit);
    }

    @Test
    void measureWidthHasNoTrailingSpacing() {
        // One digit = GLYPH_COLS cells wide, no advance spacing after it.
        assertEquals(BitmapFont.GLYPH_COLS * 4f, BitmapFont.measureWidth(5, 4f), 1e-4f);
    }

    private static int countLitCells(int digit) {
        int lit = 0;
        for (int row = 0; row < BitmapFont.GLYPH_ROWS; row++) {
            for (int col = 0; col < BitmapFont.GLYPH_COLS; col++) {
                if (BitmapFont.isCellLit(digit, col, row)) lit++;
            }
        }
        return lit;
    }
}
