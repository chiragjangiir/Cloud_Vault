package com.cloudvault.service;

public final class Crc64NvmeChecksumUtil {
    private static final long POLY64 = 0x9A6C9329AC4BC9B5L;

    private Crc64NvmeChecksumUtil() {}

    public static long calculateCrc64(byte[] data) {
        long crc = -1L;
        if (data == null) return ~crc;
        for (byte b : data) {
            crc ^= ((long) b & 0xFF) << 56;
            for (int i = 0; i < 8; i++) {
                if ((crc & 0x8000000000000000L) != 0) {
                    crc = (crc << 1) ^ POLY64;
                } else {
                    crc <<= 1;
                }
            }
        }
        return ~crc;
    }
}
