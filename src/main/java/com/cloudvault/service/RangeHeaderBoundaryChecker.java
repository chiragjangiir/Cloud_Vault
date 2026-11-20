package com.cloudvault.service;

public final class RangeHeaderBoundaryChecker {
    private RangeHeaderBoundaryChecker() {}

    public static boolean isSatisfiable(long start, long end, long totalSize) {
        if (totalSize <= 0) return false;
        if (start < 0 || end < start) return false;
        return start < totalSize;
    }
}
