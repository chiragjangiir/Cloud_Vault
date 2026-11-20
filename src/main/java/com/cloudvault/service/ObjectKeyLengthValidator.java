package com.cloudvault.service;

import java.nio.charset.StandardCharsets;

public final class ObjectKeyLengthValidator {
    public static final int MAX_KEY_BYTES = 1024;

    private ObjectKeyLengthValidator() {}

    public static boolean isValidKeyLength(String key) {
        if (key == null || key.isEmpty()) return false;
        int byteLen = key.getBytes(StandardCharsets.UTF_8).length;
        return byteLen <= MAX_KEY_BYTES;
    }
}
