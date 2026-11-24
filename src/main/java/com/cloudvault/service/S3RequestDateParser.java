package com.cloudvault.service;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public final class S3RequestDateParser {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(java.time.ZoneOffset.UTC);

    private S3RequestDateParser() {}

    public static Instant parseIso8601Basic(String raw) {
        if (raw == null) return null;
        try {
            return Instant.from(FORMATTER.parse(raw.trim()));
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
