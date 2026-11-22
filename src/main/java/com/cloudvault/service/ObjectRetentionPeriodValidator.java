package com.cloudvault.service;

import java.time.Instant;

public final class ObjectRetentionPeriodValidator {
    private ObjectRetentionPeriodValidator() {}

    public static boolean isValidRetention(Instant retainUntilDate, Instant referenceNow) {
        if (retainUntilDate == null || referenceNow == null) return false;
        return retainUntilDate.isAfter(referenceNow);
    }
}
