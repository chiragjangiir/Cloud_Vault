package com.cloudvault.service;

public final class S3ServerSideEncryptionAlgorithmSelector {
    public enum Algorithm {
        AES256, AWS_KMS, AWS_KMS_DSSE, NONE
    }

    private S3ServerSideEncryptionAlgorithmSelector() {}

    public static Algorithm parse(String raw) {
        if (raw == null) return Algorithm.NONE;
        String clean = raw.trim().toUpperCase();
        if ("AES256".equals(clean)) return Algorithm.AES256;
        if ("AWS:KMS".equals(clean)) return Algorithm.AWS_KMS;
        if ("AWS:KMS:DSSE".equals(clean)) return Algorithm.AWS_KMS_DSSE;
        return Algorithm.NONE;
    }
}
