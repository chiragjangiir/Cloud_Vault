package com.cloudvault.service;

public record BucketLoggingConfiguration(String targetBucket, String targetPrefix, boolean enabled) {
    public boolean isValid() {
        if (!enabled) return true;
        return targetBucket != null && !targetBucket.isBlank() && targetPrefix != null;
    }
}
