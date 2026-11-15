package com.cloudvault.service;

import java.time.Duration;

public final class DurationFormatUtil {
    private DurationFormatUtil() {}

    public static String formatDuration(Duration duration) {
        if (duration == null || duration.isNegative() || duration.isZero()) {
            return "0s";
        }
        long seconds = duration.toSeconds();
        if (seconds < 60) return seconds + "s";
        long minutes = seconds / 60;
        long remainingSec = seconds % 60;
        if (minutes < 60) {
            return remainingSec == 0 ? minutes + "m" : minutes + "m " + remainingSec + "s";
        }
        long hours = minutes / 60;
        long remainingMin = minutes % 60;
        return remainingMin == 0 ? hours + "h" : hours + "h " + remainingMin + "m";
    }
}
