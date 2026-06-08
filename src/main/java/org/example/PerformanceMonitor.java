package org.example;

import com.sun.management.OperatingSystemMXBean;

import java.lang.management.ManagementFactory;

final class PerformanceMonitor {

    private static final long BYTES_PER_MB = 1024L * 1024L;

    private final OperatingSystemMXBean osBean =
            (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
    private final Runtime runtime = Runtime.getRuntime();

    String title(int fps) {
        long usedMb = (runtime.totalMemory() - runtime.freeMemory()) / BYTES_PER_MB;
        long maxMb  = runtime.maxMemory() / BYTES_PER_MB;
        return format(fps, usedMb, maxMb, osBean.getProcessCpuLoad());
    }

    // cpuLoad is in [0,1], or negative when the JVM cannot yet sample it.
    static String format(int fps, long usedMb, long maxMb, double cpuLoad) {
        String cpu = cpuLoad < 0 ? "--" : String.format("%.0f%%", cpuLoad * 100);
        return String.format("MyCraft | FPS: %d | RAM: %d/%d MB | CPU: %s", fps, usedMb, maxMb, cpu);
    }
}
