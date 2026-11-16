package com.cloudvault.service;

public class ChunkSizeCalculator {
    public static final long MIN_CHUNK_SIZE = 5L * 1024 * 1024; // 5 MB
    public static final long MAX_CHUNK_SIZE = 5L * 1024 * 1024 * 1024; // 5 GB
    public static final int MAX_PARTS = 10000;

    public static long calculateChunkSize(long totalBytes) {
        if (totalBytes <= 0) {
            return MIN_CHUNK_SIZE;
        }
        long requiredChunk = (long) Math.ceil((double) totalBytes / MAX_PARTS);
        if (requiredChunk < MIN_CHUNK_SIZE) {
            return MIN_CHUNK_SIZE;
        }
        if (requiredChunk > MAX_CHUNK_SIZE) {
            return MAX_CHUNK_SIZE;
        }
        return requiredChunk;
    }

    public static int calculatePartCount(long totalBytes, long chunkSize) {
        if (totalBytes <= 0 || chunkSize <= 0) return 0;
        return (int) Math.ceil((double) totalBytes / chunkSize);
    }
}
