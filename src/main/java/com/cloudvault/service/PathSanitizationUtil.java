package com.cloudvault.service;

import java.util.ArrayList;
import java.util.List;

public final class PathSanitizationUtil {
    private PathSanitizationUtil() {}

    public static String cleanPath(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) return "";
        String[] parts = rawPath.split("/+");
        List<String> clean = new ArrayList<>();
        for (String p : parts) {
            if (p.isEmpty() || ".".equals(p)) continue;
            if ("..".equals(p)) {
                if (!clean.isEmpty()) clean.remove(clean.size() - 1);
            } else {
                clean.add(p);
            }
        }
        return String.join("/", clean);
    }
}
