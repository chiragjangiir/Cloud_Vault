package com.cloudvault.service;

import java.util.Random;

public class RetryPolicyHelper {
    public static long calculateBackoff(int attempt, long baseDelayMs, long maxDelayMs, Random rng) {
        if (attempt <= 0) return 0;
        long backoff = baseDelayMs * (1L << Math.min(attempt - 1, 30));
        long capped = Math.min(backoff, maxDelayMs);
        if (rng != null) {
            return (long) (rng.nextDouble() * capped);
        }
        return capped;
    }

    public static boolean shouldRetry(int attempt, int maxAttempts, Throwable ex) {
        if (attempt >= maxAttempts) {
            return false;
        }
        if (ex instanceof java.io.IOException || ex instanceof java.net.SocketTimeoutException) {
            return true;
        }
        return false;
    }
}
