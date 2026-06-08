package org.example;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WindowTest {

    @Test
    void constructorStoresWidthAndHeight() {
        Window w = new Window(1920, 1080, "test");
        assertEquals(1920, w.getWidth());
        assertEquals(1080, w.getHeight());
    }

    @Test
    void getAspectRatioIsWidthOverHeight() {
        Window w = new Window(1920, 1080, "test");
        assertEquals(1920f / 1080f, w.getAspectRatio(), 1e-6f);
    }
}
