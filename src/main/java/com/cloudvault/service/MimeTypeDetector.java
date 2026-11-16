package com.cloudvault.service;

import java.util.Arrays;

public class MimeTypeDetector {
    private static final byte[] PNG_MAGIC = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] JPEG_MAGIC = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PDF_MAGIC = new byte[]{0x25, 0x50, 0x44, 0x46}; // %PDF
    private static final byte[] ZIP_MAGIC = new byte[]{0x50, 0x4B, 0x03, 0x04}; // PK..
    private static final byte[] GZIP_MAGIC = new byte[]{0x1F, (byte) 0x8B};

    public static String detectFromBytes(byte[] header) {
        if (header == null || header.length < 2) {
            return "application/octet-stream";
        }
        if (startsWith(header, PNG_MAGIC)) return "image/png";
        if (startsWith(header, JPEG_MAGIC)) return "image/jpeg";
        if (startsWith(header, PDF_MAGIC)) return "application/pdf";
        if (startsWith(header, ZIP_MAGIC)) return "application/zip";
        if (startsWith(header, GZIP_MAGIC)) return "application/gzip";
        return "application/octet-stream";
    }

    private static boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) return false;
        }
        return true;
    }
}
