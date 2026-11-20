package com.cloudvault.service;

public final class PresignedUrlExpiryValidator {
    public static final long MAX_ALLOWED_EXPIRY_SECONDS = 604800L; // 7 days

    private PresignedUrlExpiryValidator() {}

    public static boolean isWithinAllowedMax(long requestedExpirySeconds) {
        return requestedExpirySeconds > 0 && requestedExpirySeconds <= MAX_ALLOWED_EXPIRY_SECONDS;
    }
}
