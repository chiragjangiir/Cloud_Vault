package com.cloudvault.service;

public final class ObjectKeyPrefixNormalizer {
    private ObjectKeyPrefixNormalizer() {}

    public static String normalize(String key) {
        if (key == null || key.isBlank()) return "";
        String s = key.replaceAll("/+", "/");
        if (s.startsWith("/")) {
            s = s.substring(1);
        }
        return s;
    }
}
