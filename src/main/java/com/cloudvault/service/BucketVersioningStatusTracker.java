package com.cloudvault.service;

public final class BucketVersioningStatusTracker {
    public enum VersioningStatus {
        ENABLED, SUSPENDED, OFF
    }

    private BucketVersioningStatusTracker() {}

    public static VersioningStatus parseStatus(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return VersioningStatus.OFF;
        }
        String clean = raw.trim().toUpperCase();
        if ("ENABLED".equals(clean)) return VersioningStatus.ENABLED;
        if ("SUSPENDED".equals(clean)) return VersioningStatus.SUSPENDED;
        return VersioningStatus.OFF;
    }
}
