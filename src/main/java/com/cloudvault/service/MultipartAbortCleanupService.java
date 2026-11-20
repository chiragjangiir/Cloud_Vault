package com.cloudvault.service;

import java.time.Instant;

public final class MultipartAbortCleanupService {
    private MultipartAbortCleanupService() {}

    public static boolean isUploadExpired(Instant uploadInitiatedAt, long retentionDays, Instant now) {
        if (uploadInitiatedAt == null || now == null || retentionDays <= 0) return false;
        long elapsedSeconds = now.getEpochSecond() - uploadInitiatedAt.getEpochSecond();
        return elapsedSeconds > (retentionDays * 86400L);
    }
}
