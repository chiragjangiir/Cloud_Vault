package com.cloudvault.service;

public final class ObjectMetadataSanitizer {
    private ObjectMetadataSanitizer() {}

    public static String sanitizeValue(String val) {
        if (val == null) return "";
        return val.replaceAll("[\r\n]+", " ").trim();
    }
}
