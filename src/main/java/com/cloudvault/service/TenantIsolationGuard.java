package com.cloudvault.service;

public class TenantIsolationGuard {
    public static boolean hasTenantPrefix(String key, String tenantId) {
        if (key == null || tenantId == null) return false;
        String prefix = tenantId + "/";
        return key.startsWith(prefix);
    }

    public static String enforceTenantPrefix(String key, String tenantId) {
        if (hasTenantPrefix(key, tenantId)) return key;
        String cleanKey = key.startsWith("/") ? key.substring(1) : key;
        return tenantId + "/" + cleanKey;
    }
}
