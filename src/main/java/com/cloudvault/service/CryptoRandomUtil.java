package com.cloudvault.service;

import java.security.SecureRandom;

public class CryptoRandomUtil {
    private static final SecureRandom RANDOM = new SecureRandom();

    public static String generateRandomHex(int byteLength) {
        byte[] bytes = new byte[byteLength];
        RANDOM.nextBytes(bytes);
        return HexEncodeUtil.toHexString(bytes);
    }
}
