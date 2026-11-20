package com.cloudvault.service;

import java.util.Locale;

public final class HttpContentTypeNormalizer {
    private HttpContentTypeNormalizer() {}

    public static String normalize(String contentType) {
        if (contentType == null || contentType.isBlank()) return "application/octet-stream";
        int semicolon = contentType.indexOf(';');
        String mediaType = (semicolon != -1) ? contentType.substring(0, semicolon) : contentType;
        return mediaType.trim().toLowerCase(Locale.ROOT);
    }
}
