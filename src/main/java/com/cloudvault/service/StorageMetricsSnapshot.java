package com.cloudvault.service;

public record StorageMetricsSnapshot(String bucketName, long objectCount, long totalBytes) {
    public double getAverageObjectSize() {
        if (objectCount <= 0) return 0.0;
        return (double) totalBytes / objectCount;
    }
}
