package com.cloudvault.service;

import java.util.Map;

public class ObjectMetadataValidator {
    public static final int MAX_KEY_LENGTH = 128;
    public static final int MAX_VALUE_LENGTH = 2048;

    public static boolean isValidMetadata(Map<String, String> metadata) {
        if (metadata == null) return true;
        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            String k = entry.getKey();
            String v = entry.getValue();
            if (k == null || k.isEmpty() || k.length() > MAX_KEY_LENGTH) return false;
            if (v != null && v.length() > MAX_VALUE_LENGTH) return false;
        }
        return true;
    }
}
