package org.example;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PerformanceMonitorTest {

    @Test
    void formatsFpsRamAndCpu() {
        String title = PerformanceMonitor.format(120, 256, 2048, 0.42);
        assertEquals("MyCraft | FPS: 120 | RAM: 256/2048 MB | CPU: 42%", title);
    }

    @Test
    void unavailableCpuLoadShownAsDashes() {
        String title = PerformanceMonitor.format(60, 100, 500, -1.0);
        assertTrue(title.contains("CPU: --"), title);
    }

    @Test
    void cpuLoadRoundedToWholePercent() {
        String title = PerformanceMonitor.format(60, 100, 500, 0.999);
        assertTrue(title.contains("CPU: 100%"), title);
    }
}
