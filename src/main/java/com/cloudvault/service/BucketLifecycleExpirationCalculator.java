package com.cloudvault.service;

import java.time.Instant;

public final class BucketLifecycleExpirationCalculator {
    private BucketLifecycleExpirationCalculator() {}

    public static Instant computeExpiry(Instant createdAt, int retentionDays) {
        if (createdAt == null || retentionDays <= 0) return null;
        return createdAt.plusSeconds(retentionDays * 86400L);
    }
}
