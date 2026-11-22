package com.cloudvault.service;

public final class ChecksumAlgorithmSelector {
    public enum SupportedAlgorithm {
        CRC32, CRC32C, SHA1, SHA256
    }

    private ChecksumAlgorithmSelector() {}

    public static SupportedAlgorithm select(String headerValue) {
        if (headerValue == null) return SupportedAlgorithm.SHA256;
        String clean = headerValue.trim().toUpperCase();
        for (SupportedAlgorithm alg : SupportedAlgorithm.values()) {
            if (alg.name().equals(clean)) return alg;
        }
        return SupportedAlgorithm.SHA256;
    }
}
