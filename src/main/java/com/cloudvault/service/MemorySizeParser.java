package com.cloudvault.service;

public final class MemorySizeParser {
    private MemorySizeParser() {}

    public static long parseBytes(String sizeStr) {
        if (sizeStr == null || sizeStr.isBlank()) return 0;
        String s = sizeStr.trim().toUpperCase();
        long multiplier = 1;
        if (s.endsWith("KB") || s.endsWith("K")) {
            multiplier = 1024L;
            s = s.replaceAll("[^0-9]", "");
        } else if (s.endsWith("MB") || s.endsWith("M")) {
            multiplier = 1024L * 1024L;
            s = s.replaceAll("[^0-9]", "");
        } else if (s.endsWith("GB") || s.endsWith("G")) {
            multiplier = 1024L * 1024L * 1024L;
            s = s.replaceAll("[^0-9]", "");
        }
        return Long.parseLong(s) * multiplier;
    }
}
