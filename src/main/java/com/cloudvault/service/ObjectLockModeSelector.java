package com.cloudvault.service;

public final class ObjectLockModeSelector {
    public enum Mode {
        GOVERNANCE, COMPLIANCE, NONE
    }

    private ObjectLockModeSelector() {}

    public static Mode parse(String raw) {
        if (raw == null) return Mode.NONE;
        String clean = raw.trim().toUpperCase();
        if ("GOVERNANCE".equals(clean)) return Mode.GOVERNANCE;
        if ("COMPLIANCE".equals(clean)) return Mode.COMPLIANCE;
        return Mode.NONE;
    }
}
