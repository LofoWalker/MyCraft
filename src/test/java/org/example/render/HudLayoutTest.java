package org.example.render;

import org.example.render.HudLayout.Rect;
import org.example.world.WorldConstants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HudLayoutTest {

    private static final int WIDTH  = 1920;
    private static final int HEIGHT = 1080;
    private static final float EPS  = 1e-3f;

    @Test
    void crosshairIsCenteredOnScreen() {
        Rect h = HudLayout.crosshairHorizontal(WIDTH, HEIGHT);
        Rect v = HudLayout.crosshairVertical(WIDTH, HEIGHT);

        assertEquals(WIDTH * 0.5f, h.centerX(), EPS);
        assertEquals(HEIGHT * 0.5f, h.centerY(), EPS);
        assertEquals(WIDTH * 0.5f, v.centerX(), EPS);
        assertEquals(HEIGHT * 0.5f, v.centerY(), EPS);
    }

    @Test
    void crosshairBarsAreOrientedAsAPlus() {
        Rect h = HudLayout.crosshairHorizontal(WIDTH, HEIGHT);
        Rect v = HudLayout.crosshairVertical(WIDTH, HEIGHT);

        assertTrue(h.w() > h.h(), "horizontal bar must be wider than tall");
        assertTrue(v.h() > v.w(), "vertical bar must be taller than wide");
    }

    @Test
    void hotbarIsHorizontallyCentered() {
        int last = WorldConstants.HOTBAR_SLOTS - 1;
        Rect first = HudLayout.slot(0, WIDTH, HEIGHT);
        Rect lastSlot = HudLayout.slot(last, WIDTH, HEIGHT);

        float leftMargin  = first.x();
        float rightMargin = WIDTH - (lastSlot.x() + lastSlot.w());
        assertEquals(leftMargin, rightMargin, EPS, "hotbar must be centered horizontally");
    }

    @Test
    void hotbarSpansComputedWidth() {
        int last = WorldConstants.HOTBAR_SLOTS - 1;
        Rect first = HudLayout.slot(0, WIDTH, HEIGHT);
        Rect lastSlot = HudLayout.slot(last, WIDTH, HEIGHT);

        float span = (lastSlot.x() + lastSlot.w()) - first.x();
        assertEquals(HudLayout.hotbarWidth(), span, EPS);
    }

    @Test
    void slotsSitAboveBottomEdge() {
        Rect slot = HudLayout.slot(0, WIDTH, HEIGHT);
        float bottom = slot.y() + slot.h();
        assertEquals(HEIGHT - HudLayout.HOTBAR_BOTTOM_MARGIN, bottom, EPS);
        assertTrue(slot.y() < HEIGHT, "slot must be on screen");
    }

    @Test
    void slotsAreEvenlySpacedAndSquare() {
        Rect a = HudLayout.slot(0, WIDTH, HEIGHT);
        Rect b = HudLayout.slot(1, WIDTH, HEIGHT);

        assertEquals(HudLayout.SLOT_SIZE, a.w(), EPS);
        assertEquals(HudLayout.SLOT_SIZE, a.h(), EPS);
        assertEquals(HudLayout.SLOT_SIZE + HudLayout.SLOT_GAP, b.x() - a.x(), EPS);
        assertEquals(a.y(), b.y(), EPS, "all slots share the same row");
    }

    @Test
    void selectedSlotFrameEnclosesItsSlot() {
        Rect slot = HudLayout.slot(3, WIDTH, HEIGHT);
        Rect frame = HudLayout.selectionFrame(3, WIDTH, HEIGHT);

        assertTrue(frame.x() < slot.x(), "frame extends left of the slot");
        assertTrue(frame.y() < slot.y(), "frame extends above the slot");
        assertTrue(frame.x() + frame.w() > slot.x() + slot.w(), "frame extends right of the slot");
        assertTrue(frame.y() + frame.h() > slot.y() + slot.h(), "frame extends below the slot");
        assertEquals(slot.centerX(), frame.centerX(), EPS, "frame stays centered on its slot");
        assertEquals(slot.centerY(), frame.centerY(), EPS);
    }

    @Test
    void itemPreviewIsInsetInsideItsSlot() {
        Rect slot = HudLayout.slot(2, WIDTH, HEIGHT);
        Rect item = HudLayout.itemPreview(2, WIDTH, HEIGHT);

        assertTrue(item.x() > slot.x() && item.y() > slot.y());
        assertTrue(item.w() < slot.w() && item.h() < slot.h());
        assertEquals(slot.centerX(), item.centerX(), EPS);
        assertEquals(slot.centerY(), item.centerY(), EPS);
    }

    @Test
    void layoutRecentersAfterResize() {
        Rect wide = HudLayout.slot(0, 2560, 1440);
        Rect narrow = HudLayout.slot(0, 1280, 720);

        float wideLeft   = wide.x();
        float wideRight  = 2560 - (HudLayout.slot(8, 2560, 1440).x() + HudLayout.SLOT_SIZE);
        float narrowLeft = narrow.x();
        float narrowRight = 1280 - (HudLayout.slot(8, 1280, 720).x() + HudLayout.SLOT_SIZE);

        assertEquals(wideLeft, wideRight, EPS);
        assertEquals(narrowLeft, narrowRight, EPS);
        assertNotEquals(wide.x(), narrow.x(), "origin must shift with screen width");
        assertNotEquals(wide.y(), narrow.y(), "row must shift with screen height");
    }
}
