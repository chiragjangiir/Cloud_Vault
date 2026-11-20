package com.cloudvault.service;

public class ByteThroughputTracker {
    private long totalBytes = 0;
    private long durationMillis = 0;

    public void recordTransfer(long bytes, long durationMs) {
        if (bytes > 0) this.totalBytes += bytes;
        if (durationMs > 0) this.durationMillis += durationMs;
    }

    public double getThroughputMegabytesPerSec() {
        if (durationMillis <= 0) return 0.0;
        double sec = durationMillis / 1000.0;
        return (totalBytes / (1024.0 * 1024.0)) / sec;
    }
}
