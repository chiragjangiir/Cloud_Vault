package com.cloudvault.service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public final class HttpDateHeaderParser {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.RFC_1123_DATE_TIME.withZone(ZoneOffset.UTC);

    private HttpDateHeaderParser() {}

    public static Instant parse(String headerValue) {
        if (headerValue == null || headerValue.isBlank()) {
            return null;
        }
        try {
            return Instant.from(FORMATTER.parse(headerValue));
        } catch (Exception e) {
            return null;
        }
    }

    public static String format(Instant instant) {
        if (instant == null) return "";
        return FORMATTER.format(instant);
    }
}
