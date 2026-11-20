package com.cloudvault.service;

import java.time.Instant;

public final class S3HeaderDateValidator {
    public static final long MAX_CLOCK_SKEW_SECONDS = 900L; // 15 minutes

    private S3HeaderDateValidator() {}

    public static boolean isClockSkewAcceptable(Instant requestDate, Instant serverDate) {
        if (requestDate == null || serverDate == null) return false;
        long diff = Math.abs(serverDate.getEpochSecond() - requestDate.getEpochSecond());
        return diff <= MAX_CLOCK_SKEW_SECONDS;
    }
}
