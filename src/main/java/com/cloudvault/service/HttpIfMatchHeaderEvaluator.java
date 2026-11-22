package com.cloudvault.service;

public final class HttpIfMatchHeaderEvaluator {
    private HttpIfMatchHeaderEvaluator() {}

    public static boolean matches(String ifMatchHeader, String currentETag) {
        if (ifMatchHeader == null || currentETag == null) return false;
        String cleanHeader = ifMatchHeader.trim();
        if ("*".equals(cleanHeader)) return true;
        String normalizedHeader = cleanHeader.replace("\"", "");
        String normalizedEtag = currentETag.replace("\"", "").trim();
        for (String part : normalizedHeader.split(",")) {
            if (part.trim().equals(normalizedEtag)) return true;
        }
        return false;
    }
}
