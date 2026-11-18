package com.cloudvault.service;

import java.util.Locale;
import java.util.Set;

public final class MimeTypeWhitelistGuard {
    private final Set<String> allowedTypes;

    public MimeTypeWhitelistGuard(Set<String> allowedTypes) {
        this.allowedTypes = allowedTypes != null ? allowedTypes : Set.of();
    }

    public boolean isAllowed(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) {
            return false;
        }
        String clean = mimeType.trim().toLowerCase(Locale.ROOT);
        if (allowedTypes.contains(clean)) {
            return true;
        }
        int slash = clean.indexOf('/');
        if (slash != -1) {
            String wildcard = clean.substring(0, slash) + "/*";
            return allowedTypes.contains(wildcard);
        }
        return false;
    }
}
