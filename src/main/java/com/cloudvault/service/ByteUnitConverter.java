package com.cloudvault.service;

public class ByteUnitConverter {
    public static double toMegabytes(long bytes) {
        return (double) bytes / (1024 * 1024);
    }

    public static double toGigabytes(long bytes) {
        return (double) bytes / (1024 * 1024 * 1024);
    }

    public static long gigabytesToBytes(double gb) {
        return (long) (gb * 1024 * 1024 * 1024);
    }
}
