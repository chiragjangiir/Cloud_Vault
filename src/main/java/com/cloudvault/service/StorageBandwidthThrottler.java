package com.cloudvault.service;

public class StorageBandwidthThrottler {
    private final long maxBytesPerSecond;

    public StorageBandwidthThrottler(long maxBytesPerSecond) {
        this.maxBytesPerSecond = maxBytesPerSecond;
    }

    public long calculateSleepMillis(long bytesToTransfer) {
        if (maxBytesPerSecond <= 0 || bytesToTransfer <= 0) return 0;
        double seconds = (double) bytesToTransfer / maxBytesPerSecond;
        return (long) (seconds * 1000.0);
    }
}
