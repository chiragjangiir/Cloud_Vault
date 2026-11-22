package com.cloudvault.service;

public final class ContentDispositionFormatter {
    private ContentDispositionFormatter() {}

    public static String formatAttachment(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return "attachment";
        }
        String sanitized = filename.replace("\"", "\\\"");
        return "attachment; filename=\"" + sanitized + "\"";
    }

    public static String formatInline(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return "inline";
        }
        String sanitized = filename.replace("\"", "\\\"");
        return "inline; filename=\"" + sanitized + "\"";
    }
}
