package com.cloudvault.service;

import java.util.Locale;
import java.util.Set;

public final class HttpHeaderWhitelistFilter {
    private static final Set<String> ALLOWED = Set.of(
        "content-type", "content-length", "content-disposition",
        "content-encoding", "cache-control", "x-amz-meta-"
    );

    private HttpHeaderWhitelistFilter() {}

    public static boolean isAllowedHeader(String headerName) {
        if (headerName == null) return false;
        String h = headerName.toLowerCase(Locale.ROOT).trim();
        for (String allowed : ALLOWED) {
            if (h.startsWith(allowed)) return true;
        }
        return false;
    }
}
