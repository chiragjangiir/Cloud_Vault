package com.cloudvault.service;

public final class PresignedUploadPolicyEvaluator {
    private PresignedUploadPolicyEvaluator() {}

    public static boolean isContentLengthAllowed(long length, long minBytes, long maxBytes) {
        return length >= minBytes && length <= maxBytes;
    }
}
