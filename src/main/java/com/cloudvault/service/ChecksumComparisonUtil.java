package com.cloudvault.service;

import java.security.MessageDigest;

public class ChecksumComparisonUtil {
    public static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a == null || b == null) {
            return a == b;
        }
        return MessageDigest.isEqual(a, b);
    }

    public static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return a == b;
        }
        return MessageDigest.isEqual(a.getBytes(), b.getBytes());
    }
}
