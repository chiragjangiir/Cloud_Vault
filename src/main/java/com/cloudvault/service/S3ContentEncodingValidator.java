package com.cloudvault.service;

import java.util.Set;

public final class S3ContentEncodingValidator {
    private static final Set<String> SUPPORTED = Set.of("gzip", "deflate", "br", "identity", "compress");

    private S3ContentEncodingValidator() {}

    public static boolean isSupportedEncoding(String encoding) {
        if (encoding == null) return true;
        return SUPPORTED.contains(encoding.trim().toLowerCase());
    }
}
