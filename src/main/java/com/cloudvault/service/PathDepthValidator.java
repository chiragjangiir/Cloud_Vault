package com.cloudvault.service;

public final class PathDepthValidator {
    private PathDepthValidator() {}

    public static int calculateDepth(String path) {
        if (path == null || path.isBlank()) {
            return 0;
        }
        String[] segments = path.split("/+");
        int count = 0;
        for (String seg : segments) {
            if (!seg.isEmpty()) {
                count++;
            }
        }
        return count;
    }

    public static boolean isWithinMaxDepth(String path, int maxDepth) {
        return calculateDepth(path) <= maxDepth;
    }
}
