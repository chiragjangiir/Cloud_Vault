package com.cloudvault.service;

public final class StoragePoolCalculator {
    private StoragePoolCalculator() {}

    public static long calculateAvailableBytes(long totalCapacity, long usedBytes, long reserveBytes) {
        long effectiveTotal = Math.max(0, totalCapacity - reserveBytes);
        return Math.max(0, effectiveTotal - usedBytes);
    }
}
