package com.cloudvault.service;

public final class LegalHoldPolicyGuard {
    private LegalHoldPolicyGuard() {}

    public static boolean isDeletionAllowed(boolean hasLegalHold, boolean hasRetentionLock) {
        return !hasLegalHold && !hasRetentionLock;
    }
}
