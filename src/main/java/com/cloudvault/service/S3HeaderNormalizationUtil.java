package com.cloudvault.service;

import java.util.HashMap;
import java.util.Map;

public final class S3HeaderNormalizationUtil {
    private S3HeaderNormalizationUtil() {}

    public static Map<String, String> normalizeHeaders(Map<String, String> rawHeaders) {
        if (rawHeaders == null) return new HashMap<>();
        Map<String, String> normalized = new HashMap<>();
        for (Map.Entry<String, String> entry : rawHeaders.entrySet()) {
            if (entry.getKey() == null) continue;
            String key = entry.getKey().trim();
            if (key.toLowerCase().startsWith("x-amz-")) {
                key = key.toLowerCase();
            }
            String value = entry.getValue() != null ? entry.getValue().trim() : "";
            normalized.put(key, value);
        }
        return normalized;
    }
}
