package com.cloudvault.service;

import java.util.Locale;

public final class FileExtensionNormalizer {
    private FileExtensionNormalizer() {}

    public static String extractExtension(String filename) {
        if (filename == null || filename.isBlank()) {
            return "";
        }
        String lower = filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".tar.gz")) return "tar.gz";
        if (lower.endsWith(".tar.bz2")) return "tar.bz2";
        if (lower.endsWith(".tar.xz")) return "tar.xz";

        int lastDot = lower.lastIndexOf('.');
        if (lastDot == -1 || lastDot == lower.length() - 1) {
            return "";
        }
        return lower.substring(lastDot + 1);
    }

    public static boolean isArchive(String filename) {
        String ext = extractExtension(filename);
        return ext.equals("tar.gz") || ext.equals("tar.bz2") || ext.equals("zip") || ext.equals("tar");
    }
}
