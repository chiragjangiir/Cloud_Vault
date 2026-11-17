package com.cloudvault.service;

import java.util.Map;

public class HeaderSecurityPolicy {
    public static Map<String, String> getDownloadHeaders() {
        return Map.of(
            "X-Content-Type-Options", "nosniff",
            "X-Frame-Options", "DENY",
            "Content-Security-Policy", "default-src 'none'; sandbox",
            "Referrer-Policy", "no-referrer"
        );
    }
}
