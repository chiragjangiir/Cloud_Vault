package com.cloudvault.service;

public class BandwidthThrottle {
    private final long bytesPerSecond;
    private long lastCheckTime;
    private long allowance;

    public BandwidthThrottle(long bytesPerSecond) {
        this.bytesPerSecond = Math.max(1, bytesPerSecond);
        this.allowance = this.bytesPerSecond;
        this.lastCheckTime = System.currentTimeMillis();
    }

    public synchronized boolean tryAcquire(long bytes) {
        long current = System.currentTimeMillis();
        long elapsed = current - lastCheckTime;
        lastCheckTime = current;

        allowance += (elapsed * bytesPerSecond) / 1000;
        if (allowance > bytesPerSecond) {
            allowance = bytesPerSecond;
        }

        if (allowance >= bytes) {
            allowance -= bytes;
            return true;
        }
        return false;
    }

    public long getBytesPerSecond() {
        return bytesPerSecond;
    }
}
