package com.cloudvault.service;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Path sanitization helper preventing directory traversal and null byte injections.
 */
public final class StoragePathNormalizer {

    private StoragePathNormalizer() {}

    public static String sanitize(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            return "";
        }
        String cleaned = rawPath.replace("\0", "").replace('\\', '/');
        Path normalized = Paths.get(cleaned).normalize();
        String result = normalized.toString().replace('\\', '/');

        while (result.startsWith("../") || result.equals("..")) {
            result = result.substring(result.startsWith("../") ? 3 : 2);
        }
        return result;
    }

    public static boolean containsTraversal(String path) {
        if (path == null) return false;
        return path.contains("..") || path.contains("\0");
    }
}
