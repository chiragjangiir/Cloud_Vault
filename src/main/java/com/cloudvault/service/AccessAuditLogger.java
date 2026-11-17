package com.cloudvault.service;

public class AccessAuditLogger {
    public static String formatAuditEvent(String tenantId, String action, String key, String clientIp) {
        return String.format("ACCESS_AUDIT tenant=%s action=%s key=%s ip=%s", tenantId, action, key, clientIp);
    }
}
