package com.cloudvault.service;

public record LifecycleRuleFilter(String prefix, long minSizeBytes) {
    public boolean matches(String key, long sizeBytes) {
        if (key == null) return false;
        if (prefix != null && !key.startsWith(prefix)) return false;
        return sizeBytes >= minSizeBytes;
    }
}
