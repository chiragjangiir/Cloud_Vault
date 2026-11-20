package com.cloudvault.service;

public final class ChunkedPayloadSigner {
    private ChunkedPayloadSigner() {}

    public static String formatChunkHeader(int chunkSizeBytes, String signature) {
        return Integer.toHexString(chunkSizeBytes) + ";chunk-signature=" + signature + "\r\n";
    }
}
