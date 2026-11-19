package com.cloudvault.service;

import java.util.Locale;

public enum ObjectLockRetentionMode {
    GOVERNANCE,
    COMPLIANCE;

    public static ObjectLockRetentionMode fromString(String mode) {
        if (mode == null) return null;
        try {
            return ObjectLockRetentionMode.valueOf(mode.toUpperCase(Locale.ROOT).trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
