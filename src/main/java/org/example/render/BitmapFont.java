package org.example.render;

/**
 * Minimal monospace digit font drawn procedurally as filled pixel-quads — no font.png needed.
 *
 * <p>The ticket asks for a bitmap atlas (font.png) with a documented fallback when a binary PNG
 * cannot be produced reliably. This is that fallback: each digit 0-9 is a {@link #GLYPH_COLS}×
 * {@link #GLYPH_ROWS} dot matrix encoded as a bit mask (one bit per cell, row-major, MSB = top-left).
 * The renderer turns each lit cell into a small solid quad, so item counts read clearly without any
 * texture binding. The glyph table is pure data, so it is unit-testable without a GL context.
 */
public final class BitmapFont {

    public static final int GLYPH_COLS = 3;
    public static final int GLYPH_ROWS = 5;
    // Width advance between consecutive glyphs, in cells (glyph width + one cell of spacing).
    public static final int GLYPH_ADVANCE_CELLS = GLYPH_COLS + 1;

    // 15-bit masks (GLYPH_COLS * GLYPH_ROWS), row-major, MSB = top-left cell.
    // A 3x5 dot-matrix digit set. Bit layout per row: top row = bits 14..12, then 11..9, etc.
    private static final int[] DIGITS = {
        0b111_101_101_101_111, // 0
        0b010_110_010_010_111, // 1
        0b111_001_111_100_111, // 2
        0b111_001_111_001_111, // 3
        0b101_101_111_001_001, // 4
        0b111_100_111_001_111, // 5
        0b111_100_111_101_111, // 6
        0b111_001_001_010_010, // 7
        0b111_101_111_101_111, // 8
        0b111_101_111_001_111, // 9
    };

    private BitmapFont() {}

    public static int glyphMask(int digit) {
        if (digit < 0 || digit > 9) {
            throw new IllegalArgumentException("Only digits 0-9 are supported, got: " + digit);
        }
        return DIGITS[digit];
    }

    public static boolean isCellLit(int digit, int col, int row) {
        if (col < 0 || col >= GLYPH_COLS || row < 0 || row >= GLYPH_ROWS) return false;
        int bitIndex = (GLYPH_ROWS - 1 - row) * GLYPH_COLS + (GLYPH_COLS - 1 - col);
        return (glyphMask(digit) & (1 << bitIndex)) != 0;
    }

    /** Number of glyphs needed to print this non-negative value (at least one for zero). */
    public static int digitCount(int value) {
        if (value <= 0) return 1;
        int count = 0;
        for (int v = value; v > 0; v /= 10) count++;
        return count;
    }

    /** Total pixel width of a number rendered at the given cell size (no trailing spacing). */
    public static float measureWidth(int value, float cellSize) {
        int digits = digitCount(value);
        return (digits * GLYPH_ADVANCE_CELLS - 1) * cellSize;
    }
}
