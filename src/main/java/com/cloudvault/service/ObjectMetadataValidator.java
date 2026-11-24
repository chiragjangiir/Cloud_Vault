package com.cloudvault.service;

import java.util.Map;

public final class ObjectMetadataValidator {
    public static final int MAX_METADATA_BYTES = 2048; // 2KB S3 limit

    private ObjectMetadataValidator() {}

    public static boolean isWithinSizeLimit(Map<String, String> userMetadata) {
        if (userMetadata == null) return true;
        int totalBytes = 0;
        for (Map.Entry<String, String> entry : userMetadata.entrySet()) {
            if (entry.getKey() != null) totalBytes += entry.getKey().getBytes().length;
            if (entry.getValue() != null) totalBytes += entry.getValue().getBytes().length;
        }
        return totalBytes <= MAX_METADATA_BYTES;
    }
}
