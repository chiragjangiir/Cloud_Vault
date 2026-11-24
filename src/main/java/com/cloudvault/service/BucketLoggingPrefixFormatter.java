package com.cloudvault.service;

public final class BucketLoggingPrefixFormatter {
    private BucketLoggingPrefixFormatter() {}

    public static String formatLoggingPrefix(String prefix) {
        if (prefix == null || prefix.trim().isEmpty()) {
            return "logs/";
        }
        String clean = prefix.trim();
        while (clean.startsWith("/")) clean = clean.substring(1);
        if (!clean.endsWith("/")) clean = clean + "/";
        return clean;
    }
}
