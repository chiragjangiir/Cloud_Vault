package com.cloudvault.service;

import java.util.Map;

public class ContentTypeMapper {
    private static final Map<String, String> EXT_MAP = Map.of(
        "html", "text/html; charset=UTF-8",
        "json", "application/json",
        "xml", "application/xml",
        "txt", "text/plain; charset=UTF-8",
        "csv", "text/csv; charset=UTF-8",
        "svg", "image/svg+xml"
    );

    public static String getContentType(String ext, String fallback) {
        if (ext == null) return fallback;
        return EXT_MAP.getOrDefault(ext.toLowerCase(), fallback);
    }
}
