package com.cloudvault.service;

import java.util.Map;
import java.util.regex.Pattern;

public final class ObjectTaggingValidator {
    private static final Pattern KEY_PATTERN = Pattern.compile("^[\\p{L}\\p{Z}\\p{N}_.:/=+\\-@]{1,128}$");
    private static final Pattern VALUE_PATTERN = Pattern.compile("^[\\p{L}\\p{Z}\\p{N}_.:/=+\\-@]{0,256}$");
    public static final int MAX_TAGS = 10;

    private ObjectTaggingValidator() {}

    public static boolean isValidKey(String key) {
        return key != null && KEY_PATTERN.matcher(key).matches();
    }

    public static boolean isValidValue(String value) {
        return value != null && VALUE_PATTERN.matcher(value).matches();
    }

    public static void validate(Map<String, String> tags) {
        if (tags == null) return;
        if (tags.size() > MAX_TAGS) {
            throw new IllegalArgumentException("Object tag count cannot exceed " + MAX_TAGS);
        }
        for (Map.Entry<String, String> entry : tags.entrySet()) {
            if (!isValidKey(entry.getKey())) {
                throw new IllegalArgumentException("Invalid tag key: " + entry.getKey());
            }
            if (!isValidValue(entry.getValue())) {
                throw new IllegalArgumentException("Invalid tag value for key: " + entry.getKey());
            }
        }
    }
}
