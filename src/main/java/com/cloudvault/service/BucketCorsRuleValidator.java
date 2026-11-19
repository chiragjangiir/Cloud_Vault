package com.cloudvault.service;

import java.util.List;

public final class BucketCorsRuleValidator {
    private BucketCorsRuleValidator() {}

    public static boolean isValidOrigin(String origin) {
        if (origin == null || origin.isBlank()) return false;
        return "*".equals(origin) || origin.startsWith("http://") || origin.startsWith("https://");
    }

    public static boolean isValidMethods(List<String> methods) {
        if (methods == null || methods.isEmpty()) return false;
        List<String> allowed = List.of("GET", "PUT", "HEAD", "POST", "DELETE");
        return allowed.containsAll(methods);
    }
}
