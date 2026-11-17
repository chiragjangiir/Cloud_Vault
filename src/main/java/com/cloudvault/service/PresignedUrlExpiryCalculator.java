package com.cloudvault.service;

public class PresignedUrlExpiryCalculator {
    public static final long MIN_EXPIRY_SECONDS = 60L;
    public static final long MAX_EXPIRY_SECONDS = 604800L; // 7 days

    public static long clampExpiry(long requestedSeconds) {
        return Math.max(MIN_EXPIRY_SECONDS, Math.min(requestedSeconds, MAX_EXPIRY_SECONDS));
    }
}
