package com.cloudvault.service;

public final class MultipartPartSizeValidator {
    public static final long MIN_PART_SIZE = 5L * 1024 * 1024; // 5 MB
    public static final long MAX_PART_SIZE = 5L * 1024 * 1024 * 1024; // 5 GB

    private MultipartPartSizeValidator() {}

    public static boolean isValidPartSize(long sizeBytes, boolean isLastPart) {
        if (sizeBytes <= 0 || sizeBytes > MAX_PART_SIZE) {
            return false;
        }
        if (!isLastPart && sizeBytes < MIN_PART_SIZE) {
            return false;
        }
        return true;
    }

    public static void validate(long sizeBytes, boolean isLastPart) {
        if (!isValidPartSize(sizeBytes, isLastPart)) {
            throw new IllegalArgumentException("Invalid multipart upload chunk size: " + sizeBytes);
        }
    }
}
