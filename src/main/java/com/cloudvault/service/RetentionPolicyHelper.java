package com.cloudvault.service;

import java.time.Instant;

public class RetentionPolicyHelper {
    public static boolean isRetained(Instant retainUntilDate, boolean legalHoldActive) {
        if (legalHoldActive) return true;
        if (retainUntilDate == null) return false;
        return retainUntilDate.isAfter(Instant.now());
    }
}
