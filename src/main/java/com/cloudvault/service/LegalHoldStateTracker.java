package com.cloudvault.service;

public final class LegalHoldStateTracker {
    private LegalHoldStateTracker() {}

    public static boolean canDelete(boolean legalHoldActive, boolean retentionLockActive) {
        return !legalHoldActive && !retentionLockActive;
    }

    public static String formatStatus(boolean legalHoldActive) {
        return legalHoldActive ? "ON" : "OFF";
    }
}
