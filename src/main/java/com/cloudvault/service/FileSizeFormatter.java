package com.cloudvault.service;

public class FileSizeFormatter {
    private static final String[] UNITS = {"B", "KiB", "MiB", "GiB", "TiB"};

    public static String formatBinary(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int unitIndex = 0;
        double value = bytes;
        while (value >= 1024 && unitIndex < UNITS.length - 1) {
            value /= 1024;
            unitIndex++;
        }
        return String.format(java.util.Locale.US, "%.2f %s", value, UNITS[unitIndex]);
    }
}
