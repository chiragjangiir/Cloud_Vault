package com.cloudvault.service;

import java.security.SecureRandom;
import java.util.Base64;

public final class CspNonceGenerator {
    private static final SecureRandom RANDOM = new SecureRandom();

    private CspNonceGenerator() {}

    public static String generateNonce(int byteLength) {
        int length = Math.max(byteLength, 16);
        byte[] bytes = new byte[length];
        RANDOM.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static String generateNonce() {
        return generateNonce(16);
    }
}
