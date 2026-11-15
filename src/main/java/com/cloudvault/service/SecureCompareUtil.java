package com.cloudvault.service;

import java.security.MessageDigest;

public final class SecureCompareUtil {
    private SecureCompareUtil() {}

    public static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return a == b;
        return MessageDigest.isEqual(a.getBytes(), b.getBytes());
    }
}
