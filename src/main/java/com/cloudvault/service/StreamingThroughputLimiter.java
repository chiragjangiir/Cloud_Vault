package com.cloudvault.service;

public final class StreamingThroughputLimiter {
    private StreamingThroughputLimiter() {}

    public static long calculateThrottleSleepMillis(long bytesTransferred, long elapsedMillis, long maxBytesPerSecond) {
        if (maxBytesPerSecond <= 0 || bytesTransferred <= 0) return 0L;
        long expectedMillis = (bytesTransferred * 1000L) / maxBytesPerSecond;
        if (expectedMillis > elapsedMillis) {
            return expectedMillis - elapsedMillis;
        }
        return 0L;
    }
}
