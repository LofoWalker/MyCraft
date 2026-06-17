package org.example.render;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class WaterSortTest {

    @Test
    void ordersFarthestChunkFirst() {
        // Three chunks on the X axis; camera at x=0. Expected back-to-front: 2 (x=20), 1 (x=10), 0 (x=2).
        float[] centerX = { 2f, 10f, 20f };
        float[] centerZ = { 0f, 0f, 0f };
        int[]   order     = new int[3];
        float[] distances = new float[3];

        WaterSort.backToFront(3, 0f, 0f, centerX, centerZ, order, distances);

        assertArrayEquals(new int[]{ 2, 1, 0 }, order);
    }

    @Test
    void usesEuclideanDistanceAcrossBothAxes() {
        // Camera at origin. Chunk 0 near on X, chunk 1 far on Z.
        float[] centerX = { 3f, 0f };
        float[] centerZ = { 0f, 9f };
        int[]   order     = new int[2];
        float[] distances = new float[2];

        WaterSort.backToFront(2, 0f, 0f, centerX, centerZ, order, distances);

        assertArrayEquals(new int[]{ 1, 0 }, order);
    }

    @Test
    void singleChunkIsTrivialOrder() {
        int[]   order     = new int[4];
        float[] distances = new float[4];

        WaterSort.backToFront(1, 5f, 5f, new float[]{ 1f }, new float[]{ 1f }, order, distances);

        assertEquals(0, order[0]);
    }

    @Test
    void respectsCameraPositionNotJustOrigin() {
        // Camera sits at x=15, so the chunk at x=20 is now nearer than the one at x=2.
        float[] centerX = { 2f, 20f };
        float[] centerZ = { 0f, 0f };
        int[]   order     = new int[2];
        float[] distances = new float[2];

        WaterSort.backToFront(2, 15f, 0f, centerX, centerZ, order, distances);

        // x=2 is 13 away, x=20 is 5 away → farthest first = chunk 0.
        assertArrayEquals(new int[]{ 0, 1 }, order);
    }

    @Test
    void buffersAreReusedWithoutAllocation() {
        int[]   order     = new int[8];
        float[] distances = new float[8];

        WaterSort.backToFront(3, 0f, 0f, new float[]{ 1f, 2f, 3f }, new float[]{ 0f, 0f, 0f }, order, distances);
        int[] first = order.clone();
        WaterSort.backToFront(3, 0f, 0f, new float[]{ 3f, 2f, 1f }, new float[]{ 0f, 0f, 0f }, order, distances);

        // The scratch buffers are oversized (capacity 8); only the first `count` entries are meaningful.
        assertArrayEquals(new int[]{ 2, 1, 0 }, Arrays.copyOf(first, 3));
        assertArrayEquals(new int[]{ 0, 1, 2 }, Arrays.copyOf(order, 3));
    }
}
