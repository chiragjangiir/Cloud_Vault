package com.cloudvault.service;

import java.security.SecureRandom;

public final class S3UploadIdGenerator {
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private S3UploadIdGenerator() {}

    public static String generateId(int length) {
        int safeLen = Math.max(16, length);
        StringBuilder sb = new StringBuilder(safeLen);
        for (int i = 0; i < safeLen; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
