package com.cloudvault.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class DigestAlgorithmUtil {
    public static byte[] computeDigest(String algorithm, byte[] data) {
        if (algorithm == null || data == null) {
            throw new IllegalArgumentException("Algorithm and data cannot be null");
        }
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            return md.digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("Unsupported algorithm: " + algorithm, e);
        }
    }

    public static boolean isSupportedAlgorithm(String algorithm) {
        try {
            MessageDigest.getInstance(algorithm);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
