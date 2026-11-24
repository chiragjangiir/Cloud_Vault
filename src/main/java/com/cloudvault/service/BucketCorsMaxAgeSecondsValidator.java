package com.cloudvault.service;

public final class BucketCorsMaxAgeSecondsValidator {
    public static final int MAX_CORS_AGE = 86400; // 24 hours

    private BucketCorsMaxAgeSecondsValidator() {}

    public static boolean isValidMaxAge(int seconds) {
        return seconds >= 0 && seconds <= MAX_CORS_AGE;
    }
}
