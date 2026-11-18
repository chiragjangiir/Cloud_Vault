package com.cloudvault.service;

public final class ByteRateCalculator {
    private ByteRateCalculator() {}

    public static double calculateRateBytesPerSec(long bytesTransferred, long durationMillis) {
        if (bytesTransferred <= 0 || durationMillis <= 0) {
            return 0.0;
        }
        return ((double) bytesTransferred / durationMillis) * 1000.0;
    }

    public static String formatRate(double bytesPerSec) {
        if (bytesPerSec >= 1024.0 * 1024.0 * 1024.0) {
            return String.format("%.2f GB/s", bytesPerSec / (1024.0 * 1024.0 * 1024.0));
        } else if (bytesPerSec >= 1024.0 * 1024.0) {
            return String.format("%.2f MB/s", bytesPerSec / (1024.0 * 1024.0));
        } else if (bytesPerSec >= 1024.0) {
            return String.format("%.2f KB/s", bytesPerSec / 1024.0);
        }
        return String.format("%.0f B/s", bytesPerSec);
    }

    public static long estimateRemainingMillis(long remainingBytes, double bytesPerSec) {
        if (remainingBytes <= 0 || bytesPerSec <= 0.0) {
            return 0L;
        }
        return (long) ((remainingBytes / bytesPerSec) * 1000.0);
    }
}
