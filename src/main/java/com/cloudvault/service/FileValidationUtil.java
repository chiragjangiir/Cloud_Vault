package com.cloudvault.service;

import java.util.Locale;
import java.util.Set;

/**
 * Utility for verifying file extensions and MIME safety.
 */
public final class FileValidationUtil {

    private static final Set<String> BLOCKED_EXTENSIONS = Set.of(
            "exe", "bat", "cmd", "sh", "bash", "ps1", "vbs", "jar", "msi", "com", "scr"
    );

    private FileValidationUtil() {
    }

    public static String extractExtension(String filename) {
        if (filename == null || filename.isBlank()) {
            return "";
        }
        int idx = filename.lastIndexOf('.');
        if (idx <= 0 || idx == filename.length() - 1) {
            return "";
        }
        return filename.substring(idx + 1).toLowerCase(Locale.ROOT).trim();
    }

    public static boolean isDangerousExecutable(String filename) {
        String ext = extractExtension(filename);
        return BLOCKED_EXTENSIONS.contains(ext);
    }

    public static boolean isAllowedFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return false;
        }
        if (filename.contains("/") || filename.contains("\\") || filename.contains("..")) {
            return false;
        }
        return !isDangerousExecutable(filename);
    }
}
