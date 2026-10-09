package com.cloudvault.service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/** Human-readable formatting for sizes and timestamps. */
public final class Format {

    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);

    private Format() {}

    public static String bytes(long value) {
        if (value < 0) return "0 B";
        if (value < 1024) return value + " B";
        double v = value;
        String[] units = {"B", "KB", "MB", "GB", "TB", "PB"};
        int i = 0;
        while (v >= 1024 && i < units.length - 1) {
            v /= 1024;
            i++;
        }
        return (v >= 100 || i == 0 ? String.format("%.0f", v) : v >= 10 ? String.format("%.1f", v) : String.format("%.2f", v))
                + " " + units[i];
    }

    public static String dateTime(Instant instant) {
        return instant == null ? "—" : DATE_TIME.format(instant);
    }

    public static String date(Instant instant) {
        return instant == null ? "—" : DATE.format(instant);
    }

    public static String percent(long used, long total) {
        if (total <= 0) return "0%";
        return String.format("%.1f%%", Math.min(100.0, (used * 100.0) / total));
    }

    /** Numeric percentage clamped to 0–100 (for progress bars). */
    public static double percentNumber(long used, long total) {
        if (total <= 0) return 0;
        return Math.min(100.0, Math.max(0.0, (used * 100.0) / total));
    }
}
