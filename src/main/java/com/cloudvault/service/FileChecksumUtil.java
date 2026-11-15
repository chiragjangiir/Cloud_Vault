package com.cloudvault.service;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Streaming checksum computation utility supporting SHA-256 and MD5.
 */
public final class FileChecksumUtil {

    private FileChecksumUtil() {}

    public static String calculateSha256(InputStream inputStream) throws IOException {
        return calculateDigest(inputStream, "SHA-256");
    }

    public static String calculateDigest(InputStream inputStream, String algorithm) throws IOException {
        if (inputStream == null) {
            throw new IllegalArgumentException("InputStream must not be null");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Digest algorithm not available: " + algorithm, e);
        }
    }
}
