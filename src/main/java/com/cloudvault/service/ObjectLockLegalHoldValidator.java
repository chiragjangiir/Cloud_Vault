package com.cloudvault.service;

public final class ObjectLockLegalHoldValidator {
    public enum LegalHoldStatus {
        ON, OFF
    }

    private ObjectLockLegalHoldValidator() {}

    public static boolean isValidStatus(String status) {
        if (status == null) return false;
        String clean = status.trim().toUpperCase();
        return "ON".equals(clean) || "OFF".equals(clean);
    }

    public static LegalHoldStatus parseStatus(String status) {
        if ("ON".equalsIgnoreCase(status)) return LegalHoldStatus.ON;
        return LegalHoldStatus.OFF;
    }
}
