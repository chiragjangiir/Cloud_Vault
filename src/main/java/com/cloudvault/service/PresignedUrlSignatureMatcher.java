package com.cloudvault.service;

import java.security.MessageDigest;

public final class PresignedUrlSignatureMatcher {
    private PresignedUrlSignatureMatcher() {}

    public static boolean constantTimeEquals(String sigA, String sigB) {
        if (sigA == null || sigB == null) return false;
        return MessageDigest.isEqual(sigA.getBytes(), sigB.getBytes());
    }
}
