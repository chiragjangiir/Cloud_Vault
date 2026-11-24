package com.cloudvault.service;

public final class RangeHeaderByteParser {
    private final long start;
    private final long end;

    public RangeHeaderByteParser(long start, long end) {
        this.start = start;
        this.end = end;
    }

    public static RangeHeaderByteParser parse(String rangeHeader, long objectSize) {
        if (rangeHeader == null || !rangeHeader.startsWith("bytes=")) return null;
        String spec = rangeHeader.substring(6).trim();
        String[] parts = spec.split("-", -1);
        if (parts.length != 2) return null;
        try {
            long s = parts[0].isEmpty() ? objectSize - Long.parseLong(parts[1]) : Long.parseLong(parts[0]);
            long e = parts[1].isEmpty() ? objectSize - 1 : Long.parseLong(parts[1]);
            if (s < 0 || s > e || e >= objectSize) return null;
            return new RangeHeaderByteParser(s, e);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public long getStart() { return start; }
    public long getEnd() { return end; }
    public long getLength() { return end - start + 1; }
}
