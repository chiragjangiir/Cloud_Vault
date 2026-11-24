package com.cloudvault.service;

public final class BucketPolicyPrincipalValidator {
    private BucketPolicyPrincipalValidator() {}

    public static boolean isValidPrincipal(String principal) {
        if (principal == null) return false;
        String clean = principal.trim();
        if ("*".equals(clean)) return true;
        return clean.startsWith("arn:aws:iam::") || clean.startsWith("arn:aws:sts::");
    }
}
