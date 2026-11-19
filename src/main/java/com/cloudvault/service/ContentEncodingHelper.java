package com.cloudvault.service;

import java.util.Locale;
import java.util.Set;

public final class ContentEncodingHelper {
    private static final Set<String> SUPPORTED = Set.of("gzip", "br", "deflate", "identity");

    private ContentEncodingHelper() {}

    public static boolean isSupported(String encoding) {
        if (encoding == null) return false;
        return SUPPORTED.contains(encoding.toLowerCase(Locale.ROOT).trim());
    }
}
