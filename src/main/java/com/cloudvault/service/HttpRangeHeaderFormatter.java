package com.cloudvault.service;

public class HttpRangeHeaderFormatter {
    public static String formatContentRange(long start, long end, long total) {
        return String.format("bytes %d-%d/%d", start, end, total);
    }
}
