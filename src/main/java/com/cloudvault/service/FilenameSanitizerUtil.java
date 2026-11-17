package com.cloudvault.service;

import java.util.Set;

public class FilenameSanitizerUtil {
    private static final Set<String> RESERVED = Set.of(
        "CON", "PRN", "AUX", "NUL",
        "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
        "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"
    );

    public static String sanitize(String filename) {
        if (filename == null || filename.trim().isEmpty()) return "unnamed_file";
        String clean = filename.replaceAll("[\\\\/:*?\"<>|\\x00]", "_").trim();
        int dot = clean.indexOf('.');
        String base = (dot > 0) ? clean.substring(0, dot) : clean;
        if (RESERVED.contains(base.toUpperCase())) {
            clean = "safe_" + clean;
        }
        return clean;
    }
}
