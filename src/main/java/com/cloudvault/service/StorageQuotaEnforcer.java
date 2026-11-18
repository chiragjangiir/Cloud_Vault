package com.cloudvault.service;

public final class StorageQuotaEnforcer {
    private StorageQuotaEnforcer() {}

    public static boolean canFit(long currentBytes, long additionalBytes, long quotaLimit) {
        if (currentBytes < 0 || additionalBytes < 0 || quotaLimit <= 0) {
            return false;
        }
        return (currentBytes + additionalBytes) <= quotaLimit;
    }

    public static void checkQuota(long currentBytes, long additionalBytes, long quotaLimit) {
        if (!canFit(currentBytes, additionalBytes, quotaLimit)) {
            throw new IllegalStateException(
                String.format("Quota exceeded: current=%d, adding=%d, limit=%d", currentBytes, additionalBytes, quotaLimit)
            );
        }
    }
}
