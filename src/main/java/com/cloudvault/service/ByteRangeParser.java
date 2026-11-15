package com.cloudvault.service;

public final class ByteRangeParser {
    public record ByteRange(long start, long end) {}

    private ByteRangeParser() {}

    public static ByteRange parse(String headerValue, long totalLength) {
        if (headerValue == null || !headerValue.startsWith("bytes=")) {
            return new ByteRange(0, totalLength - 1);
        }
        String rangePart = headerValue.substring(6).trim();
        String[] parts = rangePart.split("-", -1);
        long start = parts[0].isBlank() ? 0 : Long.parseLong(parts[0]);
        long end = parts.length > 1 && !parts[1].isBlank() ? Long.parseLong(parts[1]) : totalLength - 1;
        start = Math.max(0, Math.min(start, totalLength - 1));
        end = Math.max(start, Math.min(end, totalLength - 1));
        return new ByteRange(start, end);
    }
}
