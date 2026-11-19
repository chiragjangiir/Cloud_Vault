package com.cloudvault.service;

import java.time.Instant;

public final class SignedUrlSignatureVerifier {
    private SignedUrlSignatureVerifier() {}

    public static boolean isExpired(long expiresEpochSeconds, Instant now) {
        return now.getEpochSecond() > expiresEpochSeconds;
    }

    public static boolean verifySignature(String expectedSig, String actualSig) {
        if (expectedSig == null || actualSig == null) return false;
        return expectedSig.equals(actualSig);
    }
}
